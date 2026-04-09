package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.OperationType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ConnectionConfig(
    val version: Char = ConfigVersion.VERSION,
    @Comment("Server Registration\nKeeps your proxy's registered child servers in sync with SimpleCloud.")
    val registration: RegistrationConfig = RegistrationConfig(),
    @Comment("Subdomain Routing\nRoutes players to a connection based on the hostname they connected with.")
    val address: AddressConfig = AddressConfig(),
    @Comment("Connections\nNamed groups that reference a set of servers by a name matcher.\nThese are referenced by network-join-targets, fallback and commands.yml.")
    val connections: List<ConnectionEntry> = DefaultConfigs.CONNECTIONS,
    @Comment("Network Join Targets\nDefines where players are sent when they first join the network.")
    val networkJoinTargets: NetworkJoinTargetsConfig = DefaultConfigs.NETWORK_JOIN_TARGETS,
    @Comment("Fallback\nDefines where players are sent when their current server becomes unavailable.")
    val fallback: FallbackConfig = DefaultConfigs.FALLBACK,
)

@ConfigSerializable
data class RegistrationConfig(
    @Comment("If false, no SimpleCloud servers will be registered on the proxy.")
    val enabled: Boolean = true,
    @Comment("Pattern used to name dynamically started servers on the proxy.\nAvailable placeholders: <group>, <numerical_id>, <id>, <name>")
    val serverNamePattern: String = "<group>-<numerical_id>",
    @Comment("Pattern used for persistent servers.")
    val persistentServerNamePattern: String = "<name>",
    @Comment("Server groups and persistent servers that should not be registered on the proxy.")
    val ignoreServerGroupsAndPersistentServers: List<String> = listOf(),
    @Comment("Non SimpleCloud servers to register manually.")
    val additionalServers: List<RegistrationServer> = listOf()
)

@ConfigSerializable
data class RegistrationServer(
    val name: String = "",
    val address: String = "",
    val port: Long = 0L
)

@ConfigSerializable
data class AddressConfig(
    val routes: List<SubdomainRoute> = listOf()
)

@ConfigSerializable
data class SubdomainRoute(
    val subdomain: String = "",
    val targetConnection: String = ""
)

@ConfigSerializable
data class ConnectionEntry(
    val name: String = "",
    @Comment("Operation to match server names.\nAvailable: STARTS_WITH, ENDS_WITH, CONTAINS, EQUALS, REGEX, PATTERN, GREATER_THAN")
    val serverNameMatcher: ServerMatcherConfiguration = ServerMatcherConfiguration(),
    @Comment("Rules that must pass before a player can use this connection.\nSee \"Connection Rules\" section below for details.")
    val rules: List<ConnectionRule> = listOf(),
)

@ConfigSerializable
data class ConnectionRule(
    val type: RuleType = RuleType.PERMISSION,
    val name: String = "",
    val value: String = "",
    val operation: OperationType = OperationType.EQUALS,
    @Comment("If true, inverts the match result.")
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
    @Comment("Optional: only use this fallback if the player is coming from one of these servers.")
    val from: List<String> = listOf(),
)
