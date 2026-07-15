package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CopyOnWriteArrayList

class VelocityCommandManager(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {
    private val commands = CopyOnWriteArrayList<String>()

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
        private val logger = LogManager.getLogger(ConnectionCommand::class.java)

        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val alias = invocation.alias()
            if (source !is Player) {
                logger.info("Ignoring connection command /$alias because the source is not a player.")
                return
            }

            val config = plugin.connectionPlugin.connectionConfig.get()
            val messages = plugin.connectionPlugin.messageConfig.get()

            if (command.permission.isNotEmpty() && !source.hasPermission(command.permission)) {
                logger.warn(
                    "Player ${source.username} (${source.uniqueId}) tried connection command /$alias " +
                        "without permission ${command.permission}."
                )
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
                    if (!isFromAllowed) {
                        logger.info(
                            "Skipping target connection ${target.name} for player ${source.username} " +
                                "(${source.uniqueId}) via /$alias because current server " +
                                "$currentServerName is not allowed by from=${target.from}."
                        )
                        continue
                    }
                }

                val connection = ConnectionResolver.findConnection(target.name, config.connections)
                if (connection == null) {
                    logger.warn(
                        "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                            "references unknown target connection ${target.name}."
                    )
                    continue
                }

                val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                    source.hasPermission(permission)
                }
                if (failedRule != null) {
                    logger.warn(
                        "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                            "was blocked by rule ${failedRule.name} on target connection ${connection.name}."
                    )
                    continue
                }

                val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                if (matchingNames.isEmpty()) {
                    logger.warn(
                        "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                            "found no registered servers matching target connection ${connection.name}."
                    )
                    continue
                }

                val server = matchingNames
                    .mapNotNull { proxy.getServer(it).orElse(null) }
                    .minByOrNull { it.playersConnected.size }
                if (server == null) {
                    logger.warn(
                        "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                            "matched servers $matchingNames for target connection ${connection.name}, " +
                            "but none are registered on Velocity."
                    )
                    continue
                }

                if (currentServerName != null && server.serverInfo.name == currentServerName) {
                    source.sendMessage(messages.msg(command.messages.alreadyConnected))
                    return
                }

                logger.info(
                    "Sending player ${source.username} (${source.uniqueId}) from ${currentServerName ?: "<none>"} " +
                        "to ${server.serverInfo.name} via connection command /$alias " +
                        "(configured command: ${command.name}, target connection: ${connection.name})."
                )
                source.createConnectionRequest(server).connect().whenComplete { result, error ->
                    if (error != null) {
                        logger.warn(
                            "Connection command /$alias failed while sending player ${source.username} " +
                                "(${source.uniqueId}) to ${server.serverInfo.name}.",
                            error
                        )
                        return@whenComplete
                    }

                    if (result == null) {
                        logger.warn(
                            "Connection command /$alias completed without a result while sending player " +
                                "${source.username} (${source.uniqueId}) to ${server.serverInfo.name}."
                        )
                        return@whenComplete
                    }

                    if (result.isSuccessful) {
                        logger.info(
                            "Connection command /$alias sent player ${source.username} (${source.uniqueId}) " +
                                "to ${result.attemptedConnection.serverInfo.name}."
                        )
                        return@whenComplete
                    }

                    val attemptedServer = result.attemptedConnection.serverInfo.name
                    val reason = result.reasonComponent.map { it.toString() }.orElse("not provided")
                    if (result.status == Status.CONNECTION_CANCELLED) {
                        logger.warn(
                            "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                                "to $attemptedServer was cancelled by another plugin. Reason: $reason"
                        )
                    } else {
                        logger.warn(
                            "Connection command /$alias could not send player ${source.username} " +
                                "(${source.uniqueId}) to $attemptedServer. Status: ${result.status}. Reason: $reason"
                        )
                    }
                }
                return
            }

            logger.warn(
                "Connection command /$alias for player ${source.username} (${source.uniqueId}) " +
                    "found no usable target connection. Current server: ${currentServerName ?: "<none>"}."
            )
            source.sendMessage(messages.msg(command.messages.noTargetConnectionFound))
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            if (command.permission.isEmpty()) return true
            return invocation.source().hasPermission(command.permission)
        }

    }

}
