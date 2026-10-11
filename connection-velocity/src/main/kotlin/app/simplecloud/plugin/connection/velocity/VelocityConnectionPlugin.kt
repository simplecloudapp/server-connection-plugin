package app.simplecloud.plugin.connection.velocity

import app.simplecloud.plugin.connection.shared.ConnectionPlugin
import app.simplecloud.plugin.connection.velocity.command.ConnectionCommand
import app.simplecloud.plugin.connection.velocity.command.VelocityCommandManager
import app.simplecloud.plugin.connection.velocity.listener.KickedFromServerListener
import app.simplecloud.plugin.connection.velocity.listener.PlayerChooseInitialServerListener
import app.simplecloud.plugin.connection.velocity.registration.VelocityServerRegistry
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path

@Plugin(
    id = "simplecloud-connection",
    name = "simplecloud-connection",
    version = BuildConstants.VERSION,
    authors = ["Fllip", "hmtill"],
    description = "Central proxy plugin for SimpleCloud v3 that registers servers and manages player routing.",
    url = "https://github.com/simplecloudapp/server-connection-plugin",
    dependencies = [
        Dependency("simplecloud-api")
    ]
)
class VelocityConnectionPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
) {

    private val registry = VelocityServerRegistry(server)
    private val commandManager = VelocityCommandManager(this, server)

    val connectionPlugin = ConnectionPlugin(dataDirectory.toFile(), registry)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        cleanupServers()
        registerListeners()
        registerCommands()
        connectionPlugin.start()
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        commandManager.unregisterCommands()
        connectionPlugin.shutdown()
    }

    fun reload() {
        connectionPlugin.reload()
        commandManager.unregisterCommands()
        commandManager.registerCommands()
    }

    private fun cleanupServers() {
        if (connectionPlugin.connectionConfig.get().registration.enabled) {
            server.allServers.forEach { registeredServer ->
                server.unregisterServer(registeredServer.serverInfo)
            }
        }
    }

    private fun registerListeners() {
        server.eventManager.register(this, PlayerChooseInitialServerListener(this, server))
        server.eventManager.register(this, KickedFromServerListener(this, server))
    }

    private fun registerCommands() {
        commandManager.registerCommands()

        val meta = server.commandManager.metaBuilder("connection")
            .plugin(this)
            .build()
        server.commandManager.register(meta, ConnectionCommand(this))
    }
}
