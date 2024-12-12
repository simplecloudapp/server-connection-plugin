package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TargetConnectionConfig (
    val name: String = "",
    val priority: Int = 0,
    val from: List<MatcherConfig> = emptyList()
)
