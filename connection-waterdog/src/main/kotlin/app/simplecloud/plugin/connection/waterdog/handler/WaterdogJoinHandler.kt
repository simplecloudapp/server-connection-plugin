package app.simplecloud.plugin.connection.waterdog.handler

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class WaterdogJoinHandler(
    private val plugin: WaterdogConnectionPlugin,
    private val defaultHandler: IJoinHandler,
) : IJoinHandler {

    override fun determineServer(player: ProxiedPlayer): ServerInfo? {
        val config = plugin.connectionPlugin.connectionConfig.get()

        val routeServer = findRouteServer(player, config)
        if (routeServer != null) return routeServer
        if (!config.networkJoinTargets.enabled) return defaultHandler.determineServer(player)

        val sortedTargets = config.networkJoinTargets.targetConnections.sortedByDescending { it.priority }
        for (target in sortedTargets) {
            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue
            val serverInfo = findServer(player, connection)
            if (serverInfo != null) return serverInfo
        }

        return null
    }

    private fun findRouteServer(player: ProxiedPlayer, config: ConnectionConfig): ServerInfo? {
        val virtualHost = player.loginData.joinHostname ?: return null
        val route = ConnectionResolver.findRoute(virtualHost, config.address.routes) ?: return null
        val connection = ConnectionResolver.findConnection(route.targetConnection, config.connections) ?: return null
        return findServer(player, connection)
    }

    private fun findServer(player: ProxiedPlayer, connection: ConnectionEntry): ServerInfo? {
        val failedRule = ConnectionResolver.checkRules(connection) { permission ->
            player.hasPermission(permission)
        }
        if (failedRule != null) return null

        val serverNames = plugin.proxy.servers.map { it.serverName }
        return ConnectionResolver.findMatchingServerNames(connection, serverNames)
            .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
            .minByOrNull { it.players.size }
    }

}
