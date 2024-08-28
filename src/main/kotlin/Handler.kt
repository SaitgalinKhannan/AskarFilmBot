import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onVideo
import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.utils.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

@OptIn(PreviewFeature::class)
suspend fun BehaviourContext.handlers(dataBase: DataBase) {
    onCommand("start") {
        val fromUser = it.asFromUser()?.user

        if (fromUser != null) {
            val user = User(
                id = fromUser.id.chatId.long,
                username = fromUser.username?.username ?: "",
                firstName = fromUser.firstName,
                lastName = fromUser.lastName,
                agreement = false
            )
            dataBase.addUser(user)

            reply(
                to = it,
                entities = buildEntities {
                    boldln("Привет!")
                    +"Я бот который сделает тебя частичкой фильма" + boldln("«Лето.Город.Любовь»")
                    +"Сними горизонтальное или вертикальное романтичное видео со своей  второй половиной и отправь мне, я перешлю это создателям фильма))"
                }
            )

            val agreementMessage = SendTextMessage(
                chatId = it.chat.id,
                entities = buildEntities {
                    bold("Вы согласны передать видео ... ?")
                },
                replyMarkup = inlineKeyboard {
                    row {
                        dataButton("Не согласен ❌", "not agree")
                        dataButton("Согласен ✅", "agree")
                    }
                }
            )

            waitMessageDataCallbackQuery(agreementMessage)
                .filter { callback -> callback.user.id == it.chat.id }
                .map { callback ->
                    when (callback.data) {
                        "agree" -> {
                            runCatching {
                                dataBase.updateUser(user.copy(agreement = true))
                            }.onSuccess { result ->
                                if (result) {
                                    sendMessage(
                                        chat = it.chat,
                                        text = "Вы согласились передать видеоматериалы ..."
                                    )
                                }
                            }
                        }
                    }
                    answerCallbackQuery(callback)
                    deleteMessage(callback.message)
                }
                .first()
        }
    }

    onVideo {
        val fileName = it.content.media.fileName ?: "${it.content.media.fileId}.mp4"
        val fileExtension = fileName.substringAfterLast('.')
        val currentDateTime = LocalDateTime.now()
        val formattedDateTime = currentDateTime.format(formatter)
        val newFileName = "${it.chat.id.chatId.long}-${formattedDateTime}.${fileExtension}"
        val destinationFile = File("/home/rose/RNT/AskarFilmBot/sources/${newFileName}")
        val file = downloadFile(it.content.media.fileId, destinationFile)
        reply(to = it, text = "Началась обработка видео...")
        val processedVideo = processVideo(file)
        sendVideo(
            chat = it.chat,
            text = "Готово!",
            video = InputFile.fromFile(processedVideo)
        )
    }

    onDocument {

    }
}