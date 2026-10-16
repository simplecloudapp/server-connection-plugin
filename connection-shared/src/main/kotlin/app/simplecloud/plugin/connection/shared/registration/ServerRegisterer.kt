package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
//import app.simplecloud.api.internal.integration.player.ServerNameRegistry
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.utilities.RegisteredServerResolver
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

class ServerRegisterer(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val config: ConfigurationFactory<ConnectionConfig>,
) {

    private val logger = LoggerFactory.getLogger(ServerRegisterer::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private val registeredServers = ConcurrentHashMap<String, RegisteredServer>()
    private val serverNames = Function<String, Optional<String>> { id -> Optional.ofNullable(registeredServers[id]?.proxyName) }

    fun start() {
        //ServerNameRegistry.register(serverNames)
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
        //ServerNameRegistry.unregister(serverNames)
        subscriptions.forEach { subscription -> subscription.close() }
        subscriptions.clear()
        scope.cancel()
    }

    fun register(server: Server) {
        val registeredServer = convertToRegisteredServer(server) ?: return
        register(registeredServer)
    }

    fun refresh() {
        registeredServers.values.forEach { server ->
            register(server.copy(proxyName = resolveProxyName(server)))
        }
    }

    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") return

        val ignoredNames = config.get().registration.ignoreServerGroupsAndPersistentServers
        if (ignoredNames.any { name -> name.equals(server.serverBaseName, ignoreCase = true) }) {
            unregister(server.serverId)
            return
        }

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

        val registeredServer = RegisteredServer(
            serverId = server.serverId,
            numericalId = server.numericalId,
            ip = ip,
            port = port,
            serverBaseName = serverBaseName,
            proxyName = "",
            properties = server.properties ?: emptyMap(),
            blueprintConfigurator = server.blueprint?.configurator,
            persistent = server.isFromPersistentServer
        )
        return registeredServer.copy(proxyName = resolveProxyName(registeredServer))
    }

    private fun resolveProxyName(server: RegisteredServer): String {
        return RegisteredServerResolver.resolve(
            serverId = server.serverId,
            numericalId = server.numericalId,
            serverBaseName = server.serverBaseName,
            properties = server.properties,
            persistent = server.persistent,
            config = config.get().registration,
        )
    }
}
