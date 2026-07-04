package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import app.simplecloud.plugin.connection.shared.resolver.RegisteredServerResolver
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ConcurrentHashMap

class ServerRegisterer(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val registrationConfig: () -> RegistrationConfig,
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
                        register(convertToRegisteredServer(server))
                    }
                }
            }
        )

        subscriptions.add(
            api.event().server().onStopped { event ->
                val server = event.server ?: return@onStopped
                scope.launch {
                    unregister(convertToRegisteredServer(server))
                }
            }
        )
    }

    fun stop() {
        subscriptions.forEach { it.close() }
        subscriptions.clear()
        scope.cancel()
    }

    fun register(server: Server) {
        register(convertToRegisteredServer(server))
    }

    fun getRegisteredServers(): Map<String, RegisteredServer> {
        return registeredServers.toMap()
    }

    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") return
        if (registrationConfig().ignoreServerGroupsAndPersistentServers.any { it.equals(server.serverBaseName, ignoreCase = true) }) return

        val previousRegistration = registeredServers[server.serverId]

        if (previousRegistration?.proxyName == server.proxyName) {
            logger.info("Refreshing server ${server.serverId} (${server.proxyName})...")
        } else {
            previousRegistration?.let {
                logger.info("Replacing stale registration ${it.serverId} (${it.proxyName}) with ${server.proxyName}...")
                registry.unregister(it.proxyName, it)
            }
            logger.info("Registering server ${server.serverId} (${server.proxyName})...")
        }

        registry.register(server.proxyName, server)
        registeredServers[server.serverId] = server
    }

    private fun unregister(server: RegisteredServer) {
        val registeredServer = registeredServers.remove(server.serverId) ?: return
        logger.info("Unregistering server ${server.serverId} (${registeredServer.proxyName})...")
        registry.unregister(registeredServer.proxyName, registeredServer)
    }

    private fun convertToRegisteredServer(server: Server): RegisteredServer {
        val properties = server.properties ?: emptyMap()
        val serverBaseName = server.serverBase!!.name!!
        val proxyName = RegisteredServerResolver.resolve(
            serverId = server.serverId,
            numericalId = server.numericalId,
            serverBaseName = serverBaseName,
            properties = properties,
            persistent = server.isFromPersistentServer,
            config = registrationConfig(),
        )

        return RegisteredServer(
            serverId = server.serverId,
            numericalId = server.numericalId,
            ip = server.ip!!,
            port = server.port!!,
            serverBaseName = serverBaseName,
            proxyName = proxyName,
            properties = properties,
            blueprintConfigurator = server.blueprint?.configurator,
            persistent = server.isFromPersistentServer
        )
    }
}
