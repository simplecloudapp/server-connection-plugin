package app.simplecloud.plugin.connection.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.listener.ServerEventListener
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager
import java.io.File

class ConnectionPlugin(
    dir: String,
    private val api: CloudApi,
    registry: ServerRegistry,
) {

    private val logger = LogManager.getLogger(ConnectionPlugin::class.java)
    private val listener = ServerEventListener(api, registry) { connectionConfig.get().registration.ignoreServerGroupsAndPersistentServers }

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectionConfig = ConfigurationFactory(File(dir, "config.yml"), ConnectionConfig::class.java)
    val messageConfig = ConfigurationFactory(File(dir, "messages.yml"), MessageConfig::class.java)
    val commandConfig = ConfigurationFactory(File(dir, "commands.yml"), CommandConfig::class.java)

    init {
        File(dir).mkdirs()
        connectionConfig.loadOrCreate(ConnectionConfig())
        messageConfig.loadOrCreate(MessageConfig())
        commandConfig.loadOrCreate(CommandConfig())
    }

    suspend fun start() {
        logger.info("SimpleCloud v3 connection plugin initialized!")
        startRegistration()
    }

    fun shutdown() {
        logger.info("SimpleCloud v3 connection plugin uninitialized!")
        stopRegistration()
        scope.cancel()
    }

    fun reload() {
        connectionConfig.loadOrCreate(ConnectionConfig())
        messageConfig.loadOrCreate(MessageConfig())
        commandConfig.loadOrCreate(CommandConfig())
    }

    private suspend fun startRegistration() {
        if (connectionConfig.get().registration.enabled) {
            logger.info("Starting server registration...")
            loadExistingServers()
            listener.start()
        }
    }

    private fun stopRegistration() {
        if (connectionConfig.get().registration.enabled) {
            logger.info("Stopping server registration...")
            listener.stop()
        }
    }

    private suspend fun loadExistingServers() {
        val servers = api.server().getAllServers(
            ServerQuery.create()
                .filterByState(ServerState.AVAILABLE)
                .filterByServerGroupType(GroupServerType.SERVER)
        ).await()

        logger.info("Found ${servers.size} servers")
        servers.forEach { listener.register(it) }
    }
}