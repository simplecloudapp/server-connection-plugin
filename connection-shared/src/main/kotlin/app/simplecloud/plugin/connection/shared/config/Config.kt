package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Config(
    val connections: List<ConnectionConfig> = emptyList(),
    val commands: List<CommandConfig> = emptyList(),
)