package app.simplecloud.plugin.connection.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.DefaultConfigInstaller
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.config.migration.LegacyConfigMigrator
import app.simplecloud.plugin.connection.shared.registration.ServerRegisterer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.io.File

class ConnectionPlugin(
    private val dir: File,
    registry: ServerRegistry,
) {

    private val api = CloudApi.create()
    private val logger = LoggerFactory.getLogger(ConnectionPlugin::class.java)

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectionConfig = createConnectionConfig()
    val messageConfig = createConfig("messages.yml", MessageConfig::class.java, MessageConfig())
    val commandConfig = createConfig("commands.yml", CommandConfig::class.java, CommandConfig())

    private val registerer = ServerRegisterer(api, registry, connectionConfig)

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
            registerer.start()
        }
    }

    private fun stopRegistration() {
        if (connectionConfig.get().registration.enabled) {
            logger.info("Stopping server registration...")
            registerer.stop()
        }
    }

    private suspend fun loadExistingServers() {
        val servers = api.server().getAllServers(
            ServerQuery.create()
                .filterByState(ServerState.AVAILABLE)
                .filterByServerGroupType(GroupServerType.SERVER)
        ).await()

        logger.info("Found ${servers.size} servers")
        servers.forEach { server -> registerer.register(server) }
    }

    private fun createConnectionConfig(): ConfigurationFactory<ConnectionConfig> {
        val file = File(dir, "config.yml")
        DefaultConfigInstaller.install(file.toPath(), javaClass.classLoader)

        val factory = ConfigurationFactory(file, ConnectionConfig::class.java, LegacyConfigMigrator.create(dir))
        factory.loadOrCreate(ConnectionConfig())
        return factory
    }

    private fun <E> createConfig(fileName: String, configClass: Class<E>, default: E): ConfigurationFactory<E> {
        val file = File(dir, fileName)
        DefaultConfigInstaller.install(file.toPath(), javaClass.classLoader)

        val factory = ConfigurationFactory(file, configClass)
        factory.loadOrCreate(default)
        return factory
    }
}
