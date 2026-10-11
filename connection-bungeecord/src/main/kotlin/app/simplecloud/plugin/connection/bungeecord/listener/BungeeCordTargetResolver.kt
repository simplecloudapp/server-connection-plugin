package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionEntry
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer

class BungeeCordTargetResolver(
    private val proxy: ProxyServer,
) {

    fun findJoinTarget(player: ProxiedPlayer, config: ConnectionConfig): ServerInfo? {
        val routeServer = findRouteServer(player, config)
        if (routeServer != null) return routeServer

        if (!config.networkJoinTargets.enabled) return null

        val sortedTargets = config.networkJoinTargets.targetConnections.sortedByDescending { it.priority }
        for (target in sortedTargets) {
            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue
            val server = findServer(player, connection, null)
            if (server != null) return server
        }
        return null
    }

    fun findFallbackTarget(player: ProxiedPlayer, kickedServerName: String, config: ConnectionConfig): ServerInfo? {
        val serverNames = proxy.servers.keys.toList()
        val sortedTargets = config.fallback.targetConnections.sortedByDescending { it.priority }

        for (target in sortedTargets) {
            if (target.from.isNotEmpty()) {
                val isFromAllowed = target.from.any { connectionName ->
                    ConnectionResolver.isServerInConnection(
                        kickedServerName, connectionName, config.connections, serverNames
                    )
                }
                if (!isFromAllowed) continue
            }

            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue
            val server = findServer(player, connection, kickedServerName)
            if (server != null) return server
        }
        return null
    }

    private fun findRouteServer(player: ProxiedPlayer, config: ConnectionConfig): ServerInfo? {
        val virtualHost = player.pendingConnection.virtualHost?.hostName ?: return null
        val route = ConnectionResolver.findRoute(virtualHost, config.address.routes) ?: return null
        val connection = ConnectionResolver.findConnection(route.targetConnection, config.connections) ?: return null
        return findServer(player, connection, null)
    }

    private fun findServer(player: ProxiedPlayer, connection: ConnectionEntry, excludedServerName: String?): ServerInfo? {
        val failedRule = ConnectionResolver.checkRules(connection) { permission ->
            player.hasPermission(permission)
        }
        if (failedRule != null) return null

        val serverNames = proxy.servers.keys.toList()
        return ConnectionResolver.findMatchingServerNames(connection, serverNames)
            .mapNotNull { name -> proxy.servers[name] }
            .filter { server -> !server.name.equals(excludedServerName, ignoreCase = true) }
            .minByOrNull { server -> server.players.size }
    }

}
