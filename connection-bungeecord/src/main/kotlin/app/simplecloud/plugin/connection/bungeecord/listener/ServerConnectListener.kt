package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class ServerConnectListener(
    private val plugin: BungeeCordConnectionPlugin,
    private val audiences: BungeeAudiences,
) : Listener {

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        if (event.reason != ServerConnectEvent.Reason.JOIN_PROXY) return

        val config = plugin.connectionPlugin.connectionConfig.get()
        val messages = plugin.connectionPlugin.messageConfig.get()

        val virtualHost = event.player.pendingConnection.virtualHost?.hostName
        if (virtualHost != null) {
            val route = config.address.routes.find { it.subdomain == virtualHost }
            if (route != null) {
                val connection = ConnectionResolver.findConnection(route.targetConnection, config.connections)
                if (connection != null) {
                    val serverNames = plugin.proxy.servers.keys.toList()
                    val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                    val targetServer = matchingNames
                        .mapNotNull { plugin.proxy.servers[it] }
                        .minByOrNull { it.players.size }
                    if (targetServer != null) {
                        event.target = targetServer
                        return
                    }
                }
            }
        }

        if (!config.networkJoinTargets.enabled) return

        val serverNames = plugin.proxy.servers.keys.toList()
        val sortedTargets = config.networkJoinTargets.targetConnections.sortedByDescending { it.priority }

        for (target in sortedTargets) {
            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                event.player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            val targetServer = matchingNames
                .mapNotNull { plugin.proxy.servers[it] }
                .minByOrNull { it.players.size }
                ?: continue

            event.target = targetServer
            return
        }

        event.isCancelled = true
        val audience = audiences.player(event.player)
        audience.sendMessage(messages.send(messages.kick.noTargetConnection))
        event.player.disconnect()
    }

}
