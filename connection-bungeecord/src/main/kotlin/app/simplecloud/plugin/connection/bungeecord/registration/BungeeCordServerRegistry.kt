package app.simplecloud.plugin.connection.bungeecord.registration

import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.config.ServerInfo
import java.net.InetSocketAddress

class BungeeCordServerRegistry(
    private val proxy: ProxyServer
) : ServerRegistry {

    override fun register(proxyName: String, server: RegisteredServer) {
        val address = InetSocketAddress.createUnresolved(server.ip, server.port)
        val info: ServerInfo = proxy.constructServerInfo(
            proxyName,
            address,
            server.serverId,
            server.properties.getOrDefault("proxy-restricted", "false").toString().toBoolean()
        )
        proxy.servers[proxyName] = info
        refreshServerPriorities()
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        proxy.servers.remove(proxyName)
        refreshServerPriorities()
    }

    fun refreshServerPriorities() {
        val serverNames = proxy.servers.keys.toList()
        proxy.config.listeners.forEach { listener ->
            listener.serverPriority.removeIf { priority ->
                serverNames.none { it.equals(priority, ignoreCase = true) }
            }
            serverNames.forEach { serverName ->
                if (listener.serverPriority.none { it.equals(serverName, ignoreCase = true) }) {
                    listener.serverPriority.add(serverName)
                }
            }
        }
    }
}
