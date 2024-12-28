package app.simplecloud.plugin.connection.waterdog

import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class JoinHandler(
    private val server: ProxyServer,
    private val serverConnection: ServerConnectionPlugin<ProxiedPlayer>
) : IJoinHandler {
    override fun determineServer(player: ProxiedPlayer): ServerInfo {
        val serverConnectionInfoName = serverConnection.getServerNameForLogin(player)
        if (serverConnectionInfoName == null) {
            player.disconnect(serverConnection.config.fallbackConnectionsConfig.noTargetConnectionFoundMessage)
        }

        return server.getServerInfo(serverConnectionInfoName)
    }
}