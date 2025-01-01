package app.simplecloud.plugin.connection.waterdog

import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import dev.waterdog.waterdogpe.plugin.Plugin

class WaterdogServerConnectionPlugin : Plugin() {
    private lateinit var serverConnection: ServerConnectionPlugin<ProxiedPlayer>
    override fun onEnable() {


        serverConnection = ServerConnectionPlugin(
            dataFolder.toPath(),
            {
                proxy.servers.map {
                    ServerConnectionInfo(
                        it.serverName,
                        it.players.size
                    )
                }
            },
            { player, permission -> player.hasPermission(permission) }
        )

        proxy.reconnectHandler = ReconnectHandler(proxy, serverConnection)
        proxy.joinHandler = JoinHandler(proxy, serverConnection)
    }
}