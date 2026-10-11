package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.SimpleCommand
import org.slf4j.LoggerFactory

class ConnectionCommand(
    private val plugin: VelocityConnectionPlugin,
) : SimpleCommand {

    private val logger = LoggerFactory.getLogger(ConnectionCommand::class.java)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val args = invocation.arguments()
        val source = invocation.source()

        val messages = plugin.connectionPlugin.messageConfig.get()

        if (args.isEmpty() || !args[0].equals("reload", ignoreCase = true)) {
            source.sendMessage(messages.msg(messages.command.commandUsage))
            return
        }

        source.sendMessage(messages.msg(messages.command.configReloading))
        try {
            plugin.reload()
            source.sendMessage(messages.msg(messages.command.configReloadedSuccess))
        } catch (exception: Exception) {
            source.sendMessage(messages.msg(messages.command.configReloadedFailed))
            logger.error("Failed to reload config", exception)
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("simplecloud.connection.reload")
    }

}
