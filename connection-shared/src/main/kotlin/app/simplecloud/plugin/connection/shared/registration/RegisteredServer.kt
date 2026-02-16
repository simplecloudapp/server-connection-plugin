package app.simplecloud.plugin.connection.shared.registration

/**
 * Represents a registered server.
 */
data class RegisteredServer(
    val serverId: String,
    val numericalId: Int,
    val ip: String,
    val port: Int,
    val serverBaseName: String,
    val properties: Map<String, Any>,
    val blueprintConfigurator: String?,
    val persistent: Boolean,
)