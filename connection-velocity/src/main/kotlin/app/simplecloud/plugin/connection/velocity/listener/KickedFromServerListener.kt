package app.simplecloud.plugin.connection.velocity.listener

import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.proxy.ProxyServer

class KickedFromServerListener(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer,
) {

    @Subscribe
    fun onKickedFromServer(event: KickedFromServerEvent) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messages = plugin.connectionPlugin.messageConfig.get()

        if (!config.fallback.enabled) return

        val kickedServerName = event.server.serverInfo.name
        val serverNames = proxy.allServers.map { it.serverInfo.name }
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

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                event.player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            val server = matchingNames
                .mapNotNull { proxy.getServer(it).orElse(null) }
                .filter { it.serverInfo.name != kickedServerName }
                .minByOrNull { it.playersConnected.size }
                ?: continue

            event.result = KickedFromServerEvent.RedirectPlayer.create(server)
            return
        }

        event.result = KickedFromServerEvent.DisconnectPlayer.create(
            messages.send(messages.kick.noFallbackServers)
        )
    }

}
