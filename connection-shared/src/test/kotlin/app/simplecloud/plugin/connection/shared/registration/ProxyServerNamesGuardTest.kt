package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.CloudApi
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProxyServerNamesGuardTest {

    @Test
    fun `keeps registering servers when cloud-api has no ProxyServerNames`() {
        val cloud = FakeCloudApi()
        val registry = RecordingServerRegistry()
        val config = createConnectionConfig()
        val classLoader = ClassLoaderWithoutProxyServerNames(javaClass.classLoader)
        val registererClass = classLoader.loadClass(ServerRegisterer::class.java.name)
        val constructor = registererClass.getConstructor(
            CloudApi::class.java,
            ServerRegistry::class.java,
            ConfigurationFactory::class.java,
            CoroutineDispatcher::class.java,
        )
        val registerer = constructor.newInstance(cloud.api, registry, config, Dispatchers.Unconfined)
        val server = cloudServer("lobby-id", "lobby", 1)

        // Fails if a field or method signature of ServerRegisterer mentions ProxyServerNames.
        registererClass.declaredFields
        registererClass.declaredMethods
        registererClass.getMethod("start").invoke(registerer)
        registererClass.getMethod("register", Server::class.java).invoke(registerer, server)
        registererClass.getMethod("stop").invoke(registerer)

        assertTrue(classLoader.proxyServerNamesRequested)
        assertEquals(setOf("lobby-1"), registry.serversByProxyName.keys)
    }
}

/**
 * Hides ProxyServerNames like a proxy on cloud-api before 0.1.0-platform.61 does.
 *
 * It defines [ServerRegisterer] and [ProxyServerNamesBridge] itself, so that they resolve ProxyServerNames
 * through this loader. Every other class comes from the parent, so the test's fakes still fit.
 */
private class ClassLoaderWithoutProxyServerNames(parent: ClassLoader) : ClassLoader(parent) {

    private val hiddenClassName = "app.simplecloud.api.proxy.ProxyServerNames"
    private val ownClassNames = listOf(ServerRegisterer::class.java.name, ProxyServerNamesBridge::class.java.name)

    @Volatile
    var proxyServerNamesRequested = false
        private set

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name == hiddenClassName || name.startsWith("$hiddenClassName$")) {
            proxyServerNamesRequested = true
            throw ClassNotFoundException(name)
        }
        if (!isOwnClass(name)) return super.loadClass(name, resolve)

        synchronized(getClassLoadingLock(name)) {
            return findLoadedClass(name) ?: defineOwnClass(name)
        }
    }

    private fun isOwnClass(name: String): Boolean {
        return ownClassNames.any { ownClassName -> name == ownClassName || name.startsWith("$ownClassName$") }
    }

    private fun defineOwnClass(name: String): Class<*> {
        val resourceName = name.replace('.', '/') + ".class"
        val resource = checkNotNull(parent.getResourceAsStream(resourceName)) { "Missing $resourceName" }
        val bytes = resource.use { stream -> stream.readBytes() }
        return defineClass(name, bytes, 0, bytes.size)
    }
}
