package app.simplecloud.plugin.connection.shared

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.TargetConnectionConfig

data class ConnectionAndTargetConfig(
    val connectionConfig: ConnectionConfig,
    val targetConfig: TargetConnectionConfig,
)
