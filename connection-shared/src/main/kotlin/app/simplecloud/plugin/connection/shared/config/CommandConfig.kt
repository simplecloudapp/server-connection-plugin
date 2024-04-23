package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    val name: String = "",
    val permission: String = "",
    val alreadyConnectedMessage: String = "<red>You are already connected to this group!",
    val aliases: List<String> = emptyList(),
)
