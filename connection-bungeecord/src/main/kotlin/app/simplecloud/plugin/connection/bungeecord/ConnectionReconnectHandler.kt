package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.ReconnectHandler
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.connection.Server

/**
 * @author Niklas Nieberler
 */

class ConnectionReconnectHandler(
    private val serverConnection: ServerConnectionPlugin<ProxiedPlayer>,
    private val proxyServer: ProxyServer,
) : ReconnectHandler {

    override fun getServer(player: ProxiedPlayer?): ServerInfo {
        if (player == null)
            throw NullPointerException("failed to find player")
        val serverName = this.serverConnection.getServerNameToConnect(player)
            ?: throw NullPointerException("failed to find connected server")
        return this.proxyServer.getServerInfo(serverName)
    }

    override fun setServer(player: ProxiedPlayer?) {}

    override fun save() {}

    override fun close() {}
}