package app.simplecloud.plugin.connection.shared.config

data class TargetsConfig(
    val enabled: Boolean = false,
    val noTargetConnectionFoundMessage: String = "",
    val targetConnections: List<TargetConnectionConfig> = emptyList()
)