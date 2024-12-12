package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TargetsConfig(
    val enabled: Boolean = false,
    val noTargetConnectionFoundMessage: String = "",
    val targetConnections: List<TargetConnectionConfig> = emptyList()
)