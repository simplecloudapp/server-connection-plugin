package app.simplecloud.plugin.connection.bungeecord

import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command

/**
 * @author Niklas Nieberler
 */

class BungeeCordCommand(
    private val serverConnection: ServerConnectionPlugin<ProxiedPlayer>,
    private val commandConfig: CommandConfig,
    private val proxyServer: ProxyServer,
    private val miniMessage: MiniMessage,
) : Command(
    commandConfig.name,
    commandConfig.permission,
    *commandConfig.aliases.toTypedArray()
) {

    override fun execute(sender: CommandSender, args: Array<out String>) {
        val player = sender as ProxiedPlayer? ?: return

        val currentServerName = player.server.info.name
        val connectionToServerName =
            this.serverConnection.getConnectionAndNameForCommand(player, this.commandConfig)

        if (connectionToServerName == null) {
            player.sendMessage(*BungeeComponentSerializer.get().serialize(miniMessage.deserialize(commandConfig.noTargetConnectionFound)))
            return
        }

        if (currentServerName != null
            && connectionToServerName.first.connectionConfig.serverNameMatcher.matches(currentServerName)
        ) {
            val miniMessageComponent = this.miniMessage.deserialize(this.commandConfig.alreadyConnectedMessage)
            val component = BungeeComponentSerializer.get().serialize(miniMessageComponent)
            player.sendMessage(*component)
            return
        }

        val serverInfo = this.proxyServer.getServerInfo(connectionToServerName.second)
        if (serverInfo != null) {
            player.connect(serverInfo)
        }
    }

}