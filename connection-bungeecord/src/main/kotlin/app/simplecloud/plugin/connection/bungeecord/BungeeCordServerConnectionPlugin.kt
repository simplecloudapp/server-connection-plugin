package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.plugin.connection.shared.PermissionChecker
import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfo
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfoGetter
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

/**
 * @author Niklas Nieberler
 */

class BungeeCordServerConnectionPlugin : Plugin(), Listener {

    private val serverConnection = ServerConnectionPlugin<ProxiedPlayer>(
        dataFolder.toPath(),
        ServerConnectionInfoGetter {
            proxy.servers.map {
                ServerConnectionInfo(
                    it.key,
                    it.value.players.size
                )
            }
        },
        PermissionChecker { player, permission -> player.hasPermission(permission) }
    )

    private val miniMessage = MiniMessage.miniMessage()

    override fun onLoad() {
        proxy.reconnectHandler = ConnectionReconnectHandler(this.serverConnection, proxy)
    }

    override fun onEnable() {
        val pluginManager = proxy.pluginManager
        pluginManager.registerListener(this, this)

        this.serverConnection.getCommandConfigs().forEach {
            val bungeeCommand = BungeeCordCommand(
                this.serverConnection,
                it,
                proxy,
                miniMessage
            )
            pluginManager.registerCommand(this, bungeeCommand)
        }
    }

    @EventHandler
    fun onServerKick(event: ServerKickEvent) {
        if (event.isCancelled) {
            return
        }

        val connectionAndTargetConfigToServerName = serverConnection.getConnectionAndNameForFallback(event.player, event.kickedFrom.name)
        if (connectionAndTargetConfigToServerName == null) {
            event.reason = TextComponent.fromArray(*BungeeComponentSerializer.get().serialize(
                miniMessage.deserialize(
                    serverConnection.config.fallbackConnectionsConfig.noTargetConnectionFoundMessage
                )
            ))
            event.cancelServer = null
            event.isCancelled = true
            return
        }

        val serverInfo = proxy.getServerInfo(connectionAndTargetConfigToServerName.second) ?: return

        event.isCancelled = true
        event.cancelServer = serverInfo
    }
}