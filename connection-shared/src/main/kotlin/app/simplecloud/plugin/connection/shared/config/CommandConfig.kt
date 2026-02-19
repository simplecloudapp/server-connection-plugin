package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultCommands
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    val version: Char = ConfigVersion.VERSION,
    val commands: List<CommandEntry> = DefaultCommands.DEFAULT
)

@ConfigSerializable
data class CommandEntry(
    val name: String,
    val aliases: List<String> = emptyList(),
    val targetConnections: List<TargetConnection> = emptyList(),
    val messages: CommandMessages = CommandMessages(),
    val permission: String = ""
)

@ConfigSerializable
data class TargetConnection(
    val name: String,
    val priority: Int = 0,
    val from: List<String> = emptyList()
)

@ConfigSerializable
data class CommandMessages(
    val alreadyConnected: String = "<color:#dc2626>You are already connected to this lobby!",
    val noTargetConnectionFound: String = "<color:#dc2626>Couldn't find a target server!"
)