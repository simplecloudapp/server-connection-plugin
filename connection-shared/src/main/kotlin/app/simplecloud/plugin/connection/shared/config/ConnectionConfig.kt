package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ConnectionConfig(
    val version: Char = ConfigVersion.VERSION,

    @Comment("""
───────────────────────────────────────────────────────────────────────────────
Server Registration
This keeps your proxy's registered child servers in sync with SimpleCloud's 
server registry.

Read more @ https://docs.simplecloud.app/manual/plugins/server-connection
───────────────────────────────────────────────────────────────────────────────

Configure how servers are automatically registered with the proxy""")
    val registration: RegistrationConfig = RegistrationConfig(),
)

@ConfigSerializable
data class RegistrationConfig(
    @Comment("Attention: If this is set to false, no child servers will be registered.")
    val enabled: Boolean = true,
    val serverNamePattern: String = "<group>-<numerical_id>",
    val persistentServerNamePattern: String = "<name>",
    val ignoreServerGroups: List<String> = listOf(),
    val additionalServers: List<RegistrationServer> = listOf()
)

@ConfigSerializable
data class RegistrationServer(
    val name: String = "",
    val address: String = "",
    val port: Long = 0L
)