package app.simplecloud.plugin.connection.shared.config.migration

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegacyConfigMigratorTest {

    @Test
    fun `migrates legacy version one config into split current configs`() {
        val dataDirectory = createTempDirectory().toFile()
        val configFile = dataDirectory.resolve("config.yml")
        configFile.writeText(
            """
            version: "1"
            connections:
            - name: lobby
              server-name-matcher:
                operation: STARTS_WITH
                value: lobby
              rules:
              - type: PERMISSION
                operation: EQUALS
                name: simplecloud.connection.lobby
                value: "true"
                negate: false
                bypass-permission: simplecloud.connection.bypass
            network-join-targets:
              enabled: true
              no-target-connection-found-message: network join missing
              target-connections:
              - name: lobby
                priority: 5
                from:
                - operation: STARTS_WITH
                  value: ignored
            fallback-connections-config:
              enabled: true
              no-target-connection-found-message: fallback missing
              target-connections:
              - name: lobby
                priority: 10
                from:
                - operation: STARTS_WITH
                  value: game
            commands:
            - name: lobby
              aliases:
              - l
              - hub
              permission: simplecloud.command.lobby
              already-connected-message: already there
              no-target-connection-found: no target
              target-connections:
              - name: lobby
                priority: 3
                from:
                - operation: STARTS_WITH
                  value: ignored-by-legacy
            """.trimIndent()
        )

        val connectionConfig = ConfigurationFactory(
            configFile,
            ConnectionConfig::class.java,
            LegacyConfigMigrator.create(dataDirectory)
        ).loadOrCreate(ConnectionConfig())
        val commandConfig = ConfigurationFactory(
            dataDirectory.resolve("commands.yml"),
            CommandConfig::class.java
        ).loadOrCreate(CommandConfig())
        val messageConfig = ConfigurationFactory(
            dataDirectory.resolve("messages.yml"),
            MessageConfig::class.java
        ).loadOrCreate(MessageConfig())

        assertEquals(2, connectionConfig.version)
        assertEquals(2, commandConfig.version)
        assertEquals(2, messageConfig.version)

        assertEquals("lobby", connectionConfig.connections.first().name)
        assertEquals("simplecloud.connection.lobby", connectionConfig.connections.first().rules.first().name)
        assertEquals(listOf("lobby"), connectionConfig.networkJoinTargets.targetConnections.map { it.name })
        assertEquals(5, connectionConfig.networkJoinTargets.targetConnections.first().priority)

        val syntheticFromConnection = connectionConfig.connections.firstOrNull {
            it.serverNameMatcher.value == "game"
        }
        assertNotNull(syntheticFromConnection)
        assertEquals(listOf(syntheticFromConnection.name), connectionConfig.fallback.targetConnections.first().from)

        val command = commandConfig.commands.first()
        assertEquals("lobby", command.name)
        assertEquals(listOf("l", "hub"), command.aliases)
        assertEquals("simplecloud.command.lobby", command.permission)
        assertEquals("already there", command.messages.alreadyConnected)
        assertEquals("no target", command.messages.noTargetConnectionFound)
        assertTrue(command.targetConnections.first().from.isEmpty())

        assertEquals("network join missing", messageConfig.kick.noTargetConnection)
        assertEquals("fallback missing", messageConfig.kick.noFallbackServers)
    }

}
