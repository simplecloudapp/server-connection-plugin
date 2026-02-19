package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConnectionConfig(
    val version: Char = ConfigVersion.VERSION,
    val registration: RegistrationConfig = RegistrationConfig(),
    val connections: List<ConnectionEntry> = listOf(ConnectionEntry()),
    val networkJoinTargets: NetworkJoinTargets = NetworkJoinTargets(),
    val fallback: FallbackConfig = FallbackConfig(),
)

@ConfigSerializable
data class RegistrationConfig(
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

@ConfigSerializable
data class ConnectionEntry(
    val name: String = "lobby",
    val serverNameMatcher: ServerMatcherConfiguration = ServerMatcherConfiguration(),
    val rules: List<String> = listOf()
)

@ConfigSerializable
data class NetworkJoinTargets(
    val enabled: Boolean = true,
    val targetConnections: List<TargetConnection> = listOf(
        TargetConnection(name = "lobby", priority = 0)
    )
)

@ConfigSerializable
data class FallbackConfig(
    val enabled: Boolean = true,
    val targetConnections: List<TargetConnection> = listOf(
        TargetConnection(name = "lobby", priority = 0)
    )
)

@ConfigSerializable
data class TargetConnection(
    val name: String = "",
    val priority: Int = 0,
    val from: List<String> = listOf()
)