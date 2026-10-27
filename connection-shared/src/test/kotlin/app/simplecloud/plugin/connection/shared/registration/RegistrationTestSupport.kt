package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.CloudApi
import app.simplecloud.api.base.ServerBase
import app.simplecloud.api.blueprint.Blueprint
import app.simplecloud.api.event.EventApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.event.server.ServerEventApi
import app.simplecloud.api.event.server.ServerStoppedEvent
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.io.path.createTempDirectory

/**
 * Implements the interface [T] by answering every call with [answer], which gets the called method's name.
 */
inline fun <reified T> fake(crossinline answer: (methodName: String, arguments: Array<Any?>) -> Any?): T {
    val type = T::class.java
    val handler = InvocationHandler { _, method, arguments -> answer(method.name, arguments ?: emptyArray()) }
    val proxy = Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler)
    return proxy as T
}

/**
 * A [CloudApi] whose server events are fired by the test.
 */
class FakeCloudApi {

    private val stoppedHandlers = CopyOnWriteArrayList<Consumer<ServerStoppedEvent>>()
    private val subscription = fake<Subscription> { _, _ -> null }

    private val serverEventApi = fake<ServerEventApi> { methodName, arguments ->
        when (methodName) {
            "onStopped" -> subscribeToStopped(arguments)
            "onStateChanged" -> subscription
            else -> null
        }
    }

    private val eventApi = fake<EventApi> { methodName, _ ->
        if (methodName == "server") serverEventApi else null
    }

    val api = fake<CloudApi> { methodName, _ ->
        if (methodName == "event") eventApi else null
    }

    fun stopServer(serverId: String) {
        val server = fake<Server> { methodName, _ ->
            if (methodName == "getServerId") serverId else null
        }
        val event = fake<ServerStoppedEvent> { methodName, _ ->
            if (methodName == "getServer") server else null
        }
        stoppedHandlers.forEach { handler -> handler.accept(event) }
    }

    private fun subscribeToStopped(arguments: Array<Any?>): Subscription {
        @Suppress("UNCHECKED_CAST")
        val handler = arguments[0] as Consumer<ServerStoppedEvent>
        stoppedHandlers.add(handler)
        return subscription
    }
}

/**
 * Keeps the servers by proxy name, like a proxy does, and records which servers were unregistered.
 */
class RecordingServerRegistry : ServerRegistry {

    val serversByProxyName = ConcurrentHashMap<String, RegisteredServer>()
    val unregisteredServerIds = CopyOnWriteArrayList<String>()

    override fun register(proxyName: String, server: RegisteredServer) {
        serversByProxyName[proxyName] = server
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        serversByProxyName.remove(proxyName)
        unregisteredServerIds.add(server.serverId)
    }
}

fun cloudServer(
    serverId: String,
    groupName: String,
    numericalId: Int,
    blueprintConfigurator: String? = null,
): Server {
    val serverBase = fake<ServerBase> { methodName, _ ->
        when (methodName) {
            "getName" -> groupName
            "getType" -> GroupServerType.SERVER
            else -> null
        }
    }
    val blueprint = fake<Blueprint> { methodName, _ ->
        if (methodName == "getConfigurator") blueprintConfigurator else null
    }

    return fake { methodName, _ ->
        when (methodName) {
            "getServerId" -> serverId
            "getNumericalId" -> numericalId
            "getIp" -> "127.0.0.1"
            "getPort" -> 25565 + numericalId
            "getServerBase" -> serverBase
            "getProperties" -> emptyMap<String, Any>()
            "getBlueprint" -> blueprint
            "isFromPersistentServer" -> false
            else -> null
        }
    }
}

fun createConnectionConfig(registration: RegistrationConfig = RegistrationConfig()): ConfigurationFactory<ConnectionConfig> {
    val configFile = createTempDirectory().resolve("config.yml").toFile()
    val config = ConfigurationFactory(configFile, ConnectionConfig::class.java)
    config.loadOrCreate(ConnectionConfig(registration = registration))
    return config
}
