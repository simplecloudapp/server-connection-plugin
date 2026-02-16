package app.simplecloud.plugin.connection.velocity.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import app.simplecloud.plugin.connection.shared.resolver.RegisteredServerResolver
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull

class VelocityServerRegistry(
    private val plugin: VelocityConnectionPlugin,
    private val proxy: ProxyServer
) : ServerRegistry {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val info = ServerInfo(
            RegisteredServerResolver.resolve(server, plugin.connectionPlugin.connectionConfig.registration),
            InetSocketAddress.createUnresolved(server.ip, server.port)
        )
        proxy.registerServer(info)
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        val registered = proxy.getServer(
            RegisteredServerResolver.resolve(server, plugin.connectionPlugin.connectionConfig.registration)
        ).getOrNull() ?: return
        proxy.unregisterServer(registered.serverInfo)
        servers.remove(server.serverId)
    }
}