package app.simplecloud.plugin.connection.velocity.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future

class KickedFromServerListener(
    private val proxyServer: ProxyServer,
    private val scope: CoroutineScope,
    private val config: () -> ConnectionConfig,
    private val messageConfig: () -> MessageConfig
) {

    @Subscribe(order = PostOrder.NORMAL)
    fun onKickedFromServer(event: KickedFromServerEvent): EventTask {
        return EventTask.resumeWhenComplete(
            scope.future { handleFallback(event) }
        )
    }

    private fun handleFallback(event: KickedFromServerEvent) {
        val currentConfig = config()
        val messages = messageConfig()

        if (!currentConfig.fallback.enabled) return

        val kickedFromServerName = event.server.serverInfo.name
        val resolver = ConnectionResolver(currentConfig)

        val entry = resolver.resolve(
            targetConnections = currentConfig.fallback.targetConnections,
            currentServerName = kickedFromServerName
        )

        if (entry == null) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(messages.deserialize(messages.kick.noTargetConnection))
            return
        }

        val servers = proxyServer.allServers
            .filter { entry.serverNameMatcher.matches(it.serverInfo.name) }
            .filter { !it.serverInfo.name.equals(kickedFromServerName, ignoreCase = true) }

        val targetServer = servers.randomOrNull()

        if (targetServer == null) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(messages.deserialize(messages.kick.noFallbackServers))
            return
        }

        event.result = KickedFromServerEvent.RedirectPlayer.create(targetServer, event.serverKickReason.orElse(null))
    }
}