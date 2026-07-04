package app.simplecloud.plugin.connection.waterdog

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import app.simplecloud.plugin.connection.waterdog.command.ConnectionCommand
import app.simplecloud.plugin.connection.waterdog.command.WaterdogCommandManager
import app.simplecloud.plugin.connection.waterdog.handler.WaterdogJoinHandler
import app.simplecloud.plugin.connection.waterdog.handler.WaterdogReconnectHandler
import app.simplecloud.plugin.connection.waterdog.registration.WaterdogServerRegistry
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo
import dev.waterdog.waterdogpe.plugin.Plugin
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress

class WaterdogConnectionPlugin : Plugin() {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(WaterdogConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandManager = WaterdogCommandManager(this)

    val connectionPlugin = ConnectionPlugin(
        dataFolder.toString(),
        api,
        WaterdogServerRegistry(proxy)
    )

    override fun onEnable() {
        cleanupServers()
        registerAdditionalServers()
        registerHandlers()
        registerCommands()

        scope.launch {
            connectionPlugin.start()
        }
    }

    override fun onDisable() {
        commandManager.unregisterCommands()
        connectionPlugin.shutdown()
        scope.cancel()
    }

    private fun cleanupServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            proxy.servers.forEach {
                proxy.removeServerInfo(it.serverName)
            }
        }
    }

    private fun registerAdditionalServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            val additionalServers = connectionPlugin.connectionConfig.get().registration.additionalServers
            additionalServers.forEach {
                val address = InetSocketAddress.createUnresolved(it.address, it.port)
                val info = BedrockServerInfo(it.name, address, address)
                proxy.registerServerInfo(info)
                logger.info("Additional server ${info.serverName} has been registered!")
            }
        }
    }

    private fun registerHandlers() {
        proxy.joinHandler = WaterdogJoinHandler(this)
        proxy.reconnectHandler = WaterdogReconnectHandler(this)
    }

    private fun registerCommands() {
        commandManager.registerCommands()
        proxy.commandMap.registerCommand(ConnectionCommand(this))
    }

}
