package app.simplecloud.plugin.connection.velocity.command

import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.velocity.VelocityConnectionPlugin
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.minimessage.MiniMessage

class VelocityCommandManager(
    private val server: ProxyServer,
    private val plugin: VelocityConnectionPlugin,
) {

    private val miniMessage = MiniMessage.miniMessage()
    private val registeredCommands = mutableListOf<String>()

    fun registerAll(commandConfig: CommandConfig) {
        commandConfig.commands.forEach { entry ->
            registerCommand(entry)
        }
    }

    fun unregisterAll() {
        val commandManager = server.commandManager
        registeredCommands.forEach { name -> commandManager.unregister(name) }
        registeredCommands.clear()
    }

    private fun registerCommand(entry: CommandEntry) {
        val commandManager = server.commandManager
        val meta = commandManager.metaBuilder(entry.name)
            .aliases(*entry.aliases.toTypedArray())
            .plugin(plugin)
            .build()

        commandManager.register(meta, buildBrigadierCommand(entry))
        registeredCommands.add(entry.name)
    }

    private fun buildBrigadierCommand(entry: CommandEntry): BrigadierCommand {
        val node = BrigadierCommand.literalArgumentBuilder(entry.name)
            .requires {
                entry.permission.isEmpty() || it.hasPermission(entry.permission)
            }
            .executes { context ->
                val player = context.source as? Player ?: return@executes 0
                handleCommand(player, entry)
                1
            }
            .build()

        return BrigadierCommand(node)
    }

    private fun handleCommand(player: Player, entry: CommandEntry) {
        val currentServerName = player.currentServer.orElse(null)?.serverInfo?.name
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

                val registeredServer = server.getServer(target.name).orElse(null) ?: continue

                if (currentServerName.equals(registeredServer.serverInfo.name, ignoreCase = true)) {
                    player.sendMessage(miniMessage.deserialize(entry.messages.alreadyConnected))
                    return
                }

                player.createConnectionRequest(registeredServer).fireAndForget()
                return
            }
        }

        player.sendMessage(miniMessage.deserialize(entry.messages.noTargetConnectionFound))
    }
}