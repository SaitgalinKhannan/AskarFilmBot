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

val logger = DefaultKSLog(defaultTag = "TelegramBot")

suspend fun main(args: Array<String>) {
    val mode = args.firstOrNull()?.let { if (it == "prod") "prod" else null } ?: "dev"
    val bot = telegramBot("7480243763:AAEiKkUvt1bygSzDdFSy_4hPTAAYoqkMx80")
    //val bot = telegramBot("7373274922:AAEyLbf-6w0TyBve9lt0hO65B0I07EjgwFI")
    val commands: List<BotCommand> = listOf(BotCommand("start", "Старт"))
    val dataBase = DataBase()
    val description = """
                        Привет! Я бот, который сделает тебя частичкой фильма «Лето.Город.Любовь.»
                        Сними романтичное, вертикальное фото или видео со своей второй половиной. Отправь мне, я перешлю его создателям фильма. В подарок ты получишь персональный постер, которым можно похвастаться в социальных сетях.
                    """.trimIndent()

    bot.setMyCommands(commands)
    bot.setMyDescription(description)
    bot.setMyShortDescription("Привет! Я бот, который сделает тебя частичкой фильма «Лето.Город.Любовь.»")

    runCatching {
        val sources = File("/sources")
        if (!sources.exists()) {
            sources.mkdir()
        }

        val output = File("/output")
        if (!output.exists()) {
            output.mkdir()
        }
    }

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        handlers(dataBase, mode)
    }.join()
}