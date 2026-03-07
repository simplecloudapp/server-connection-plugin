package app.simplecloud.plugin.connection.bungeecord.command

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import org.apache.logging.log4j.LogManager

class ConnectionCommand(
    private val plugin: BungeeCordConnectionPlugin,
    private val audiences: BungeeAudiences,
) : Command("connection", "simplecloud.connection.reload") {

    private val logger = LogManager.getLogger(ConnectionCommand::class.java)

    override fun execute(sender: CommandSender, args: Array<out String>) {
        val messages = plugin.connectionPlugin.messageConfig.get()
        val audience = audiences.sender(sender)

        if (args.firstOrNull()?.equals("reload", ignoreCase = true) != true) {
            audience.sendMessage(messages.send(messages.command.commandUsage))
            return
        }

        audience.sendMessage(messages.send(messages.command.configReloading))
        try {
            CoroutineScope(Dispatchers.IO).launch {
                plugin.connectionPlugin.connectionConfig.reload()
                plugin.connectionPlugin.commandConfig.reload()
                plugin.connectionPlugin.messageConfig.reload()

                audience.sendMessage(messages.send(messages.command.configReloadedSuccess))
            }
        } catch (e: Exception) {
            audience.sendMessage(messages.send(messages.command.configReloadedFailed))
            logger.error("Failed to reload config", e)
        }
    }

}
