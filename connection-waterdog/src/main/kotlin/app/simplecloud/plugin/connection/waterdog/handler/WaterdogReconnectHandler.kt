package app.simplecloud.plugin.connection.waterdog.handler

import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class WaterdogReconnectHandler(
    private val plugin: WaterdogConnectionPlugin,
) : IReconnectHandler {

    override fun getFallbackServer(
        player: ProxiedPlayer?,
        oldServer: ServerInfo?,
        reason: ReconnectReason?,
        kickMessage: String?
    ): ServerInfo? {
        if (player == null) return null

        val config = plugin.connectionPlugin.connectionConfig

        if (!config.fallback.enabled) return null

        val kickedServerName = oldServer?.serverName
        val serverNames = plugin.proxy.servers.map { it.serverName }
        val sortedTargets = config.fallback.targetConnections.sortedByDescending { it.priority }

        for (target in sortedTargets) {
            if (target.from.isNotEmpty() && kickedServerName != null) {
                val isFromAllowed = target.from.any { connectionName ->
                    ConnectionResolver.isServerInConnection(
                        kickedServerName, connectionName, config.connections, serverNames
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

            val serverInfo = matchingNames
                .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
                .filter { it.serverName != kickedServerName }
                .minByOrNull { it.players.size }

            if (serverInfo != null) return serverInfo
        }

        return null
    }

}
