package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.config.VersionedConfig
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandConfig(
    override val version: Int = ConfigVersion.VERSION,
    val commands: List<CommandEntry> = DefaultConfigs.COMMANDS,
) : VersionedConfig

@ConfigSerializable
data class CommandEntry(
    val name: String = "",
    val aliases: List<String> = listOf(),
    val permission: String = "",
    val messages: CommandMessages = CommandMessages(),
    val targetConnections: List<FallbackTargetConnection> = listOf(),
)

@ConfigSerializable
data class CommandMessages(
    val alreadyConnected: String = "<color:#dc2626>You are already connected to this server!",
    val noTargetConnectionFound: String = "<color:#dc2626>Couldn't find a target server!",
)
