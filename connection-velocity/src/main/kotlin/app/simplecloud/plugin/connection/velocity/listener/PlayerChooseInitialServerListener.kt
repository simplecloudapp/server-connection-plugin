package app.simplecloud.plugin.connection.velocity.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.ProxyServer

class PlayerChooseInitialServerListener(
    private val proxyServer: ProxyServer,
    private val config: () -> ConnectionConfig
) {

    @Subscribe(order = PostOrder.NORMAL)
    fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        handleJoin(event)
    }

    private fun handleJoin(event: PlayerChooseInitialServerEvent) {
        val currentConfig = config()

        if (!currentConfig.networkJoinTargets.enabled) return

        val resolver = ConnectionResolver(currentConfig)

        val entry = resolver.resolve(
            targetConnections = currentConfig.networkJoinTargets.targetConnections,
            currentServerName = null
        )

        if (entry == null) {
            return
        }

        val targetServer = proxyServer.allServers
            .filter { entry.serverNameMatcher.matches(it.serverInfo.name) }
            .randomOrNull()

        if (targetServer == null) {
            return
        }

        event.setInitialServer(targetServer)
    }
}