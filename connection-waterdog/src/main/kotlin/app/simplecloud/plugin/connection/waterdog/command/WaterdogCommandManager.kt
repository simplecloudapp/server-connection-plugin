package app.simplecloud.plugin.connection.waterdog.command

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.command.Command
import dev.waterdog.waterdogpe.command.CommandSender
import dev.waterdog.waterdogpe.command.CommandSettings
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CopyOnWriteArrayList

class WaterdogCommandManager(
    private val plugin: WaterdogConnectionPlugin,
) {

    private val commands = CopyOnWriteArrayList<String>()
    private val miniMessage = MiniMessage.miniMessage()
    private val serializer = PlainTextComponentSerializer.plainText()
    private val logger = LogManager.getLogger(WaterdogCommandManager::class.java)

    fun registerCommands() {
        val commands = plugin.connectionPlugin.commandConfig.get().commands
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommands() {
        commands.forEach { plugin.proxy.commandMap.unregisterCommand(it) }
        commands.clear()
    }

    private fun registerCommand(command: CommandEntry) {
        val settings = CommandSettings.builder()
            .setAliases(*command.aliases.toTypedArray())
            .apply {
                if (command.permission.isNotEmpty()) {
                    permission = command.permission
                }
            }
            .build()

        val connectionCommand = object : Command(command.name, settings) {
            override fun onExecute(sender: CommandSender, alias: String?, args: Array<out String>): Boolean {
                val player = sender as? ProxiedPlayer ?: return false
                handleCommand(player, command, alias ?: command.name)
                return true
            }
        }

        plugin.proxy.commandMap.registerCommand(connectionCommand)
        commands.add(command.name)
    }

    private fun handleCommand(player: ProxiedPlayer, command: CommandEntry, alias: String) {
        val config = plugin.connectionPlugin.connectionConfig.get()

        if (command.permission.isNotEmpty() && !player.hasPermission(command.permission)) {
            logger.warn(
                "Player ${player.name} (${player.uniqueId}) tried connection command /$alias " +
                    "without permission ${command.permission}."
            )
            return
        }

        val currentServerName = player.serverInfo?.serverName
        val serverNames = plugin.proxy.servers.map { it.serverName }
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
                            "(${player.uniqueId}) via /$alias because current server " +
                            "$currentServerName is not allowed by from=${target.from}."
                    )
                    continue
                }
            }

            val connection = ConnectionResolver.findConnection(target.name, config.connections)
            if (connection == null) {
                logger.warn(
                    "Connection command /$alias for player ${player.name} (${player.uniqueId}) " +
                        "references unknown target connection ${target.name}."
                )
                continue
            }

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                player.hasPermission(permission)
            }
            if (failedRule != null) {
                logger.warn(
                    "Connection command /$alias for player ${player.name} (${player.uniqueId}) " +
                        "was blocked by rule ${failedRule.name} on target connection ${connection.name}."
                )
                continue
            }

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) {
                logger.warn(
                    "Connection command /$alias for player ${player.name} (${player.uniqueId}) " +
                        "found no registered servers matching target connection ${connection.name}."
                )
                continue
            }

            val serverInfo = matchingNames
                .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
                .minByOrNull { it.players.size }
            if (serverInfo == null) {
                logger.warn(
                    "Connection command /$alias for player ${player.name} (${player.uniqueId}) " +
                        "matched servers $matchingNames for target connection ${connection.name}, " +
                        "but none are registered on Waterdog."
                )
                continue
            }

            if (currentServerName != null && serverInfo.serverName.equals(currentServerName, ignoreCase = true)) {
                sendMessage(player, command.messages.alreadyConnected)
                return
            }

            logger.info(
                "Sending player ${player.name} (${player.uniqueId}) from ${currentServerName ?: "<none>"} " +
                    "to ${serverInfo.serverName} via connection command /$alias " +
                    "(configured command: ${command.name}, target connection: ${connection.name})."
            )
            try {
                player.connect(serverInfo)
            } catch (exception: Exception) {
                logger.warn(
                    "Connection command /$alias failed while sending player ${player.name} " +
                        "(${player.uniqueId}) to ${serverInfo.serverName}.",
                    exception
                )
            }
            return
        }

        logger.warn(
            "Connection command /$alias for player ${player.name} (${player.uniqueId}) " +
                "found no usable target connection. Current server: ${currentServerName ?: "<none>"}."
        )
        sendMessage(player, command.messages.noTargetConnectionFound)
    }

    private fun sendMessage(player: ProxiedPlayer, rawMessage: String) {
        val component = miniMessage.deserialize(rawMessage)
        val plain = serializer.serialize(component)
        player.sendMessage(plain)
    }

}
