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
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class BungeeCordCommandManager(
    private val plugin: BungeeCordConnectionPlugin,
    private val audiences: BungeeAudiences,
) {

    private val commands = CopyOnWriteArrayList<String>()
    private val logger = LoggerFactory.getLogger(BungeeCordCommandManager::class.java)

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

        if (command.permission.isNotEmpty() && !player.hasPermission(command.permission)) return

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
                audience.sendMessage(messages.msg(command.messages.alreadyConnected))
                return
            }

            val targetServer = matchingNames
                .mapNotNull { name -> plugin.proxy.servers[name] }
                .minByOrNull { it.players.size }
                ?: continue

            player.connect(
                ServerConnectRequest.builder()
                    .target(targetServer)
                    .reason(ServerConnectEvent.Reason.COMMAND)
                    .callback { result: ServerConnectRequest.Result?, error: Throwable? ->
                        if (error != null) {
                            logger.warn("Failed to send player ${player.name} to ${targetServer.name}", error)
                            return@callback
                        }
                        if (result == ServerConnectRequest.Result.SUCCESS) return@callback

                        logger.warn("Could not send player ${player.name} to ${targetServer.name} ($result)")
                    }
                    .build()
            )
            return
        }

        audience.sendMessage(messages.msg(command.messages.noTargetConnectionFound))
    }

}
