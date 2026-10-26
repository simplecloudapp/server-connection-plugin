package app.simplecloud.plugin.connection.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.base.ServerBase
import app.simplecloud.api.event.EventApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.api.event.server.ServerEventApi
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerApi
import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import app.simplecloud.plugin.connection.shared.registration.ServerRegistry
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConnectionPluginTest {

    private val scheduler = TestCoroutineScheduler()
    private val cloud = FakeCloud()
    private val registry = RecordingServerRegistry()
    private val plugin = ConnectionPlugin(
        dir = createTempDirectory().toFile(),
        registry = registry,
        api = cloud.api,
        dispatcher = StandardTestDispatcher(scheduler),
    )

    @AfterTest
    fun shutdownPlugin() {
        plugin.shutdown()
    }

    @Test
    fun `subscribes to server events even if the first load fails`() {
        plugin.start()
        scheduler.runCurrent()

        assertEquals(1, cloud.loadAttempts)
        assertEquals(listOf("onStateChanged", "onStopped"), cloud.subscribedEvents)
    }

    @Test
    fun `retries the load and registers the servers once the controller answers`() {
        cloud.availableServers = listOf(cloudServer("lobby-id", "lobby", 1))

        plugin.start()
        scheduler.runCurrent()
        advanceTimeBy(5.seconds)

        assertEquals(2, cloud.loadAttempts)
        assertEquals(emptyList(), registry.registeredProxyNames)

        cloud.controllerReachable = true
        advanceTimeBy(10.seconds)

        assertEquals(3, cloud.loadAttempts)
        assertEquals(listOf("lobby-1"), registry.registeredProxyNames)
    }

    @Test
    fun `doubles the retry delay up to 60 seconds`() {
        plugin.start()
        scheduler.runCurrent()

        val expectedRetryDelays = listOf(5.seconds, 10.seconds, 20.seconds, 40.seconds, 60.seconds, 60.seconds)
        expectedRetryDelays.forEach { retryDelay ->
            val attemptsBeforeRetry = cloud.loadAttempts
            advanceTimeBy(retryDelay - 1.seconds)
            assertEquals(attemptsBeforeRetry, cloud.loadAttempts, "retried before $retryDelay")

            advanceTimeBy(1.seconds)
            assertEquals(attemptsBeforeRetry + 1, cloud.loadAttempts, "did not retry after $retryDelay")
        }
    }

    @Test
    fun `stops retrying on shutdown`() {
        plugin.start()
        scheduler.runCurrent()

        plugin.shutdown()
        advanceTimeBy(10.minutes)

        assertEquals(1, cloud.loadAttempts)
    }

    private fun advanceTimeBy(duration: Duration) {
        scheduler.advanceTimeBy(duration)
        scheduler.runCurrent()
    }
}

/**
 * Implements the interface [T] by answering every call with [answer], which gets the called method's name.
 */
private inline fun <reified T> fake(crossinline answer: (methodName: String) -> Any?): T {
    val type = T::class.java
    val handler = InvocationHandler { _, method, _ -> answer(method.name) }
    val proxy = Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler)
    return proxy as T
}

/**
 * A [CloudApi] whose controller is unreachable until the test makes it reachable.
 */
private class FakeCloud {

    var controllerReachable = false
    var availableServers = emptyList<Server>()
    var loadAttempts = 0
    val subscribedEvents = CopyOnWriteArrayList<String>()

    private val subscription = fake<Subscription> { null }

    private val serverEventApi = fake<ServerEventApi> { methodName ->
        subscribedEvents.add(methodName)
        subscription
    }

    private val eventApi = fake<EventApi> { methodName ->
        if (methodName == "server") serverEventApi else null
    }

    private val serverApi = fake<ServerApi> { methodName ->
        if (methodName == "getAllServers") loadServers() else null
    }

    val api = fake<CloudApi> { methodName ->
        when (methodName) {
            "event" -> eventApi
            "server" -> serverApi
            else -> null
        }
    }

    private fun loadServers(): CompletableFuture<List<Server>> {
        loadAttempts++
        if (!controllerReachable) {
            return CompletableFuture.failedFuture(IllegalStateException("controller unreachable"))
        }
        return CompletableFuture.completedFuture(availableServers)
    }
}

private class RecordingServerRegistry : ServerRegistry {

    val registeredProxyNames = CopyOnWriteArrayList<String>()

    override fun register(proxyName: String, server: RegisteredServer) {
        registeredProxyNames.add(proxyName)
    }

    override fun unregister(proxyName: String, server: RegisteredServer) {
        registeredProxyNames.remove(proxyName)
    }
}

private fun cloudServer(serverId: String, groupName: String, numericalId: Int): Server {
    val serverBase = fake<ServerBase> { methodName ->
        when (methodName) {
            "getName" -> groupName
            "getType" -> GroupServerType.SERVER
            else -> null
        }
    }

    return fake { methodName ->
        when (methodName) {
            "getServerId" -> serverId
            "getNumericalId" -> numericalId
            "getIp" -> "127.0.0.1"
            "getPort" -> 25565
            "getServerBase" -> serverBase
            "getProperties" -> emptyMap<String, Any>()
            "isFromPersistentServer" -> false
            else -> null
        }
    }
}
