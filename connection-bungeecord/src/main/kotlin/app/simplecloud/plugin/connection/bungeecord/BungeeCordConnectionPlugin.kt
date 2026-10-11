package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.plugin.connection.bungeecord.command.BungeeCordCommandManager
import app.simplecloud.plugin.connection.bungeecord.command.ConnectionCommand
import app.simplecloud.plugin.connection.bungeecord.listener.BungeeCordTargetResolver
import app.simplecloud.plugin.connection.bungeecord.listener.ServerConnectListener
import app.simplecloud.plugin.connection.bungeecord.listener.ServerKickListener
import app.simplecloud.plugin.connection.bungeecord.registration.BungeeCordServerRegistry
import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin

class BungeeCordConnectionPlugin : Plugin() {

    private val audiences = BungeeAudiences.create(this)
    private val commandManager = BungeeCordCommandManager(this, audiences)
    private val serverRegistry = BungeeCordServerRegistry(proxy)

    val connectionPlugin = ConnectionPlugin(
        dataFolder,
        serverRegistry
    )

    override fun onEnable() {
        cleanupServers()
        registerListeners()
        registerCommands()
        connectionPlugin.start()
    }

    override fun onDisable() {
        commandManager.unregisterCommands()
        audiences.close()
        connectionPlugin.shutdown()
    }

    fun reload() {
        connectionPlugin.reload()
        commandManager.unregisterCommands()
        commandManager.registerCommands()
    }

    private fun cleanupServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            serverRegistry.clear()
        }
    }

    private fun registerListeners() {
        val targetResolver = BungeeCordTargetResolver(proxy)
        proxy.pluginManager.registerListener(this, ServerConnectListener(this, targetResolver))
        proxy.pluginManager.registerListener(this, ServerKickListener(this, targetResolver))
    }

    private fun registerCommands() {
        commandManager.registerCommands()
        proxy.pluginManager.registerCommand(this, ConnectionCommand(this, audiences))
    }

}
