package app.simplecloud.plugin.connection.velocity

import app.simplecloud.plugin.connection.shared.PermissionChecker
import app.simplecloud.plugin.connection.shared.ServerConnectionPlugin
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfo
import app.simplecloud.plugin.connection.shared.server.ServerConnectionInfoGetter
import com.google.inject.Inject
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.minimessage.MiniMessage
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

@Plugin(
    id = "connection-velocity",
    name = "connection-velocity",
    version = "1.0-SNAPSHOT",
    authors = ["Fllip", "hmtill"],
    url = "https://github.com/theSimpleCloud/server-connection-plugin"
)
class VelocityServerConnectionPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
    private val logger: Logger
) {

    private val serverConnection = ServerConnectionPlugin<Player>(
        dataDirectory,
        ServerConnectionInfoGetter {
            server.allServers.map {
                ServerConnectionInfo(
                    it.serverInfo.name,
                    it.playersConnected.size
                )
            }
        },
        PermissionChecker { player, permission -> player.hasPermission(permission) }
    )

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        registerCommands()
    }

    @Subscribe
    fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        val serverConnectionInfoName = serverConnection.getServerNameForLogin(event.player)
        if (serverConnectionInfoName == null) {
            event.player.disconnect(miniMessage.deserialize(
                serverConnection.config.fallbackConnectionsConfig.noTargetConnectionFoundMessage
            ))
            return
        }

        val serverInfo = server.getServer(serverConnectionInfoName)
        serverInfo.ifPresent {
            event.setInitialServer(it)
        }
    }

    @Subscribe
    fun onKickedFromServer(event: KickedFromServerEvent) {
        val connectionAndTargetConfigToServerName = serverConnection.getConnectionAndNameForFallback(event.player, event.server.serverInfo.name)
        if (connectionAndTargetConfigToServerName == null) {
            event.result = KickedFromServerEvent.DisconnectPlayer.create(miniMessage.deserialize(
                serverConnection.config.fallbackConnectionsConfig.noTargetConnectionFoundMessage
            ))
            return
        }

        val (_, serverName) = connectionAndTargetConfigToServerName
        if (event.server.serverInfo.name == serverName) {
            return
        }

        if (event.player.currentServer.isEmpty) {
            return
        }

        server.getServer(serverName).ifPresent {
            event.result = KickedFromServerEvent.RedirectPlayer.create(it)
        }
    }

    private fun registerCommands() {
        val commandManager = server.commandManager
        serverConnection.getCommandConfigs().forEach {
            val commandMeta = commandManager.metaBuilder(it.name)
                .aliases(*it.aliases.toTypedArray())
                .plugin(this)
                .build()

            val commandToRegister = createCommand(it)
            commandManager.register(commandMeta, commandToRegister)
        }
    }

    private fun createCommand(commandConfig: CommandConfig): BrigadierCommand {
        val commandNode = BrigadierCommand.literalArgumentBuilder(commandConfig.name)
            .requires { commandConfig.permission.isEmpty() || it.hasPermission(commandConfig.permission) }
            .executes {
                val player = it.source as? Player ?: return@executes 0
                val currentServerName = player.currentServer.getOrNull()?.serverInfo?.name
                val connectionToServerName = serverConnection.getConnectionAndNameForCommand(
                    player,
                    commandConfig,
                    currentServerName ?: ""
                )

                if (connectionToServerName == null) {
                    player.sendMessage(miniMessage.deserialize(commandConfig.noTargetConnectionFound))
                    return@executes 1
                }

                if (currentServerName != null
                    && connectionToServerName.first.connectionConfig.serverNameMatcher.matches(currentServerName)
                ) {
                    player.sendMessage(miniMessage.deserialize(commandConfig.alreadyConnectedMessage))
                    return@executes 1
                }

                val registeredServer = server.getServer(connectionToServerName.second)
                registeredServer.ifPresent {
                    player.createConnectionRequest(it).fireAndForget()
                }

                return@executes 1
            }
            .build()

        return BrigadierCommand(commandNode)
    }

}