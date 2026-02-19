package app.simplecloud.plugin.connection.waterdog.connection

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import dev.waterdog.waterdogpe.network.connection.handler.IJoinHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer

class WaterdogJoinHandler(
    private val config: () -> ConnectionConfig
) : IJoinHandler {

    override fun determineServer(player: ProxiedPlayer): ServerInfo? {
        val currentConfig = config()
        if (!currentConfig.networkJoinTargets.enabled) return null

        val resolver = ConnectionResolver(currentConfig)
        val entry = resolver.resolve(
            targetConnections = currentConfig.networkJoinTargets.targetConnections,
            currentServerName = null
        ) ?: return null

        return player.proxy.servers
            .filter { entry.serverNameMatcher.matches(it.serverName) }
            .randomOrNull()
    }
}