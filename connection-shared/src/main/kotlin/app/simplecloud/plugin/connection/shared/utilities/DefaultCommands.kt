package app.simplecloud.plugin.connection.shared.utilities

import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.config.CommandMessages
import app.simplecloud.plugin.connection.shared.config.TargetConnection

object DefaultCommands {

    val DEFAULT: List<CommandEntry> = listOf(
        lobbyCommand()
    )

    private fun lobbyCommand() = CommandEntry(
        name = "lobby",
        aliases = listOf("l", "hub", "quit", "leave"),
        targetConnections = listOf(
            TargetConnection(
                name = "lobby",
                priority = 0,
                from = emptyList()
            )
        ),
        messages = CommandMessages(
            alreadyConnected = "<color:#dc2626>You are already connected to this lobby!",
            noTargetConnectionFound = "<color:#dc2626>Couldn't find a target server!"
        ),
        permission = ""
    )
}