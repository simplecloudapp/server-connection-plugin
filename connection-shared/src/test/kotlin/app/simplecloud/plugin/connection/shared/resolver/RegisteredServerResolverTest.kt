package app.simplecloud.plugin.connection.shared.resolver

import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class RegisteredServerResolverTest {

    @Test
    fun `normalizes dynamic port property names for MiniMessage placeholders`() {
        val resolvedName = RegisteredServerResolver.resolve(
            serverId = "server-id",
            numericalId = 1,
            serverBaseName = "lobby",
            properties = mapOf("dynamic_port:simple_voice_chat" to 24_454),
            persistent = false,
            config = RegistrationConfig(
                serverNamePattern = "voice-<dynamic_port_simple_voice_chat>",
            ),
        )

        assertEquals("voice-24454", resolvedName)
    }
}
