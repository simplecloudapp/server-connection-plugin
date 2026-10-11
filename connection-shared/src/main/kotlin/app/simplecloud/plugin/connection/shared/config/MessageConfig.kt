package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.config.AbstractMessageConfig
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    override val version: Int = ConfigVersion.VERSION,
    override val variables: Map<String, String> = DefaultConfigs.VARIABLES,
    val kick: KickMessages = KickMessages(),
    val command: ConnectionCommandMessages = ConnectionCommandMessages()
) : VersionedConfig, AbstractMessageConfig()

@ConfigSerializable
data class KickMessages(
    val noFallbackServers: String = "<color:#dc2626>There is no fallback server available.",
    val noTargetConnection: String = "<color:#dc2626>You have been disconnected from the network<br>because there are no fallback servers available.",
)

@ConfigSerializable
data class ConnectionCommandMessages(
    val commandUsage: String = "<prefix> <color:#ffffff>Usage: /connection reload",
    val configReloading: String = "<prefix> <color:#ffffff>Reloading Connection configurations...",
    val configReloadedSuccess: String = "<prefix> <color:#22c55e>Successfully reloaded all Connection configurations.",
    val configReloadedFailed: String = "<prefix> <color:#dc2626>Failed to reload Connection configurations."
)