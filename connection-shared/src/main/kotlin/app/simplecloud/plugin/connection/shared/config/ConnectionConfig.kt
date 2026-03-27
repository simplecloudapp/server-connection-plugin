package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.OperationType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConnectionConfig(
    val version: Char = ConfigVersion.VERSION,
    val registration: RegistrationConfig = RegistrationConfig(),
    val subdomain: SubdomainConfig = SubdomainConfig(),
    val connections: List<ConnectionEntry> = DefaultConfigs.CONNECTIONS,
    val networkJoinTargets: NetworkJoinTargetsConfig = DefaultConfigs.NETWORK_JOIN_TARGETS,
    val fallback: FallbackConfig = DefaultConfigs.FALLBACK,
)

@ConfigSerializable
data class RegistrationConfig(
    val enabled: Boolean = true,
    val serverNamePattern: String = "<group>-<numerical_id>",
    val persistentServerNamePattern: String = "<name>",
    val ignoreServerGroupsAndPersistentServers: List<String> = listOf(),
    val additionalServers: List<RegistrationServer> = listOf()
)

@ConfigSerializable
data class RegistrationServer(
    val name: String = "",
    val address: String = "",
    val port: Long = 0L
)

@ConfigSerializable
data class SubdomainConfig(
    val enabled: Boolean = true,
    val mappings: List<SubdomainMapping> = listOf()
)

@ConfigSerializable
data class SubdomainMapping(
    val subdomain: String = "",
    val targetConnection: String = ""
)

@ConfigSerializable
data class ConnectionEntry(
    val name: String = "",
    val serverNameMatcher: ServerMatcherConfiguration = ServerMatcherConfiguration(),
    val rules: List<ConnectionRule> = listOf(),
)

@ConfigSerializable
data class ConnectionRule(
    val type: RuleType = RuleType.PERMISSION,
    val name: String = "",
    val value: String = "",
    val operation: OperationType = OperationType.EQUALS,
    val negate: Boolean = false,
    val bypassPermission: String = "",
)

enum class RuleType {
    PERMISSION,
    ENV
}

@ConfigSerializable
data class NetworkJoinTargetsConfig(
    val enabled: Boolean = true,
    val targetConnections: List<TargetConnection> = listOf(),
)

@ConfigSerializable
data class TargetConnection(
    val name: String = "",
    val priority: Int = 0,
)

@ConfigSerializable
data class FallbackConfig(
    val enabled: Boolean = true,
    val targetConnections: List<FallbackTargetConnection> = listOf(),
)

@ConfigSerializable
data class FallbackTargetConnection(
    val name: String = "",
    val priority: Int = 0,
    val from: List<String> = listOf(),
)
