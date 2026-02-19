package app.simplecloud.plugin.connection.waterdog.connection

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.connection.ConnectionResolver
import dev.waterdog.waterdogpe.network.connection.handler.IReconnectHandler
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class WaterdogReconnectHandler(
    private val config: () -> ConnectionConfig,
    private val messageConfig: () -> MessageConfig
) : IReconnectHandler {

    private val plainSerializer = PlainTextComponentSerializer.plainText()

    override fun getFallbackServer(
        player: ProxiedPlayer,
        kickedFrom: ServerInfo,
        reason: String
    ): ServerInfo? {
        val currentConfig = config()
        val messages = messageConfig()

        if (!currentConfig.fallback.enabled) return null

        val kickedFromServerName = kickedFrom.serverName
        val resolver = ConnectionResolver(currentConfig)

        val entry = resolver.resolve(
            targetConnections = currentConfig.fallback.targetConnections,
            currentServerName = kickedFromServerName
        )

        if (entry == null) {
            player.sendMessage(
                plainSerializer.serialize(messages.deserialize(messages.kick.noTargetConnection))
            )
            return null
        }

        val candidates = player.proxy.servers
            .filter { entry.serverNameMatcher.matches(it.serverName) }
            .filter { !it.serverName.equals(kickedFromServerName, ignoreCase = true) }

        val targetServer = candidates.randomOrNull()

        if (targetServer == null) {
            player.sendMessage(
                plainSerializer.serialize(messages.deserialize(messages.kick.noFallbackServers))
            )
            return null
        }

        return targetServer
    }
}