package app.simplecloud.plugin.connection.waterdog.command

import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.CommandEntry
import dev.waterdog.waterdogpe.command.Command
import dev.waterdog.waterdogpe.command.CommandSender
import dev.waterdog.waterdogpe.command.CommandSettings
import dev.waterdog.waterdogpe.player.ProxiedPlayer
import dev.waterdog.waterdogpe.ProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.apache.logging.log4j.LogManager

class WaterDogCommandManager(
    private val proxy: ProxyServer,
    private val scope: CoroutineScope
) {

    private val logger = LogManager.getLogger(WaterDogCommandManager::class.java)
    private val miniMessage = MiniMessage.miniMessage()
    private val registeredCommands = mutableListOf<String>()

    fun registerAll(commandConfig: CommandConfig) {
        commandConfig.commands.forEach { entry ->
            registerCommand(entry)
        }
        logger.info("Registered ${registeredCommands.size} command(s): ${registeredCommands.joinToString(", ")}")
    }

    fun unregisterAll() {
        registeredCommands.forEach { name ->
            proxy.commandMap.unregisterCommand(name)
        }
        logger.info("Unregistered ${registeredCommands.size} command(s)")
        registeredCommands.clear()
    }

    private fun registerCommand(entry: CommandEntry) {
        val command = buildCommand(entry)
        proxy.commandMap.registerCommand(command)
        registeredCommands.add(entry.name)
    }

    private fun buildCommand(entry: CommandEntry): Command {
        val settings = CommandSettings.builder()
            .setAliases(entry.aliases.toTypedArray().toString())
            .apply {
                if (entry.permission.isNotEmpty()) {
                    permission = entry.permission
                }
            }
            .build()

        return object : Command(entry.name, settings) {
            override fun onExecute(sender: CommandSender, alias: String?, args: Array<out String>): Boolean {
                val player = sender as? ProxiedPlayer ?: return false
                scope.launch { handleCommand(player, entry) }
                return true
            }
        }
    }

    private fun handleCommand(player: ProxiedPlayer, entry: CommandEntry) {
        val currentServerName = player.serverInfo?.serverName
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

                if (currentServerName.equals(serverInfo.serverName, ignoreCase = true)) {
                    sendMessage(player, entry.messages.alreadyConnected)
                    return
                }

                player.connect(serverInfo)
                return
            }
        }

        sendMessage(player, entry.messages.noTargetConnectionFound)
    }

    private fun sendMessage(player: ProxiedPlayer, rawMessage: String) {
        val component = miniMessage.deserialize(rawMessage)
        val plain = PlainTextComponentSerializer.plainText().serialize(component)
        player.sendMessage(plain)
    }
}