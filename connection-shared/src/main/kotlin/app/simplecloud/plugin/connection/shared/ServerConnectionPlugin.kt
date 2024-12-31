package app.simplecloud.plugin.connection.shared

import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConfigFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.TargetConnectionConfig
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfoGetter
import java.nio.file.Path

class ServerConnectionPlugin<P>(
    private val dataDirectory: Path,
    private val serverConnectionInfoGetter: ServerConnectionInfoGetter,
    private val permissionChecker: PermissionChecker<P>
) {

    val config = ConfigFactory.loadOrCreate(dataDirectory)

    fun getCommandConfigs(): List<CommandConfig> {
        return config.commands
    }

    fun getServerNameForLogin(player: P): String? {
        return getConnectionAndNameForLogin(player)?.second
    }

    fun getConnectionAndNameForLogin(player: P): Pair<ConnectionAndTargetConfig, String>? {
        return getConnectionAndName(player, config.networkJoinTargets.targetConnections)
    }

    fun getConnectionAndNameForFallback(player: P, fromServerName: String): Pair<ConnectionAndTargetConfig, String>? {
        return getConnectionAndName(player, config.fallbackConnectionsConfig.targetConnections, fromServerName)
    }

    fun getConnectionAndNameForCommand(player: P, commandConfig: CommandConfig): Pair<ConnectionAndTargetConfig, String>? {
        return getConnectionAndName(player, commandConfig.targetConnections)
    }

    private fun getConnectionAndName(player: P, targetConnections: List<TargetConnectionConfig>, fromServerName: String = ""): Pair<ConnectionAndTargetConfig, String>? {
        val possibleConnections = getPossibleServerConnections(player)
        val possibleConnectionsWithTarget = possibleConnections.map { possibleConnection ->
            val targetConfig= targetConnections
                .filter { fromServerName.isBlank() || matchesTargetConnection(it, fromServerName) }
                .firstOrNull { possibleConnection.name == it.name } ?: return null
            ConnectionAndTargetConfig(possibleConnection, targetConfig)
        }

        val connectionAndTargetConfig = possibleConnectionsWithTarget.maxByOrNull { it.targetConfig.priority }?: return null
        val bestServerToConnect = getBestServerToConnect(fromServerName, connectionAndTargetConfig.connectionConfig)?: return null
        return Pair(connectionAndTargetConfig, bestServerToConnect)
    }

    private fun matchesTargetConnection(
        targetConnectionConfig: TargetConnectionConfig,
        fromServerName: String
    ): Boolean {
        if (targetConnectionConfig.from.isEmpty())
            return true
        return targetConnectionConfig.from.any { it.matches(fromServerName) }
    }

    private fun getPossibleServerConnections(
        player: P
    ): List<ConnectionConfig> {
        val serverConnectionInfos = serverConnectionInfoGetter.get()
        val serverNames = serverConnectionInfos.map { it.name }

        return config.connections.filter { connection ->
            connection.serverNameMatcher.anyMatches(serverNames)
                    && connection.rules.all { it.isAllowed(player, permissionChecker) }
        }
    }

    private fun getBestServerToConnect(fromServerName: String, bestConnection: ConnectionConfig): String? {
        val serverConnectionInfos = serverConnectionInfoGetter.get()
        val bestServer = serverConnectionInfos
            .filter { it.name != fromServerName }
            .sortedBy { it.onlinePlayers }
            .firstOrNull { bestConnection.serverNameMatcher.matches(it.name) }
        return bestServer?.name
    }

}