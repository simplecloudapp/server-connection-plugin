package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Config(
    val connections: List<ConnectionConfig> = emptyList(),
    val networkJoinTargets: TargetsConfig = TargetsConfig(
        noTargetConnectionFoundMessage = "<red>Couldn't connect you to the network because no target servers are available."
    ),
    val fallbackConnectionsConfig: TargetsConfig = TargetsConfig(
        noTargetConnectionFoundMessage = "<red>You have been disconnected from the network since you have been kicked and no fallback server are available."
    ),
    val commands: List<CommandConfig> = emptyList(),
)