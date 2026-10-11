package app.simplecloud.plugin.connection.velocity.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import kotlin.jvm.optionals.getOrNull

class VelocityServerRegistry(
    private val proxy: ProxyServer
) : ServerRegistry {

    override fun register(proxyName: String, server: RegisteredServer) {
        unregisterExisting(proxyName)
        proxy.registerServer(ServerInfo(proxyName, InetSocketAddress.createUnresolved(server.ip, server.port)))
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        unregisterExisting(proxyName)
    }

    private fun unregisterExisting(proxyName: String) {
        val existing = proxy.getServer(proxyName).getOrNull() ?: return
        proxy.unregisterServer(existing.serverInfo)
    }
}
