package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.SimpleCommand
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager

class ConnectionCommand(
    private val plugin: VelocityConnectionPlugin,
) : SimpleCommand {

    private val logger = LogManager.getLogger(ConnectionCommand::class.java)

    override fun execute(invocation: SimpleCommand.Invocation) {
        val args = invocation.arguments()
        val source = invocation.source()

        val messages = plugin.connectionPlugin.messageConfig.get()

        if (args.isEmpty() || !args[0].equals("reload", ignoreCase = true)) {
            source.sendMessage(messages.send(messages.command.commandUsage))
            return
        }

        source.sendMessage(messages.send(messages.command.configReloading))
        plugin.connectionPlugin.scope.launch {
            try {
                plugin.connectionPlugin.reload()
                source.sendMessage(messages.send(messages.command.configReloadedSuccess))
            } catch (e: Exception) {
                source.sendMessage(messages.send(messages.command.configReloadedFailed))
                logger.error("Failed to reload config", e)
            }
        }
    }

    override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
        return invocation.source().hasPermission("simplecloud.connection.reload")
    }

}
