package app.simplecloud.plugin.connection.waterdog.command

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.command.Command
import dev.waterdog.waterdogpe.command.CommandSender
import dev.waterdog.waterdogpe.command.CommandSettings
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class WaterdogCommandManager(
    private val plugin: WaterdogConnectionPlugin,
) {

    private val commands = CopyOnWriteArrayList<Command>()
    private val serializer = PlainTextComponentSerializer.plainText()
    private val logger = LoggerFactory.getLogger(WaterdogCommandManager::class.java)

    fun registerCommands() {
        val commands = plugin.connectionPlugin.commandConfig.get().commands
        for (command in commands) {
            registerCommand(command)
        }
    }

    fun unregisterCommands() {
        val commandMap = plugin.proxy.commandMap
        // Collect the names first, so the command name and every alias of our commands are removed.
        val registeredNames = commandMap.commands
            .filter { (_, command) -> command in commands }
            .map { (name, _) -> name }
        registeredNames.forEach { name -> commandMap.unregisterCommand(name) }
        commands.clear()
    }

    private fun registerCommand(command: CommandEntry) {
        val settingsBuilder = CommandSettings.builder()
            .setAliases(*command.aliases.toTypedArray())
        if (command.permission.isNotEmpty()) {
            settingsBuilder.permission = command.permission
        }
        val settings = settingsBuilder.build()

        val connectionCommand = object : Command(command.name, settings) {
            override fun onExecute(sender: CommandSender, alias: String?, args: Array<out String>): Boolean {
                val player = sender as? ProxiedPlayer ?: return false
                handleCommand(player, command)
                return true
            }
        }

        plugin.proxy.commandMap.registerCommand(connectionCommand)
        commands.add(connectionCommand)
    }

    private fun handleCommand(player: ProxiedPlayer, command: CommandEntry) {
        if (command.permission.isNotEmpty() && !player.hasPermission(command.permission)) return

        val config = plugin.connectionPlugin.connectionConfig.get()
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
                if (!isFromAllowed) continue
            }

            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            if (currentServerName != null && matchingNames.any { name -> name.equals(currentServerName, ignoreCase = true) }) {
                sendMessage(player, command.messages.alreadyConnected)
                return
            }

            val serverInfo = matchingNames
                .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
                .minByOrNull { it.players.size }
                ?: continue

            try {
                player.connect(serverInfo)
            } catch (exception: Exception) {
                logger.warn("Failed to send player ${player.name} to ${serverInfo.serverName}", exception)
            }
            return
        }

        sendMessage(player, command.messages.noTargetConnectionFound)
    }

    private fun sendMessage(player: ProxiedPlayer, rawMessage: String) {
        val messages = plugin.connectionPlugin.messageConfig.get()
        val plain = serializer.serialize(messages.msg(rawMessage))
        player.sendMessage(plain)
    }

}
