package app.simplecloud.plugin.connection.velocity

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import app.simplecloud.plugin.connection.velocity.registration.VelocityServerRegistry
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress
import java.nio.file.Path

@Plugin(
    id = "connection-velocity",
    name = "connection-velocity",
    version = "1.0.0",
    authors = ["Fllip", "hmtill"],
    url = "https://github.com/simplecloudapp/server-connection-plugin"
)
class VelocityConnectionPlugin @Inject constructor(
    private val server: ProxyServer,
    @DataDirectory val dataDirectory: Path
) {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(VelocityConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectionPlugin = ConnectionPlugin(
        dataDirectory.toString(),
        api,
        VelocityServerRegistry(this, server)
    )

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        logger.info("Initialize velocity-connection plugin...")
        connectionPlugin.config.save("config", connectionPlugin.connectionConfig)
        cleanupServers()
        registerAdditionalServers()
        scope.launch {
            connectionPlugin.start()
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("Shutting down velocity-connection plugin...")
        scope.launch {
            connectionPlugin.shutdown()
        }
        scope.cancel()
    }

    private fun cleanupServers() {
        server.allServers.forEach {
            server.unregisterServer(it.serverInfo)
        }
    }

    private fun registerAdditionalServers() {
        val additionalServers = connectionPlugin.connectionConfig.registration.additionalServers
        additionalServers.forEach {
            val info = ServerInfo(it.name, InetSocketAddress.createUnresolved(it.address, it.port.toInt()))
            server.registerServer(info)
            logger.info("Additional server ${info.name} has been registered!")
        }
    }
}