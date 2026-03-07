package app.simplecloud.plugin.connection.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.config.YamlConfig
import app.simplecloud.plugin.connection.shared.listener.ServerEventListener
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import kotlinx.coroutines.future.await
import org.apache.logging.log4j.LogManager

class ConnectionPlugin(
    dir: String,
    private val api: CloudApi,
    registry: ServerRegistry,
) {

    private val logger = LogManager.getLogger(ConnectionPlugin::class.java)
    private val listener = ServerEventListener(api, registry)

    val config = YamlConfig(dir)

    val connectionConfig = config.load<ConnectionConfig>("config")
    val messageConfig = config.load<MessageConfig>("messages")
    val commandConfig = config.load<CommandConfig>("commands")

    suspend fun start() {
        logger.info("SimpleCloud v3 connection plugin initialized!")
        config.save("config", connectionConfig)
        config.save("messages", messageConfig)
        config.save("commands", commandConfig)
        startRegistration()
    }

    fun shutdown() {
        logger.info("SimpleCloud v3 connection plugin uninitialized!")
        config.close()
        if (connectionConfig.get().registration.enabled) {
            listener.stop()
        }
    }

    private suspend fun startRegistration() {
        if (connectionConfig.get().registration.enabled) {
            loadExistingServers()
            listener.start()
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