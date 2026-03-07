package app.simplecloud.plugin.connection.shared.utilities

import app.simplecloud.plugin.api.shared.matcher.OperationType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import app.simplecloud.plugin.connection.shared.config.*

object DefaultConfigs {

    val VARIABLES: Map<String, String> = mapOf("prefix" to "<color:#38bdf8><bold>⚡</bold></color>")

    val CONNECTIONS: List<ConnectionEntry> = listOf(
        ConnectionEntry(
            name = "lobby",
            serverNameMatcher = ServerMatcherConfiguration(
                operation = OperationType.STARTS_WITH,
                value = "lobby",
                negate = false,
            ),
            rules = listOf(),
        ),
    )

    val NETWORK_JOIN_TARGETS: NetworkJoinTargetsConfig = NetworkJoinTargetsConfig(
        enabled = true,
        targetConnections = listOf(
            TargetConnection(
                name = "lobby",
                priority = 0,
            ),
        ),
    )

    val FALLBACK: FallbackConfig = FallbackConfig(
        enabled = true,
        targetConnections = listOf(
            FallbackTargetConnection(
                name = "lobby",
                priority = 0,
                from = listOf(),
            ),
        ),
    )

    val COMMANDS: List<CommandEntry> = listOf(
        CommandEntry(
            name = "lobby",
            aliases = listOf("l", "hub", "quit", "leave"),
            targetConnections = listOf(
                FallbackTargetConnection(
                    name = "lobby",
                    priority = 0,
                    from = listOf(),
                ),
            ),
            messages = CommandMessages(
                alreadyConnected = "<color:#dc2626>You are already connected to this lobby!",
                noTargetConnectionFound = "<color:#dc2626>Couldn't find a target server!",
            ),
            permission = "",
        ),
    )

}