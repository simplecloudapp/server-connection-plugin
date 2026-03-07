package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer

class VelocityCommandManager(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {
    private val commands = mutableListOf<String>()

    fun registerCommands() {
        val commands = plugin.connectionPlugin.commandConfig.get().commands
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommands() {
        commands.forEach { proxy.commandManager.unregister(it) }
        commands.clear()
    }

    private fun registerCommand(command: CommandEntry) {
        val connectionCommand = ConnectionCommand(plugin, proxy, command)
        val meta = proxy.commandManager.metaBuilder(command.name)
            .aliases(*command.aliases.toTypedArray())
            .plugin(plugin)
            .build()

        proxy.commandManager.register(meta, connectionCommand)
        commands.add(command.name)
    }

    private class ConnectionCommand(
        private val plugin: VelocityConnectionPlugin,
        private val proxy: ProxyServer,
        private val command: CommandEntry,
    ) : SimpleCommand {

        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            if (source !is Player) return

            val config = plugin.connectionPlugin.connectionConfig.get()
            val messages = plugin.connectionPlugin.messageConfig.get()

            if (command.permission.isNotEmpty() && !source.hasPermission(command.permission)) {
                source.sendMessage(messages.send(messages.kick.permissionDenied))
                return
            }

            val currentServerName = source.currentServer.orElse(null)?.serverInfo?.name
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
                if (failedRule != null) {
                    source.sendMessage(messages.send(messages.kick.permissionDenied))
                    return
                }

                val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                if (matchingNames.isEmpty()) continue

                val server = matchingNames
                    .mapNotNull { proxy.getServer(it).orElse(null) }
                    .minByOrNull { it.playersConnected.size }
                    ?: continue

                if (currentServerName != null && server.serverInfo.name == currentServerName) {
                    source.sendMessage(messages.send(command.messages.alreadyConnected))
                    return
                }

                source.createConnectionRequest(server).fireAndForget()
                return
            }

            source.sendMessage(messages.send(command.messages.noTargetConnectionFound))
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            if (command.permission.isEmpty()) return true
            return invocation.source().hasPermission(command.permission)
        }

    }

}
