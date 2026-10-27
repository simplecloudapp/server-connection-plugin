package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.proxy.ProxyServerNames
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import kotlinx.coroutines.Dispatchers
import java.util.Optional
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerRegistererTest {

    private val cloud = FakeCloudApi()
    private val registry = RecordingServerRegistry()
    private val startedRegisterers = mutableListOf<ServerRegisterer>()

    @AfterTest
    fun stopRegisterers() {
        startedRegisterers.forEach { registerer -> registerer.stop() }
    }

    @Test
    fun `maps the id of a registered cloud server to its proxy name`() {
        val registerer = startRegisterer(createConnectionConfig())

        registerer.register(cloudServer("lobby-id", "lobby", 1))

        assertEquals(Optional.of("lobby-1"), ProxyServerNames.proxyNameOf("lobby-id"))
    }

    @Test
    fun `does not map ignored or standalone servers`() {
        val registration = RegistrationConfig(ignoreServerGroupsAndPersistentServers = listOf("ignored"))
        val registerer = startRegisterer(createConnectionConfig(registration))

        registerer.register(cloudServer("ignored-id", "ignored", 1))
        registerer.register(cloudServer("standalone-id", "standalone", 1, blueprintConfigurator = "standalone"))

        assertEquals(Optional.empty(), ProxyServerNames.proxyNameOf("ignored-id"))
        assertEquals(Optional.empty(), ProxyServerNames.proxyNameOf("standalone-id"))
    }

    @Test
    fun `maps only the server that holds a shared proxy name`() {
        val registerer = startRegisterer(createConnectionConfig(RegistrationConfig(serverNamePattern = "<group>")))

        registerer.register(cloudServer("first-id", "lobby", 1))
        registerer.register(cloudServer("second-id", "lobby", 2))

        assertEquals(Optional.empty(), ProxyServerNames.proxyNameOf("first-id"))
        assertEquals(Optional.of("lobby"), ProxyServerNames.proxyNameOf("second-id"))
        assertEquals("second-id", registry.serversByProxyName.getValue("lobby").serverId)
    }

    @Test
    fun `treats proxy names that differ only in case as shared`() {
        val registerer = startRegisterer(createConnectionConfig(RegistrationConfig(serverNamePattern = "<group>")))

        registerer.register(cloudServer("first-id", "Lobby", 1))
        registerer.register(cloudServer("second-id", "lobby", 2))

        assertEquals(Optional.empty(), ProxyServerNames.proxyNameOf("first-id"))
        assertEquals(Optional.of("lobby"), ProxyServerNames.proxyNameOf("second-id"))
    }

    @Test
    fun `stopping a shadowed server keeps the holder of its proxy name`() {
        val registerer = startRegisterer(createConnectionConfig(RegistrationConfig(serverNamePattern = "<group>")))
        registerer.register(cloudServer("first-id", "lobby", 1))
        registerer.register(cloudServer("second-id", "lobby", 2))

        cloud.stopServer("first-id")

        assertEquals(emptyList(), registry.unregisteredServerIds)
        assertEquals("second-id", registry.serversByProxyName.getValue("lobby").serverId)
        assertEquals(Optional.of("lobby"), ProxyServerNames.proxyNameOf("second-id"))
    }

    @Test
    fun `refresh registers both servers again once their proxy names no longer collide`() {
        val config = createConnectionConfig(RegistrationConfig(serverNamePattern = "<group>"))
        val registerer = startRegisterer(config)
        registerer.register(cloudServer("first-id", "lobby", 1))
        registerer.register(cloudServer("second-id", "lobby", 2))

        config.save(ConnectionConfig(registration = RegistrationConfig(serverNamePattern = "<group>-<numerical_id>")))
        config.reload()
        registerer.refresh()

        assertEquals(Optional.of("lobby-1"), ProxyServerNames.proxyNameOf("first-id"))
        assertEquals(Optional.of("lobby-2"), ProxyServerNames.proxyNameOf("second-id"))
        assertEquals(setOf("lobby-1", "lobby-2"), registry.serversByProxyName.keys)
    }

    @Test
    fun `stops mapping server ids once stopped`() {
        val registerer = startRegisterer(createConnectionConfig())
        registerer.register(cloudServer("lobby-id", "lobby", 1))

        registerer.stop()

        assertEquals(Optional.empty(), ProxyServerNames.proxyNameOf("lobby-id"))
    }

    private fun startRegisterer(config: ConfigurationFactory<ConnectionConfig>): ServerRegisterer {
        val registerer = ServerRegisterer(cloud.api, registry, config, Dispatchers.Unconfined)
        registerer.start()
        startedRegisterers.add(registerer)
        return registerer
    }
}
