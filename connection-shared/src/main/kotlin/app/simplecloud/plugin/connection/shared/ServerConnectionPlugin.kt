package app.simplecloud.plugin.connection.shared

import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.ConfigFactory
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfoGetter
import java.nio.file.Path

class ServerConnectionPlugin<P>(
    private val dataDirectory: Path,
    private val serverConnectionInfoGetter: ServerConnectionInfoGetter,
    private val permissionChecker: PermissionChecker<P>
) {

    private val config = ConfigFactory.loadOrCreate(dataDirectory)

    fun getCommandConfigs(): List<CommandConfig> {
        return config.commands
    }

    fun getServerNameToConnect(player: P, commandConfig: CommandConfig? = null): String? {
        return getConnectionAndNameToConnect(player, commandConfig)?.second
    }

    fun getConnectionAndNameToConnect(player: P, commandConfig: CommandConfig? = null): Pair<ConnectionConfig, String>? {
        val serverConnectionInfos = serverConnectionInfoGetter.get()
        val serverNames = serverConnectionInfos.map { it.name }

        val possibleConnections = getPossibleServerConnections(player, serverNames, commandConfig == null)
        val bestConnection = possibleConnections
            .filter { commandConfig == null || it.commandRef == commandConfig.name }
            .maxByOrNull { it.priority } ?: return null

        val bestServerToConnect = getBestServerToConnect(bestConnection)?: return null
        return Pair(bestConnection, bestServerToConnect)
    }

    private fun getPossibleServerConnections(
        player: P,
        serverNames: List<String>,
        login: Boolean
    ): List<ConnectionConfig> {
        return config.connections.filter { connection ->
            connection.serverNameMatcher.anyMatches(serverNames)
                    && connection.rules.all { it.isAllowed(player, permissionChecker) }
                    && (!login || connection.tryOnLogin)
        }
    }

    private fun getBestServerToConnect(bestConnection: ConnectionConfig): String? {
        val serverConnectionInfos = serverConnectionInfoGetter.get()
        val bestServer = serverConnectionInfos
            .shuffled()
            .sortedBy { it.onlinePlayers }
            .firstOrNull { bestConnection.serverNameMatcher.matches(it.name) }

        return bestServer?.name
    }

}