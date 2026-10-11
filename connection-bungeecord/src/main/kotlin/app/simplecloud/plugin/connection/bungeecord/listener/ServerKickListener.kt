package app.simplecloud.plugin.connection.bungeecord.listener

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerKickListener(
    private val plugin: BungeeCordConnectionPlugin,
    private val targetResolver: BungeeCordTargetResolver,
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onServerKick(event: ServerKickEvent) {
        val config = plugin.connectionPlugin.connectionConfig.get()
        val messageConfig = plugin.connectionPlugin.messageConfig.get()

        if (!config.fallback.enabled) return
        if (event.state == ServerKickEvent.State.CONNECTING && event.player.server != null) return

        val targetServer = targetResolver.findFallbackTarget(event.player, event.kickedFrom.name, config)
        if (targetServer != null) {
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
