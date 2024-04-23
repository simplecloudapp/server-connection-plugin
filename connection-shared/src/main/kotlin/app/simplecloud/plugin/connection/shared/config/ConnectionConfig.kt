package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConnectionConfig(
    val name: String = "",
    val priority: Int = 0,
    val commandRef: String = "",
    val tryOnLogin: Boolean = false,
    val serverNameMatcher: MatcherConfig = MatcherConfig(),
    val rules: List<RulesConfig> = emptyList()
)
