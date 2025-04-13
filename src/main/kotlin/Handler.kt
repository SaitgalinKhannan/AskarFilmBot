import dev.inmo.kslog.common.i
import dev.inmo.kslog.common.logger
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.forwardMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.asPossiblyForwardedMessage
import dev.inmo.tgbotapi.extensions.utils.asPossiblyReplyMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.asTelegramMessageId
import dev.inmo.tgbotapi.types.files.DocumentFile
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.files.VideoFile
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss")
val ffMpeg = FFMpeg()

@OptIn(PreviewFeature::class)
suspend fun BehaviourContext.handlers(dataBase: DataBase, mode: String) {
    onCommand("start") {
        val fromUser = it.asFromUser()?.user

        if (fromUser != null) {
            val userFromDB = runCatching {
                dataBase.getUser(fromUser.id.chatId.long)
            }.getOrNull()
            val user = User(
                userId = fromUser.id.chatId.long,
                username = fromUser.username?.username ?: "",
                firstName = fromUser.firstName,
                lastName = fromUser.lastName,
                agreement = userFromDB?.agreement ?: false
            )

            if (userFromDB == null) {
                runCatching {
                    dataBase.addUser(user)
                }
            }

            reply(
                to = it,
                text = """
                    <strong>Привет!</strong> 🌟 Я бот, который сделает тебя частичкой фильма <i>Лето.Город.Любовь</i> 🎥

                    📸 <b>Сними</b> романтичное вертикальное фото или видео со своей второй половиной
                    📤 <b>Отправь</b> его мне и получи персональный постер, которым можно похвастаться в социальных сетях 💖
                """.trimIndent(),
                parseMode = HTMLParseMode
            )
        }
    }

    onPhoto {
        runCatching {
            val user = dataBase.getUser(it.chat.id.chatId.long) ?: return@onPhoto
            logger.i(user)
            val path = processImage(it.content.media, it.chat.id, it.messageId, mode)
            saveVideoData(path, user, dataBase, it)
        }.onFailure { e ->
            if (e is CommonRequestException && "file is too big" in e.response.description.toString()) {
                reply(to = it, text = "Слишком большой файл, попробуйте отправить фото меньшего размера")
            } else {
                reply(to = it, text = "Произошла ошибка, попробуйте снова!")
            }

            throw e
        }
    }

    onVideo {
        runCatching {
            val user = dataBase.getUser(it.chat.id.chatId.long) ?: return@onVideo
            logger.i(user)
            val path = processVideo(it.content.media, it.chat.id, it.messageId, mode)
            saveVideoData(path, user, dataBase, it)
        }.onFailure { e ->
            if (e is CommonRequestException && "file is too big" in e.response.description.toString()) {
                reply(to = it, text = "Слишком большой файл, попробуйте отправить более короткое видео")
            } else {
                reply(to = it, text = "Произошла ошибка, попробуйте снова!")
            }

            throw e
        }
    }

    onDocument {
        runCatching {
            val user = dataBase.getUser(it.chat.id.chatId.long) ?: return@onDocument
            logger.i(user)
            val path = processDocument(it.content.media, it.chat.id, it.messageId, mode)
            saveVideoData(path, user, dataBase, it)
        }.onFailure { e ->
            if (e is CommonRequestException && "file is too big" in e.response.description.toString()) {
                reply(to = it, text = "Слишком большой файл, попробуйте отправить более короткое видео")
            } else {
                reply(to = it, text = "Произошла ошибка, попробуйте снова!")
            }

            throw e
        }
    }


    onMessageDataCallbackQuery { callback ->
        if (callback.user.id == callback.message.chat.id) {
            when {
                "yes" in callback.data -> {
                    answerCallbackQuery(callback)
                    callback.message.asPossiblyReplyMessage()?.replyTo?.asPossiblyForwardedMessage()?.let { message ->
                        val chatId = if (mode == "dev") -1001731128191 else -4538503015
                        val messageId = callback.data.substringAfter("=").toLongOrNull()
                        val message1 = forwardMessage(toChatId = chatId.toChatId(), message = message)
                        val message2 = if (messageId != null) {
                            forwardMessage(
                                toChatId = chatId.toChatId(),
                                fromChat = callback.message.chat,
                                messageId = messageId.asTelegramMessageId()
                            )
                        } else {
                            null
                        }

                        val message3 = sendTextMessage(
                            chatId = chatId.toChatId(),
                            text = """
                                ID: ${callback.user.id.chatId.long}
                                Username: ${callback.user.username?.username ?: "Отсутствует"}
                                ${callback.user.lastName} ${callback.user.firstName}                        
                            """.trimIndent()
                        )
                        launch {
                            runCatching {
                                val video = dataBase.getVideoByMessageId(message.messageId.long)
                                val user = dataBase.getUser(callback.user.id.chatId.long)
                                if (video != null && user != null) {
                                    val url = uploadVideo(video, user, mode)
                                    dataBase.updateVideo(
                                        video.copy(
                                            url = url,
                                            isSent = true,
                                            messageIds = "${message1.chat.id.chatId.long}:${message1.messageId.long},${message3.chat.id.chatId.long}:${message3.messageId.long}" +
                                                    message2?.let { msg -> ",${msg.chat.id.chatId.long}:${msg.messageId.long}" }
                                        )
                                    )
                                }
                            }.onFailure {
                                it.printStackTrace()
                            }
                            deleteMessage(callback.message)
                        }
                        reply(to = message, text = "Вы отправили свое фото/видео создателям фильма.")
                    }
                }

                callback.data == "no" -> {
                    reply(
                        to = callback.message,
                        text = "Если захотите, то можете нажать на кнопку, чтобы отправить свое фото/видео создателям фильма."
                    )
                    answerCallbackQuery(callback)
                }
            }
        }
    }
}

suspend fun saveVideoData(
    path: String?,
    user: User,
    dataBase: DataBase,
    message: CommonMessage<*>
) {
    if (path == null) return

    runCatching {
        dataBase.addVideo(
            Video(
                userId = user.userId,
                path = path,
                url = null,
                chatId = message.chat.id.chatId.long,
                messageId = message.messageId.long,
                isSent = false,
                messageIds = ""
            )
        )
    }.onFailure {
        it.printStackTrace()
    }
}


suspend fun BehaviourContext.processVideo(
    video: VideoFile,
    chatId: IdChatIdentifier,
    messageId: MessageId,
    mode: String
): String? = withContext(dispatcherIO) {
    val fileName = video.fileName ?: "${video.fileId}.mp4"
    val fileExtension = fileName.substringAfterLast('.')
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(formatter)
    val newFileName = "${chatId.chatId.long}-${formattedDateTime}.${fileExtension}"
    val destinationFile = File(
        if (mode == "dev")
            "$basePath/sources/${newFileName}"
        else
            "$basePath/sources/${newFileName}"
    ) //
    val file = downloadFile(video.fileId, destinationFile)
    reply(toChatId = chatId, toMessageId = messageId, text = "Обработка началась ...")
    val poster = choosePoster(height = video.height, width = video.width, chatId = chatId, mode = mode) ?: return@withContext null
    val processedVideo = ffMpeg.addVideoToProcess(
        inputVideo = file,
        height = video.height,
        width = video.width,
        overlayImage = poster,
        mode = mode
    )
    val message = sendVideo(
        chatId = chatId,
        text = """
            Готово!
            Не забудь выложить его в своих соц сетях и увидимся в кино!
        """.trimIndent(),
        video = InputFile.fromFile(processedVideo),
        height = video.height,
        width = video.width,
        duration = 15
    )

    return@withContext file.absolutePath
}

suspend fun BehaviourContext.processImage(
    photo: PhotoSize,
    chatId: IdChatIdentifier,
    messageId: MessageId,
    mode: String
): String? = withContext(dispatcherIO) {
    val fileExtension = "jpg"
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(formatter)
    val newFileName = "${chatId.chatId.long}-${formattedDateTime}.${fileExtension}"
    val destinationFile = File(
        if (mode == "dev")
            "$basePath/sources/${newFileName}"
        else
            "$basePath/sources/${newFileName}"
    ) //
    val file = downloadFile(photo.fileId, destinationFile)
    reply(toChatId = chatId, toMessageId = messageId, text = "Обработка началась, подождите немного ...")
    val poster = choosePoster(height = photo.height, width = photo.width, chatId = chatId, mode = mode) ?: return@withContext null
    val processedVideo = ffMpeg.addImageToProcess(file, photo.height, photo.width, poster, mode)
    sendVideo(
        chatId = chatId,
        text = """
            Готово!
            Не забудь выложить его в своих соц сетях и увидимся в кино!
        """.trimIndent(),
        video = InputFile.fromFile(processedVideo),
        height = photo.height,
        width = photo.width,
        duration = 15
    )

    return@withContext file.absolutePath
}

suspend fun BehaviourContext.processDocument(
    document: DocumentFile,
    chatId: IdChatIdentifier,
    messageId: MessageId,
    mode: String
): String? = withContext(dispatcherIO) {
    if ("video" in document.mimeType.toString()) {
        return@withContext null
    }

    val fName = if ("image" in document.mimeType.toString()) {
        "${document.fileId}.jpg"
    } else {
        throw Exception("Not supported MIME type")
    }
    val fileExtension = fName.substringAfterLast('.')
    val currentDateTime = LocalDateTime.now()
    val formattedDateTime = currentDateTime.format(formatter)
    val newFileName = "${chatId.chatId.long}-${formattedDateTime}.${fileExtension}"
    val destinationFile = File(
        if (mode == "dev")
            "$basePath/sources/${newFileName}"
        else
            "$basePath//sources/${newFileName}"
    )
    val file = downloadFile(document.fileId, destinationFile)
    val (width, height) = if ("image" in document.mimeType.toString()) {
        getImageDimensions(file)
    } else if ("video" in document.mimeType.toString()) {
        Pair(720, 1280)
    } else {
        throw Exception("Not supported MIME type")
    }
    reply(toChatId = chatId, toMessageId = messageId, text = "Обработка началась ...")
    val poster = choosePoster(height = height, width = width, chatId = chatId, mode = mode) ?: return@withContext null
    logger.i("width: $width, height: $height")
    val processedVideo = if ("image" in document.mimeType.toString()) {
        ffMpeg.addImageToProcess(file, height, width, poster, mode)
    } else if ("video" in document.mimeType.toString()) {
        ffMpeg.addVideoToProcess(file, height, width, poster, mode)
    } else {
        throw Exception("Not supported MIME type")
    }

    sendVideo(
        chatId = chatId,
        text = """
            Готово!
            Не забудь выложить его в своих соц сетях и увидимся в кино!
        """.trimIndent(),
        video = InputFile.fromFile(processedVideo),
        duration = 15
    )

    return@withContext file.absolutePath
}

@OptIn(RiskFeature::class)
suspend fun BehaviourContext.choosePoster(
    height: Int,
    width: Int,
    chatId: IdChatIdentifier,
    mode: String
): String? {
    val ratio = getAspectRatio(height = height, width = width)
    val examplePosters = if (ratio == AspectRatio.R169) {
        listOf(
            Poster.BLUE_TOP_16_9,
            Poster.BLUE_BOTTOM_16_9,
            Poster.PINK_TOP_16_9,
            Poster.PINK_BOTTOM_16_9
        )
    } else {
        listOf(
            Poster.BLUE_TOP_4_3,
            Poster.BLUE_BOTTOM_4_3,
            Poster.PINK_TOP_4_3,
            Poster.PINK_BOTTOM_4_3
        )
    }
    val images = examplePosters.map { if (mode == "dev") "$basePath/resources/${it.path}" else "$basePath/resources/${it.path}" }
    val photos = images.map {
        TelegramMediaPhoto(InputFile.fromFile(File(it)))
    }.toList()
    val message = sendMediaGroup(
        chatId = chatId,
        media = photos
    )

    reply(
        to = message,
        text = "Выберите постер:",
        replyMarkup = inlineKeyboard {
            row {
                dataButton("1", "1")
                dataButton("2", "2")
            }
            row {
                dataButton("3", "3")
                dataButton("4", "4")
            }
            row {
                dataButton("Отмена", "cancel")
                dataButton("Случайный", "random")
            }
        }
    )

    return merge(
        waitMessageDataCallbackQuery()
            .filter { callback -> callback.user.id == chatId }
            .map { callback ->
                val photo = when (callback.data) {
                    "1", "2", "3", "4" -> {
                        val photoId = callback.data.toIntOrNull()
                        if (photoId != null) {
                            images[photoId - 1]
                        } else {
                            images.random(random)
                        }
                    }

                    "random" -> {
                        images.random(random)
                    }

                    else -> null
                }
                answerCallbackQuery(callback)
                runCatching {
                    deleteMessage(message)
                    deleteMessage(callback.message)
                }
                photo
            },
        waitAnyContentMessage()
            .filter { content -> content.chat.id == chatId }
            .map { null }
    ).first()
}

enum class AspectRatio {
    R43,
    R169,
    R11
}

fun getAspectRatio(height: Int, width: Int): AspectRatio {
    if (height <= 0 || width <= 0) return AspectRatio.R43

    val ratio = maxOf(height, width).toDouble() / minOf(height, width).toDouble()
    val thresholdPercent = 0.1 // 10% порог

    val target43 = 4.0 / 3.0
    val target169 = 16.0 / 9.0

    val lower43 = target43 * (1 - thresholdPercent)
    val upper43 = target43 * (1 + thresholdPercent)
    val lower169 = target169 * (1 - thresholdPercent)
    val upper169 = target169 * (1 + thresholdPercent)

    return when (ratio) {
        in lower43..upper43 -> AspectRatio.R43
        in lower169..upper169 -> AspectRatio.R169
        else -> AspectRatio.R11
    }
}