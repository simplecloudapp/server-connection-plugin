package app.simplecloud.plugin.connection.waterdog

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import app.simplecloud.plugin.connection.waterdog.registration.WaterDogServerRegistry
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import dev.waterdog.waterdogpe.plugin.Plugin
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress

class WaterDogConnectionPlugin : Plugin() {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(WaterDogConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectionPlugin = ConnectionPlugin(
        dataFolder.toString(),
        api,
        WaterDogServerRegistry(this, proxy)
    )

    override fun onEnable() {
        logger.info("Initialize waterdog-connection plugin...")
        connectionPlugin.config.save("config", connectionPlugin.connectionConfig)
        cleanupServers()
        registerAdditionalServers()
        scope.launch {
            connectionPlugin.start()
        }
    }

    override fun onDisable() {
        logger.info("Shutting down waterdog-connection plugin...")
        scope.launch {
            connectionPlugin.shutdown()
        }
        scope.cancel()
    }

    private fun cleanupServers() {
        proxy.servers.forEach {
            proxy.removeServerInfo(it.serverName)
        }
    }

    private fun registerAdditionalServers() {
        val additionalServers = connectionPlugin.connectionConfig.registration.additionalServers
        additionalServers.forEach {
            val address = InetSocketAddress.createUnresolved(it.address, it.port.toInt())
            val info = BedrockServerInfo(it.name, address, address)
            proxy.registerServerInfo(info)
            logger.info("Additional server ${info.serverName} has been registered!")
        }
    }
}