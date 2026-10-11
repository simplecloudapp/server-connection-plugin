package app.simplecloud.plugin.connection.velocity.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlin.jvm.optionals.getOrNull

class PlayerChooseInitialServerListener(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {

    @Subscribe
    fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messages = plugin.connectionPlugin.messageConfig.get()

        val routeServer = findRouteServer(event.player, config)
        if (routeServer != null) {
            event.setInitialServer(routeServer)
            return
        }

        if (!config.networkJoinTargets.enabled) return

        val serverNames = proxy.allServers.map { it.serverInfo.name }
        val sortedTargets = config.networkJoinTargets.targetConnections.sortedByDescending { it.priority }

        for (target in sortedTargets) {
            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                event.player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            val server = matchingNames
                .mapNotNull { name -> proxy.getServer(name).getOrNull() }
                .minByOrNull { it.playersConnected.size }
                ?: continue

            event.setInitialServer(server)
            return
        }

        event.setInitialServer(null)
        event.player.disconnect(messages.msg(messages.kick.noTargetConnection))
    }

    private fun findRouteServer(player: Player, config: ConnectionConfig): RegisteredServer? {
        val virtualHost = player.virtualHost.getOrNull()?.hostName ?: return null
        val route = ConnectionResolver.findRoute(virtualHost, config.address.routes) ?: return null
        val connection = ConnectionResolver.findConnection(route.targetConnection, config.connections) ?: return null

        val failedRule = ConnectionResolver.checkRules(connection) { permission ->
            player.hasPermission(permission)
        }
        if (failedRule != null) return null

        val serverNames = proxy.allServers.map { it.serverInfo.name }
        return ConnectionResolver.findMatchingServerNames(connection, serverNames)
            .mapNotNull { name -> proxy.getServer(name).getOrNull() }
            .minByOrNull { it.playersConnected.size }
    }

}
