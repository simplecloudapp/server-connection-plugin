package app.simplecloud.plugin.connection.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.YamlConfig
import app.simplecloud.plugin.connection.shared.listener.ServerEventListener
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import org.apache.logging.log4j.LogManager

class ConnectionPlugin(
    private val dir: String,
    private val api: CloudApi,
    private val registry: ServerRegistry,
) {

    private val logger = LogManager.getLogger(ConnectionPlugin::class.java)
    private val listener = ServerEventListener(api, registry)

    val config = YamlConfig(dir)
    val connectionConfig: ConnectionConfig
        get() = config.load<ConnectionConfig>("config") ?: ConnectionConfig()

    suspend fun start() {
        loadExistingServers()
        listener.start()
    }

    suspend fun shutdown() {
        config.close()
        listener.stop()
    }

    private fun loadExistingServers() {
        api.server().getAllServers(
            ServerQuery.create()
                .filterByState(ServerState.AVAILABLE)
                .filterByServerGroupType(GroupServerType.SERVER)
        ).thenAccept { servers ->
            logger.info("Found ${servers.size} servers")
            servers.forEach { listener.register(it) }
        }
    }
}