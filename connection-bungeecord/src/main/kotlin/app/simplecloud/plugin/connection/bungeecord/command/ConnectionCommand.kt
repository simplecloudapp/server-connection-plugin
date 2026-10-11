package app.simplecloud.plugin.connection.bungeecord.command

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import org.slf4j.LoggerFactory

class ConnectionCommand(
    private val plugin: BungeeCordConnectionPlugin,
    private val audiences: BungeeAudiences,
) : Command("connection", "simplecloud.connection.reload") {

    private val logger = LoggerFactory.getLogger(ConnectionCommand::class.java)

    override fun execute(sender: CommandSender, args: Array<out String>) {
        val messages = plugin.connectionPlugin.messageConfig.get()
        val audience = audiences.sender(sender)

        if (args.firstOrNull()?.equals("reload", ignoreCase = true) != true) {
            audience.sendMessage(messages.msg(messages.command.commandUsage))
            return
        }

        audience.sendMessage(messages.msg(messages.command.configReloading))
        try {
            plugin.reload()
            audience.sendMessage(messages.msg(messages.command.configReloadedSuccess))
        } catch (exception: Exception) {
            audience.sendMessage(messages.msg(messages.command.configReloadedFailed))
            logger.error("Failed to reload config", exception)
        }
    }

}
