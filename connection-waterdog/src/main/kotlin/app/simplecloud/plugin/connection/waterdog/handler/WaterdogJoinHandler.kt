package app.simplecloud.plugin.connection.waterdog.handler

import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class WaterdogJoinHandler(
    private val plugin: WaterdogConnectionPlugin,
) : IJoinHandler {

    override fun determineServer(player: ProxiedPlayer): ServerInfo? {
        val config = plugin.connectionPlugin.connectionConfig.get()

        val virtualHost = player.loginData.joinHostname
        val route = config.address.routes.find { it.subdomain == virtualHost }
        if (route != null) {
            val connection = ConnectionResolver.findConnection(route.targetConnection, config.connections)
            if (connection != null) {
                val serverNames = plugin.proxy.servers.map { it.serverName }
                val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                val serverInfo = matchingNames
                    .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
                    .minByOrNull { it.players.size }
                if (serverInfo != null) return serverInfo
            }
        }

        if (!config.networkJoinTargets.enabled) return null

        val serverNames = plugin.proxy.servers.map { it.serverName }
        val sortedTargets = config.networkJoinTargets.targetConnections.sortedByDescending { it.priority }

        for (target in sortedTargets) {
            val connection = ConnectionResolver.findConnection(target.name, config.connections) ?: continue

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            val serverInfo = matchingNames
                .mapNotNull { name -> plugin.proxy.getServerInfo(name) }
                .minByOrNull { it.players.size }

            if (serverInfo != null) return serverInfo
        }

        return null
    }

}
