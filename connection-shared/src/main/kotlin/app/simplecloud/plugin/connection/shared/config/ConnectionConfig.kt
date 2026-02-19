package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class ConnectionConfig(
    val version: Char = ConfigVersion.VERSION,
    val registration: RegistrationConfig = RegistrationConfig(),
)

@ConfigSerializable
data class RegistrationConfig(
    val enabled: Boolean = true,
    val serverNamePattern: String = "<group>-<numerical_id>",
    val persistentServerNamePattern: String = "<name>",
    val ignoreServerGroups: List<String> = listOf(),
    val additionalServers: List<RegistrationServer> = listOf()
)

@ConfigSerializable
data class RegistrationServer(
    val name: String = "",
    val address: String = "",
    val port: Long = 0L
)