package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.bungeecord.registration.BungeeCordServerRegistry
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import kotlinx.coroutines.*
import net.md_5.bungee.api.plugin.Plugin
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress

class BungeeCordConnectionPlugin : Plugin() {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(BungeeCordConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectionPlugin = ConnectionPlugin(
        dataFolder.toString(),
        api,
        BungeeCordServerRegistry(this, proxy)
    )

    override fun onEnable() {
        logger.info("Initialize bungeecord-connection plugin...")
        connectionPlugin.config.save("config", connectionPlugin.connectionConfig)
        cleanupServers()
        registerAdditionalServers()
        scope.launch {
            connectionPlugin.start()
        }
    }

    override fun onDisable() {
        logger.info("Shutting down bungeecord-connection plugin...")
        scope.launch {
            connectionPlugin.shutdown()
        }
        scope.cancel()
    }

    private fun cleanupServers() {
        proxy.servers.clear()
        proxy.configurationAdapter.servers.clear()
        proxy.configurationAdapter.listeners.forEach {
            it.serverPriority.clear()
        }
    }

    private fun registerAdditionalServers() {
        val additionalServers = connectionPlugin.connectionConfig.registration.additionalServers
        additionalServers.forEach {
            val info = proxy.constructServerInfo(
                it.name,
                InetSocketAddress.createUnresolved(it.address, it.port.toInt()),
                it.name,
                false
            )
            proxy.servers[it.name] = info
            logger.info("Additional server ${info.name} has been registered!")
        }
    }
}