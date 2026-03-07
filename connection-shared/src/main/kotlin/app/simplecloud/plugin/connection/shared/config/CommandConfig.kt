package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    val version: Char = ConfigVersion.VERSION,
    val commands: List<CommandEntry> = DefaultConfigs.COMMANDS,
)

@ConfigSerializable
data class CommandEntry(
    val name: String = "",
    val aliases: List<String> = listOf(),
    val targetConnections: List<FallbackTargetConnection> = listOf(),
    val messages: CommandMessages = CommandMessages(),
    val permission: String = "",
)

@ConfigSerializable
data class CommandMessages(
    val alreadyConnected: String = "<color:#dc2626>You are already connected to this server!",
    val noTargetConnectionFound: String = "<color:#dc2626>Couldn't find a target server!",
)
