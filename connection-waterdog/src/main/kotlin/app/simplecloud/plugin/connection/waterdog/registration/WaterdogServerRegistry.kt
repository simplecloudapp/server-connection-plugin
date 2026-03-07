package app.simplecloud.plugin.connection.waterdog.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import app.simplecloud.plugin.connection.shared.resolver.RegisteredServerResolver
import app.simplecloud.plugin.connection.waterdog.WaterdogConnectionPlugin
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import java.net.InetSocketAddress

class WaterdogServerRegistry(
    private val plugin: WaterdogConnectionPlugin,
    private val proxy: ProxyServer
) : ServerRegistry {

    private val servers = mutableMapOf<String, RegisteredServer>()

    override fun getRegistered(): Map<String, RegisteredServer> {
        return servers
    }

    override fun register(server: RegisteredServer) {
        val address = InetSocketAddress.createUnresolved(server.ip, server.port)
        val info = BedrockServerInfo(
            RegisteredServerResolver.resolve(server, plugin.connectionPlugin.connectionConfig.get().registration),
            address,
            address
        )
        proxy.registerServerInfo(info)
        servers[server.serverId] = server
    }

    override fun unregister(server: RegisteredServer) {
        val name = RegisteredServerResolver.resolve(
            server,
            plugin.connectionPlugin.connectionConfig.get().registration
        )
        proxy.removeServerInfo(name) ?: return
        servers.remove(server.serverId)
    }
}