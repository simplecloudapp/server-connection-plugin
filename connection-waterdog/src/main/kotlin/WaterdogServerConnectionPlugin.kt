import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import dev.waterdog.waterdogpe.plugin.Plugin

class WaterdogServerConnectionPlugin: Plugin() {


    private val serverConnection = ServerConnectionPlugin<ProxiedPlayer>(
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

    override fun onEnable() {
        proxy.reconnectHandler = ReconnectHandler(proxy, serverConnection)
        proxy.joinHandler = JoinHandler(proxy, serverConnection)
    }
}