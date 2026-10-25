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
import org.slf4j.LoggerFactory
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class ServerRegisterer(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val config: ConfigurationFactory<ConnectionConfig>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val logger = LoggerFactory.getLogger(ServerRegisterer::class.java)
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val subscriptions: MutableList<Subscription> = mutableListOf()
    private val registeredServers = ConcurrentHashMap<String, RegisteredServer>()

    private val proxyNameHolders = ConcurrentHashMap<String, String>()

    @Volatile
    private var serverIdMapping: AutoCloseable? = null

    fun start() {
        publishServerIdMapping()

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
        withdrawServerIdMapping()
    }

    fun register(server: Server) {
        val registeredServer = convertToRegisteredServer(server) ?: return
        register(registeredServer)
    }

    @Synchronized
    fun refresh() {
        val servers = registeredServers.values.toList()
        servers.forEach { server ->
            register(server.copy(proxyName = resolveProxyName(server)))
        }
    }

    @Synchronized
    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") return

        val ignoredNames = config.get().registration.ignoreServerGroupsAndPersistentServers
        if (ignoredNames.any { name -> name.equals(server.serverBaseName, ignoreCase = true) }) {
            unregister(server.serverId)
            return
        }

        val ownPreviousRegistration = registeredServers[server.serverId]

        if (ownPreviousRegistration?.proxyName == server.proxyName) {
            logger.info("Refreshing server ${server.serverId} (${server.proxyName})...")
        } else {
            if (ownPreviousRegistration != null) {
                logger.info("Replacing stale registration ${ownPreviousRegistration.serverId} (${ownPreviousRegistration.proxyName}) with ${server.proxyName}...")
                unregisterFromProxy(ownPreviousRegistration)
            }
            logger.info("Registering server ${server.serverId} (${server.proxyName})...")
        }

        val proxyNameKey = proxyNameKey(server)
        val currentHolderId = proxyNameHolders[proxyNameKey]
        val takesOverName = currentHolderId != null && currentHolderId != server.serverId
        if (takesOverName) {
            logger.warn("Server ${server.serverId} takes over the name ${server.proxyName} from server $currentHolderId")
        }

        // Claimed before the proxy switches, so the server ID mapping never answers for the replaced server.
        proxyNameHolders[proxyNameKey] = server.serverId
        registry.register(server.proxyName, server)
        registeredServers[server.serverId] = server
    }

    @Synchronized
    private fun unregister(serverId: String) {
        val registeredServer = registeredServers.remove(serverId) ?: return
        logger.info("Unregistering server $serverId (${registeredServer.proxyName})...")
        unregisterFromProxy(registeredServer)
    }

    // A server whose name another server took over is no longer on the proxy, so it is only forgotten.
    private fun unregisterFromProxy(server: RegisteredServer) {
        if (!holdsProxyName(server)) return
        proxyNameHolders.remove(proxyNameKey(server))
        registry.unregister(server.proxyName, server)
    }

    private fun holdsProxyName(server: RegisteredServer): Boolean {
        val holderId = proxyNameHolders[proxyNameKey(server)]
        return holderId == server.serverId
    }

    // Proxies look server names up case-insensitively.
    private fun proxyNameKey(server: RegisteredServer): String {
        return server.proxyName.lowercase()
    }

    // cloud-api calls this on the NATS dispatcher thread, so it only reads the concurrent maps.
    private fun findRegisteredProxyName(serverId: String): Optional<String> {
        val registeredServer = registeredServers[serverId] ?: return Optional.empty()
        if (!holdsProxyName(registeredServer)) return Optional.empty()
        return Optional.of(registeredServer.proxyName)
    }

    private fun publishServerIdMapping() {
        try {
            serverIdMapping = ProxyServerNamesBridge.register(::findRegisteredProxyName)
        } catch (_: NoClassDefFoundError) {
            logger.warn("The installed SimpleCloud API does not support connecting players by server ID yet. Update it to enable this.")
        }
    }

    private fun withdrawServerIdMapping() {
        serverIdMapping?.close()
        serverIdMapping = null
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
