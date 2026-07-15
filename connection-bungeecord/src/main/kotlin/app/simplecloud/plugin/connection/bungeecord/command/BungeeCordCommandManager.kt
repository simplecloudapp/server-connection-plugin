package app.simplecloud.plugin.connection.bungeecord.command

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ServerConnectRequest
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Command
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CopyOnWriteArrayList

class BungeeCordCommandManager(
    private val plugin: BungeeCordConnectionPlugin,
    private val audiences: BungeeAudiences,
) {

    private val commands = CopyOnWriteArrayList<String>()
    private val logger = LogManager.getLogger(BungeeCordCommandManager::class.java)

    fun registerCommands() {
        val commands = plugin.connectionPlugin.commandConfig.get().commands
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommands() {
        commands.forEach { plugin.proxy.pluginManager.unregisterCommand(findCommand(it)) }
        commands.clear()
    }

    private fun findCommand(name: String): Command? {
        return plugin.proxy.pluginManager.commands.firstOrNull {
            it.value.name.equals(name, ignoreCase = true)
        }?.value
    }

    private fun registerCommand(command: CommandEntry) {
        val permission = command.permission.ifEmpty { null }

        val connectionCommand = object : Command(
            command.name,
            permission,
            *command.aliases.toTypedArray()
        ) {
            override fun execute(sender: CommandSender, args: Array<out String>) {
                val player = sender as? ProxiedPlayer ?: return
                handleCommand(player, command)
            }
        }

        plugin.proxy.pluginManager.registerCommand(plugin, connectionCommand)
        commands.add(command.name)
    }

    private fun handleCommand(player: ProxiedPlayer, command: CommandEntry) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messages = plugin.connectionPlugin.messageConfig.get()
        val audience = audiences.player(player)
        val commandName = command.name

        if (command.permission.isNotEmpty() && !player.hasPermission(command.permission)) {
            logger.warn(
                "Player ${player.name} (${player.uniqueId}) tried connection command /$commandName " +
                    "without permission ${command.permission}."
            )
            return
        }

        val currentServerName = player.server?.info?.name
        val serverNames = plugin.proxy.servers.keys.toList()
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
                        "Skipping target connection ${target.name} for player ${player.name} " +
                            "(${player.uniqueId}) via /$commandName because current server " +
                            "$currentServerName is not allowed by from=${target.from}."
                    )
                    continue
                }
            }

            val connection = ConnectionResolver.findConnection(target.name, config.connections)
            if (connection == null) {
                logger.warn(
                    "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                        "references unknown target connection ${target.name}."
                )
                continue
            }

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                player.hasPermission(permission)
            }
            if (failedRule != null) {
                logger.warn(
                    "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                        "was blocked by rule ${failedRule.name} on target connection ${connection.name}."
                )
                continue
            }

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) {
                logger.warn(
                    "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                        "found no registered servers matching target connection ${connection.name}."
                )
                continue
            }

            val targetServer = matchingNames
                .mapNotNull { plugin.proxy.servers[it] }
                .minByOrNull { it.players.size }
            if (targetServer == null) {
                logger.warn(
                    "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                        "matched servers $matchingNames for target connection ${connection.name}, " +
                        "but none are registered on BungeeCord."
                )
                continue
            }

            if (currentServerName != null && targetServer.name.equals(currentServerName, ignoreCase = true)) {
                audience.sendMessage(messages.msg(command.messages.alreadyConnected))
                return
            }

            logger.info(
                "Sending player ${player.name} (${player.uniqueId}) from ${currentServerName ?: "<none>"} " +
                    "to ${targetServer.name} via connection command /$commandName " +
                    "(target connection: ${connection.name})."
            )
            player.connect(
                ServerConnectRequest.builder()
                    .target(targetServer)
                    .reason(ServerConnectEvent.Reason.COMMAND)
                    .callback { result: ServerConnectRequest.Result?, error: Throwable? ->
                        if (error != null) {
                            logger.warn(
                                "Connection command /$commandName failed while sending player ${player.name} " +
                                    "(${player.uniqueId}) to ${targetServer.name}.",
                                error
                            )
                            return@callback
                        }

                        if (result == ServerConnectRequest.Result.SUCCESS) {
                            logger.info(
                                "Connection command /$commandName sent player ${player.name} " +
                                    "(${player.uniqueId}) to ${targetServer.name}."
                            )
                            return@callback
                        }

                        if (result == ServerConnectRequest.Result.EVENT_CANCEL) {
                            logger.warn(
                                "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                                    "to ${targetServer.name} was cancelled by ServerConnectEvent."
                            )
                        } else {
                            logger.warn(
                                "Connection command /$commandName could not send player ${player.name} " +
                                    "(${player.uniqueId}) to ${targetServer.name}. Result: $result."
                            )
                        }
                    }
                    .build()
            )
            return
        }

        logger.warn(
            "Connection command /$commandName for player ${player.name} (${player.uniqueId}) " +
                "found no usable target connection. Current server: ${currentServerName ?: "<none>"}."
        )
        audience.sendMessage(messages.msg(command.messages.noTargetConnectionFound))
    }

}
