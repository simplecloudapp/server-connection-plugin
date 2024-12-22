
import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class ReconnectHandler(
    private val server: ProxyServer,
    private val serverConnection: ServerConnectionPlugin<ProxiedPlayer>
) : IReconnectHandler {
    override fun getFallbackServer(proxiedPlayer: ProxiedPlayer, serverInfo: ServerInfo, s: String): ServerInfo? {
        return server.getServerInfo(serverConnection.getServerNameForLogin(proxiedPlayer))
    }
}