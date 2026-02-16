package app.simplecloud.plugin.connection.bungeecord.registration

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import app.simplecloud.plugin.connection.shared.resolver.RegisteredServerResolver
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import java.net.InetSocketAddress

class BungeeCordServerRegistry(
    private val plugin: BungeeCordConnectionPlugin,
    private val proxy: ProxyServer
) : ServerRegistry {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val address = InetSocketAddress.createUnresolved(server.ip, server.port)
        val name = RegisteredServerResolver.resolve(
            server,
            plugin.connectionPlugin.connectionConfig.registration
        )
        val info: ServerInfo = ProxyServer.getInstance().constructServerInfo(
            name,
            address,
            server.serverId,
            server.properties.getOrDefault("proxy-restricted", "false").toString().toBoolean()
        )
        proxy.servers[name] = info
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        val name = RegisteredServerResolver.resolve(
            server,
            plugin.connectionPlugin.connectionConfig.registration
        )
        proxy.servers.remove(name)
        servers.remove(server.serverId)
    }
}