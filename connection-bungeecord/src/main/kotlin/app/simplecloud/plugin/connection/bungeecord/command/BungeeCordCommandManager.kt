package app.simplecloud.plugin.connection.bungeecord.command

import app.simplecloud.plugin.connection.bungeecord.BungeeCordConnectionPlugin
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.CommandEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import org.apache.logging.log4j.LogManager

class BungeeCordCommandManager(
    private val proxy: ProxyServer,
    private val plugin: BungeeCordConnectionPlugin,
    private val scope: CoroutineScope
) {

    private val logger = LogManager.getLogger(BungeeCordCommandManager::class.java)
    private val miniMessage = MiniMessage.miniMessage()
    private val serializer = BungeeComponentSerializer.get()
    private val registeredCommands = mutableListOf<String>()

    fun registerAll(commandConfig: CommandConfig) {
        commandConfig.commands.forEach { entry ->
            registerCommand(entry)
        }
    }

    fun unregisterAll() {
        registeredCommands.forEach { _ ->
            proxy.pluginManager.unregisterCommands(plugin)
        }
        registeredCommands.clear()
    }

    private fun registerCommand(entry: CommandEntry) {
        val command = buildCommand(entry)
        proxy.pluginManager.registerCommand(plugin, command)
        registeredCommands.add(entry.name)
    }

    private fun buildCommand(entry: CommandEntry): Command {
        val permission = entry.permission.ifEmpty { null }

        return object : Command(
            entry.name,
            permission,
            *entry.aliases.toTypedArray()
        ) {
            override fun execute(sender: CommandSender, args: Array<out String>) {
                val player = sender as? ProxiedPlayer ?: return
                scope.launch { handleCommand(player, entry) }
            }
        }
    }

    private fun handleCommand(player: ProxiedPlayer, entry: CommandEntry) {
        val currentServerName = player.server?.info?.name
        val groupedByPriority = entry.targetConnections
            .groupBy { it.priority }
            .entries
            .sortedByDescending { it.key }

        for ((_, targets) in groupedByPriority) {
            for (target in targets.shuffled()) {
                if (target.from.isNotEmpty()) {
                    val matchesFrom = currentServerName != null &&
                            target.from.any { it.equals(currentServerName, ignoreCase = true) }
                    if (!matchesFrom) continue
                }

                val serverInfo = proxy.getServerInfo(target.name) ?: continue

                if (currentServerName.equals(serverInfo.name, ignoreCase = true)) {
                    player.sendMessage(miniMessage.deserialize(entry.messages.alreadyConnected))
                    return
                }

                player.connect(serverInfo)
                return
            }
        }

        player.sendMessage(miniMessage.deserialize(entry.messages.noTargetConnectionFound))
    }

    private fun ProxiedPlayer.sendMessage(component: Component) {
        val bungeeComponents = serializer.serialize(component)
        sendMessage(ChatMessageType.SYSTEM, *bungeeComponents)
    }
}