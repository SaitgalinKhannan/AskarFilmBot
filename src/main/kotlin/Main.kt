import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

suspend fun main() {
    val bot = telegramBot("7373274922:AAEyLbf-6w0TyBve9lt0hO65B0I07EjgwFI")
    val commands: List<BotCommand> = listOf(BotCommand("start", "Старт"))
    val dataBase = DataBase()

    bot.setMyCommands(commands)

    bot.buildBehaviourWithLongPolling(scope = CoroutineScope(Dispatchers.IO)) {
        handlers(dataBase)
    }.join()
}