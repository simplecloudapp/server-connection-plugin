package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.CommandMeta
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.optionals.getOrNull

class VelocityCommandManager(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {
    private val commands = CopyOnWriteArrayList<CommandMeta>()

    fun registerCommands() {
        val commands = plugin.connectionPlugin.commandConfig.get().commands
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommands() {
        commands.forEach { meta -> proxy.commandManager.unregister(meta) }
        commands.clear()
    }

    private fun registerCommand(command: CommandEntry) {
        val connectionCommand = ConnectionCommand(plugin, proxy, command)
        val meta = proxy.commandManager.metaBuilder(command.name)
            .aliases(*command.aliases.toTypedArray())
            .plugin(plugin)
            .build()

        proxy.commandManager.register(meta, connectionCommand)
        commands.add(meta)
    }

    private class ConnectionCommand(
        private val plugin: VelocityConnectionPlugin,
        private val proxy: ProxyServer,
        private val command: CommandEntry,
    ) : SimpleCommand {
        private val logger = LoggerFactory.getLogger(ConnectionCommand::class.java)

        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source() as? Player ?: return
            if (command.permission.isNotEmpty() && !source.hasPermission(command.permission)) return

            val config = plugin.connectionPlugin.connectionConfig.get()
            val messages = plugin.connectionPlugin.messageConfig.get()

            val currentServerName = source.currentServer.getOrNull()?.serverInfo?.name
            val serverNames = proxy.allServers.map { it.serverInfo.name }
            val sortedTargets = command.targetConnections.sortedByDescending { it.priority }

            for (target in sortedTargets) {
                if (target.from.isNotEmpty() && currentServerName != null) {
                    val isFromAllowed = target.from.any { connectionName ->
                        ConnectionResolver.isServerInConnection(
                            currentServerName, connectionName, config.connections, serverNames
                        )
                    }
                    if (!isFromAllowed) continue
                }

                val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue

                val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                    source.hasPermission(permission)
                }
                if (failedRule != null) continue

                val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                if (matchingNames.isEmpty()) continue

                if (currentServerName != null && matchingNames.any { name -> name.equals(currentServerName, ignoreCase = true) }) {
                    source.sendMessage(messages.msg(command.messages.alreadyConnected))
                    return
                }

                val server = matchingNames
                    .mapNotNull { name -> proxy.getServer(name).getOrNull() }
                    .minByOrNull { it.playersConnected.size }
                    ?: continue

                source.createConnectionRequest(server).connect().whenComplete { result, error ->
                    if (error != null) {
                        logger.warn("Failed to send player ${source.username} to ${server.serverInfo.name}", error)
                        return@whenComplete
                    }
                    if (result == null || result.isSuccessful) return@whenComplete

                    val reason = result.reasonComponent.getOrNull()?.toString() ?: "not provided"
                    logger.warn("Could not send player ${source.username} to ${server.serverInfo.name} (${result.status}). Reason: $reason")
                }
                return
            }

            source.sendMessage(messages.msg(command.messages.noTargetConnectionFound))
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            if (command.permission.isEmpty()) return true
            return invocation.source().hasPermission(command.permission)
        }

    }

}
