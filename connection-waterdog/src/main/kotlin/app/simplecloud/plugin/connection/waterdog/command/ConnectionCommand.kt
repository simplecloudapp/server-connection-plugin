package app.simplecloud.plugin.connection.waterdog.command

import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.command.Command
import dev.waterdog.waterdogpe.command.CommandSender
import dev.waterdog.waterdogpe.command.CommandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.logging.log4j.LogManager

class ConnectionCommand(
    private val plugin: WaterdogConnectionPlugin,
) : Command(
    "connection",
    CommandSettings.builder()
        .setPermission("simplecloud.connection.reload")
        .build()
) {

    private val logger = LogManager.getLogger(ConnectionCommand::class.java)
    private val serializer = PlainTextComponentSerializer.plainText()

    override fun onExecute(sender: CommandSender, alias: String?, args: Array<out String>): Boolean {
        val messages = plugin.connectionPlugin.messageConfig.get()

        if (args.firstOrNull()?.equals("reload", ignoreCase = true) != true) {
            sendMessage(sender, messages.command.commandUsage)
            return true
        }

        sendMessage(sender, messages.command.configReloading)
        try {
            CoroutineScope(Dispatchers.IO).launch {
                plugin.connectionPlugin.connectionConfig.reload()
                plugin.connectionPlugin.commandConfig.reload()
                plugin.connectionPlugin.messageConfig.reload()

                sendMessage(sender, messages.command.configReloadedSuccess)
            }
        } catch (e: Exception) {
            sendMessage(sender, messages.command.configReloadedFailed)
            logger.error("Failed to reload config", e)
        }

        return true
    }

    private fun sendMessage(sender: CommandSender, rawMessage: String) {
        val messages = plugin.connectionPlugin.messageConfig.get()
        val component = messages.send(rawMessage)
        val plain = serializer.serialize(component)
        sender.sendMessage(plain)
    }

}
