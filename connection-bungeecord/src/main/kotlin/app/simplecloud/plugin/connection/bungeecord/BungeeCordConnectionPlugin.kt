package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.connection.bungeecord.command.BungeeCordCommandManager
import app.simplecloud.plugin.connection.bungeecord.listener.ServerConnectListener
import app.simplecloud.plugin.connection.bungeecord.listener.ServerKickListener
import app.simplecloud.plugin.connection.bungeecord.registration.BungeeCordServerRegistry
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import kotlinx.coroutines.*
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin
import org.apache.logging.log4j.LogManager
import java.net.InetSocketAddress

class BungeeCordConnectionPlugin : Plugin() {

    private val api = CloudApi.create()
    private val logger = LogManager.getLogger(BungeeCordConnectionPlugin::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val audiences = BungeeAudiences.create(this)
    private val commandManager = BungeeCordCommandManager(this, audiences)

    val connectionPlugin = ConnectionPlugin(
        dataFolder.toString(),
        api,
        BungeeCordServerRegistry(this, proxy)
    )

    override fun onEnable() {
        cleanupServers()
        registerAdditionalServers()
        registerListeners()
        registerCommands()

        scope.launch {
            connectionPlugin.start()
        }
    }

    override fun onDisable() {
        commandManager.unregisterCommands()
        audiences.close()
        scope.launch { connectionPlugin.shutdown() }
        scope.cancel()
    }

    private fun cleanupServers() {
        if (connectionPlugin.connectionConfig.registration.enabled) {
            proxy.servers.clear()
            proxy.configurationAdapter.servers.clear()
            proxy.configurationAdapter.listeners.forEach {
                it.serverPriority.clear()
            }
        }
    }

    private fun registerAdditionalServers() {
        if (connectionPlugin.connectionConfig.registration.enabled) {
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

    private fun registerListeners() {
        proxy.pluginManager.registerListener(this, ServerConnectListener(this, audiences))
        proxy.pluginManager.registerListener(this, ServerKickListener(this, audiences))
    }

    private fun registerCommands() {
        commandManager.registerCommands()
    }

}
