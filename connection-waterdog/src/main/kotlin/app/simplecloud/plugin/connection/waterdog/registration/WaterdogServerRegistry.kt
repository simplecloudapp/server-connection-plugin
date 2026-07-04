package app.simplecloud.plugin.connection.waterdog.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import dev.waterdog.waterdogpe.ProxyServer
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import java.net.InetSocketAddress

class WaterdogServerRegistry(
    private val proxy: ProxyServer
) : ServerRegistry {

    override fun register(proxyName: String, server: RegisteredServer) {
        val address = InetSocketAddress.createUnresolved(server.ip, server.port)
        proxy.removeServerInfo(proxyName)
        val info = BedrockServerInfo(
            proxyName,
            address,
            address
        )
        proxy.registerServerInfo(info)
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        proxy.removeServerInfo(proxyName)
    }
}
