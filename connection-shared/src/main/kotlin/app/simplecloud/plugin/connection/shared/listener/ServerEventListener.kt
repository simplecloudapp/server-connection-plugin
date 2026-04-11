package app.simplecloud.plugin.connection.shared.listener

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager

class ServerEventListener(
    private val api: CloudApi,
    private val registry: ServerRegistry,
    private val ignoreList: () -> List<String>,
) {

    private val logger = LogManager.getLogger(ServerEventListener::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var subscriptions: MutableList<Subscription> = mutableListOf()

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

    private fun register(server: RegisteredServer) {
        if (server.blueprintConfigurator == "standalone") return
        if (ignoreList().any { it.equals(server.serverBaseName, ignoreCase = true) }) {
            logger.info("Ignoring server ${server.serverId} (${server.serverBaseName}) due to ignore list")
            return
        }

        if (server.persistent) {
            logger.info("Registering server ${server.serverId} (${server.serverBaseName})...")
        } else {
            logger.info("Registering server ${server.serverId} (${server.serverBaseName}-${server.numericalId})...")
        }

        registry.register(server)
    }

    private fun unregister(server: RegisteredServer) {
        if (registry.getRegistered().containsKey(server.serverId)) {
            if (server.persistent) {
                logger.info("Unregistering server ${server.serverId} (${server.serverBaseName})...")
            } else {
                logger.info("Unregistering server ${server.serverId} (${server.serverBaseName}-${server.numericalId})...")
            }

            registry.unregister(server)
        }
    }

    private fun convertToRegisteredServer(server: Server): RegisteredServer {
        return RegisteredServer(
            serverId = server.serverId,
            numericalId = server.numericalId,
            ip = server.ip!!,
            port = server.port!!,
            serverBaseName = server.serverBase!!.name!!,
            properties = server.properties ?: emptyMap(),
            blueprintConfigurator = server.blueprint?.configurator,
            persistent = server.isFromPersistentServer
        )
    }
}