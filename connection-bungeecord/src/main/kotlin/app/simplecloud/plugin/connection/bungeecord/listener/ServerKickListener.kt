package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerKickListener(
    private val plugin: BungeeCordConnectionPlugin,
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onServerKick(event: ServerKickEvent) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messageConfig = plugin.connectionPlugin.messageConfig.get()

        if (!config.fallback.enabled) return

        val kickedServerName = event.kickedFrom.name
        val serverNames = plugin.proxy.servers.keys.toList()
        val sortedTargets = config.fallback.targetConnections.sortedByDescending { it.priority }

        for ((name, _, from) in sortedTargets) {
            if (from.isNotEmpty()) {
                val isFromAllowed = from.any { connectionName ->
                    ConnectionResolver.isServerInConnection(
                        kickedServerName, connectionName, config.connections, serverNames
                    )
                }
                if (!isFromAllowed) continue
            }

            val connection = ConnectionResolver.findConnection(name, config.connections) ?: continue

            val failedRule = ConnectionResolver.checkRules(connection) { permission ->
                event.player.hasPermission(permission)
            }
            if (failedRule != null) continue

            val matchingNames = ConnectionResolver.findMatchingServerNames(connection, serverNames)
            if (matchingNames.isEmpty()) continue

            val targetServer = matchingNames
                .mapNotNull { plugin.proxy.servers[it] }
                .filter { it.name != kickedServerName }
                .minByOrNull { it.players.size }
                ?: continue

            event.cancelServer = targetServer
            event.isCancelled = true
            return
        }

        event.reason = TextComponent.fromArray(
            *BungeeComponentSerializer.get().serialize(
                messageConfig.msg(messageConfig.kick.noFallbackServers)
            )
        )
    }

}
