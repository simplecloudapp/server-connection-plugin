package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    val name: String = "",
    val aliases: List<String> = emptyList(),
    val targetConnections: List<TargetConnectionConfig> = emptyList(),
    val alreadyConnectedMessage: String = "<color:#dc2626>You are already connected to this group!",
    val noTargetConnectionFound: String = "<color:#dc2626>Couldn't find a target connection!",
    val permission: String = "",
)
