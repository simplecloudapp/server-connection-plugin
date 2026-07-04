package app.simplecloud.plugin.connection.velocity.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress

class VelocityServerRegistry(
    private val proxy: ProxyServer
) : ServerRegistry {

    override fun register(proxyName: String, server: RegisteredServer) {
        val info = ServerInfo(
            proxyName,
            InetSocketAddress.createUnresolved(server.ip, server.port)
        )
        proxy.getServer(proxyName).ifPresent {
            proxy.unregisterServer(it.serverInfo)
        }
        proxy.registerServer(info)
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        proxy.getServer(proxyName).ifPresent {
            proxy.unregisterServer(it.serverInfo)
        }
    }
}
