import dev.inmo.kslog.common.DefaultKSLog
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.bot.setMyDescription
import dev.inmo.tgbotapi.extensions.api.bot.setMyShortDescription
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

const val basePath = "/home/faye/Projects/RNT/AskarFilmBot"
//const val basePath = "/root/AskarFilmBot"
val logger = DefaultKSLog(defaultTag = "TelegramBot")

suspend fun main(args: Array<String>) {
    val mode = args.firstOrNull()?.let { if (it == "prod") "prod" else null } ?: "dev"
    val bot =
        telegramBot(if (mode == "prod") "7480243763:AAEiKkUvt1bygSzDdFSy_4hPTAAYoqkMx80" else "8118618681:AAHDsMLFdq3e6FPMsZxvHC4A4R6T7LmDJA4")
    val commands: List<BotCommand> = listOf(BotCommand("start", "Старт"))
    val dataBase = DataBase()
    val description = """
                        Привет! Я бот, который сделает тебя частичкой фильма «Лето.Город.Любовь.»
                        Сними романтичное, вертикальное фото или видео со своей второй половиной. Отправь мне, я перешлю его создателям фильма. В подарок ты получишь персональный постер, которым можно похвастаться в социальных сетях.
                    """.trimIndent()

    bot.setMyCommands(commands)
    bot.setMyDescription(description)
    bot.setMyShortDescription("Привет! Я бот, который сделает тебя частичкой фильма Лето.Город.Любовь.")

    try {
        val sources = File("$basePath/sources")
        if (!sources.exists()) {
            sources.mkdir()
        }

        val output = File("$basePath/output")
        if (!output.exists()) {
            output.mkdir()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    bot.buildBehaviourWithLongPolling(
        scope = CoroutineScope(Dispatchers.IO),
        defaultExceptionsHandler = { e ->
            e.printStackTrace()
        }
    ) {
        handlers(dataBase, mode)
    }.join()
}