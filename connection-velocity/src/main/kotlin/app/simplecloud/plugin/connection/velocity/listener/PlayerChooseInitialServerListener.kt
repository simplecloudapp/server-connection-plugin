package app.simplecloud.plugin.connection.velocity.listener

import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.ProxyServer

class PlayerChooseInitialServerListener(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {

    @Subscribe
    fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messages = plugin.connectionPlugin.messageConfig.get()

        if (config.subdomain.enabled) {
            val virtualHost = event.player.virtualHost.map { it.hostName }.orElse(null)
            if (virtualHost != null) {
                val mapping = config.subdomain.mappings.find { it.subdomain == virtualHost }
                if (mapping != null) {
                    val connection = ConnectionResolver.findConnection(mapping.targetConnection, config.connections)
                    if (connection != null) {
                        val serverNames = proxy.allServers.map { it.serverInfo.name }
                        val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
                        val server = matchingNames
                            .mapNotNull { proxy.getServer(it).orElse(null) }
                            .minByOrNull { it.playersConnected.size }
                        if (server != null) {
                            event.setInitialServer(server)
                            return
                        }
                    }
                }
            }
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
                .mapNotNull { proxy.getServer(it).orElse(null) }
                .minByOrNull { it.playersConnected.size }
                ?: continue

            event.setInitialServer(server)
            return
        }

        event.setInitialServer(null)
        event.player.disconnect(messages.send(messages.kick.noTargetConnection))
    }

}
