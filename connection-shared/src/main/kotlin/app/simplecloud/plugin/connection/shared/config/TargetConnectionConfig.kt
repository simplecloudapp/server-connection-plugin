package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TargetConnectionConfig (
    val name: String = "",
    val priority: Int = 0,
    val from: List<ServerMatcherConfiguration> = emptyList()
)
