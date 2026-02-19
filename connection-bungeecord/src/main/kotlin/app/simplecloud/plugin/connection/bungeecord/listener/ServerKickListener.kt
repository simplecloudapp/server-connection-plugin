package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerKickListener(
    private val proxy: ProxyServer,
    private val config: () -> ConnectionConfig,
    private val messageConfig: () -> MessageConfig
) : Listener {

    private val serializer = BungeeComponentSerializer.get()

    @EventHandler(priority = EventPriority.NORMAL)
    fun onServerKick(event: ServerKickEvent) {
        val currentConfig = config()
        val messages = messageConfig()

        if (!currentConfig.fallback.enabled) return

        val kickedFromServerName = event.kickedFrom.name
        val resolver = ConnectionResolver(currentConfig)

        val entry = resolver.resolve(
            targetConnections = currentConfig.fallback.targetConnections,
            currentServerName = kickedFromServerName
        )

        if (entry == null) {
            event.isCancelled = true
            event.player.disconnect(
                *serializer.serialize(messages.deserialize(messages.kick.noTargetConnection))
            )
            return
        }

        val servers = proxy.servers.values
            .filter { entry.serverNameMatcher.matches(it.name) }
            .filter { !it.name.equals(kickedFromServerName, ignoreCase = true) }

        val targetServer = servers.randomOrNull()

        if (targetServer == null) {
            event.isCancelled = true
            event.player.disconnect(
                *serializer.serialize(messages.deserialize(messages.kick.noFallbackServers))
            )
            return
        }

        event.cancelServer = targetServer
        event.isCancelled = true
    }
}