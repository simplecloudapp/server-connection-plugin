package app.simplecloud.plugin.connection.waterdog.connection

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler
import dev.waterdog.waterdogpe.network.connection.handler.ReconnectReason
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class WaterdogReconnectHandler(
    private val config: () -> ConnectionConfig,
    private val messageConfig: () -> MessageConfig
) : IReconnectHandler {

    private val serializer = PlainTextComponentSerializer.plainText()

    override fun getFallbackServer(
        player: ProxiedPlayer?,
        oldServer: ServerInfo?,
        reason: ReconnectReason?,
        kickMessage: String?
    ): ServerInfo? {
        player ?: return null
        oldServer ?: return null

        val currentConfig = config()
        val messages = messageConfig()

        if (!currentConfig.fallback.enabled) return null

        val kickedFromServerName = oldServer.serverName
        val resolver = ConnectionResolver(currentConfig)

        val entry = resolver.resolve(
            targetConnections = currentConfig.fallback.targetConnections,
            currentServerName = kickedFromServerName
        )

        if (entry == null) {
            player.sendMessage(serializer.serialize(messages.deserialize(messages.kick.noTargetConnection)))
            return null
        }

        val servers = player.proxy.servers
            .filter { entry.serverNameMatcher.matches(it.serverName) }
            .filter { !it.serverName.equals(kickedFromServerName, ignoreCase = true) }

        val targetServer = servers.randomOrNull()

        if (targetServer == null) {
            player.sendMessage(serializer.serialize(messages.deserialize(messages.kick.noFallbackServers)))
            return null
        }

        return targetServer
    }
}