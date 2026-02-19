package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerConnectListener(
    private val proxy: ProxyServer,
    private val config: () -> ConnectionConfig
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onServerConnect(event: ServerConnectEvent) {
        if (event.reason != ServerConnectEvent.Reason.JOIN_PROXY) return

        val currentConfig = config()
        if (!currentConfig.networkJoinTargets.enabled) return

        val resolver = ConnectionResolver(currentConfig)
        val entry = resolver.resolve(
            targetConnections = currentConfig.networkJoinTargets.targetConnections,
            currentServerName = null
        ) ?: return

        val targetServer = proxy.servers.values
            .filter { entry.serverNameMatcher.matches(it.name) }
            .randomOrNull() ?: return

        event.target = targetServer
    }
}