package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConnectionConfig(
    val name: String = "",
    val serverNameMatcher: ServerMatcherConfiguration = ServerMatcherConfiguration(),
    val rules: List<RulesConfig> = emptyList()
)
