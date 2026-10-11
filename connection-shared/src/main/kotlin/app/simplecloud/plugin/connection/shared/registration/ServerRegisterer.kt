package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.utilities.RegisteredServerResolver
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap

class ServerRegisterer(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val config: ConfigurationFactory<ConnectionConfig>,
) {

    private val logger = LogManager.getLogger(ServerRegisterer::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private val registeredServers = ConcurrentHashMap<String, RegisteredServer>()

    fun start() {
        subscriptions.add(
            api.event().server().onStateChanged { event ->
                val server = event.server ?: return@onStateChanged
                if (server.serverBase?.type != GroupServerType.SERVER) return@onStateChanged
                if (event.newState == ServerState.AVAILABLE && event.oldState != ServerState.AVAILABLE) {
                    scope.launch {
                        register(server)
                    }
                }
            }
        )

        subscriptions.add(
            api.event().server().onStopped { event ->
                val server = event.server ?: return@onStopped
                scope.launch {
                    unregister(server.serverId)
                }
            }
        )
    }

    fun stop() {
        subscriptions.forEach { subscription -> subscription.close() }
        subscriptions.clear()
        scope.cancel()
    }

    fun register(server: Server) {
        val registeredServer = convertToRegisteredServer(server) ?: return
        register(registeredServer)
    }

    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") return
        val ignoredNames = config.get().registration.ignoreServerGroupsAndPersistentServers
        if (ignoredNames.any { name -> name.equals(server.serverBaseName, ignoreCase = true) }) return

        val previousRegistration = registeredServers[server.serverId]

        if (previousRegistration?.proxyName == server.proxyName) {
            logger.info("Refreshing server ${server.serverId} (${server.proxyName})...")
        } else {
            if (previousRegistration != null) {
                logger.info("Replacing stale registration ${previousRegistration.serverId} (${previousRegistration.proxyName}) with ${server.proxyName}...")
                registry.unregister(previousRegistration.proxyName, previousRegistration)
            }
            logger.info("Registering server ${server.serverId} (${server.proxyName})...")
        }

        registry.register(server.proxyName, server)
        registeredServers[server.serverId] = server
    }

    private fun unregister(serverId: String) {
        val registeredServer = registeredServers.remove(serverId) ?: return
        logger.info("Unregistering server $serverId (${registeredServer.proxyName})...")
        registry.unregister(registeredServer.proxyName, registeredServer)
    }

    private fun convertToRegisteredServer(server: Server): RegisteredServer? {
        val serverBaseName = server.serverBase?.name
        val ip = server.ip
        val port = server.port
        if (serverBaseName == null || ip == null || port == null) {
            logger.warn("Skipping server ${server.serverId} because its server base name, ip or port is missing.")
            return null
        }

        val properties = server.properties ?: emptyMap()
        val proxyName = RegisteredServerResolver.resolve(
            serverId = server.serverId,
            numericalId = server.numericalId,
            serverBaseName = serverBaseName,
            properties = properties,
            persistent = server.isFromPersistentServer,
            config = config.get().registration,
        )

        return RegisteredServer(
            serverId = server.serverId,
            numericalId = server.numericalId,
            ip = ip,
            port = port,
            serverBaseName = serverBaseName,
            proxyName = proxyName,
            properties = properties,
            blueprintConfigurator = server.blueprint?.configurator,
            persistent = server.isFromPersistentServer
        )
    }
}
