package app.simplecloud.plugin.connection.velocity

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import app.simplecloud.plugin.connection.velocity.command.VelocityCommandManager
import app.simplecloud.plugin.connection.velocity.listener.KickedFromServerListener
import app.simplecloud.plugin.connection.velocity.listener.PlayerChooseInitialServerListener
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
    id = "simplecloud-connection",
    name = "simplecloud-connection",
    version = "1.0.0",
    authors = ["Fllip", "hmtill"],
    url = "https://github.com/simplecloudapp/server-connection-plugin"
)
class VelocityConnectionPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
) {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(VelocityConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val registry = VelocityServerRegistry(this, server)
    private val commandManager = VelocityCommandManager(this, server)

    val connectionPlugin = ConnectionPlugin(
        dataDirectory.toString(),
        api,
        registry
    )

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        cleanupServers()
        registerAdditionalServers()
        registerListeners()
        registerCommands()

        scope.launch {
            connectionPlugin.start()
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        commandManager.unregisterCommands()
        scope.launch { connectionPlugin.shutdown() }
        scope.cancel()
    }

    private fun cleanupServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            server.allServers.forEach {
                server.unregisterServer(it.serverInfo)
            }
        }
    }

    private fun registerAdditionalServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            val additionalServers = connectionPlugin.connectionConfig.get().registration.additionalServers
            additionalServers.forEach {
                val info = ServerInfo(it.name, InetSocketAddress.createUnresolved(it.address, it.port.toInt()))
                server.registerServer(info)
                logger.info("Additional server ${info.name} has been registered!")
            }
        }
    }

    private fun registerListeners() {
        server.eventManager.register(this, PlayerChooseInitialServerListener(this, server))
        server.eventManager.register(this, KickedFromServerListener(this, server))
    }

    private fun registerCommands() {
        commandManager.registerCommands()
    }
}
