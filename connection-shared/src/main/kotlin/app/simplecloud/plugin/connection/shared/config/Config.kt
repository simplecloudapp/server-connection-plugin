package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.MatcherType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import org.spongepowered.configurate.objectmapping.ConfigSerializable
@ConfigSerializable
data class Config(
    val version: String = "1",
    val connections: List<ConnectionConfig> = emptyList(),
    val networkJoinTargets: TargetsConfig = TargetsConfig(
        noTargetConnectionFoundMessage = "<color:#dc2626>Couldn't connect you to the network because no target servers are available."
    ),
    val fallbackConnectionsConfig: TargetsConfig = TargetsConfig(
        noTargetConnectionFoundMessage = "<color:#dc2626>You have been disconnected from the network since you have been kicked and no fallback server are available."
    ),
    val commands: List<CommandConfig> = emptyList(),
) {
    companion object {
        fun createDefaultConfig(): Config {
            val defaultConnections = listOf(
                ConnectionConfig(
                    name = "lobby",
                    serverNameMatcher = ServerMatcherConfiguration(
                        operation = MatcherType.STARTS_WITH,
                        value = "lobby"
                    )
                ),
                ConnectionConfig(
                    name = "hub",
                    serverNameMatcher = ServerMatcherConfiguration(
                        operation = MatcherType.STARTS_WITH,
                        value = "hub"
                    )
                ),
                ConnectionConfig(
                    name = "premium-lobby",
                    serverNameMatcher = ServerMatcherConfiguration(
                        operation = MatcherType.STARTS_WITH,
                        value = "premium"
                    ),
                    rules = listOf(
                        RulesConfig(
                            type = RulesConfig.Type.PERMISSION,
                            name = "simplecloud.connection.premium",
                            value = "true",
                        )
                    )
                ),
                ConnectionConfig(
                    name = "vip-lobby",
                    serverNameMatcher = ServerMatcherConfiguration(
                        operation = MatcherType.STARTS_WITH,
                        value = "vip"
                    ),
                    rules = listOf(
                        RulesConfig(
                            type = RulesConfig.Type.PERMISSION,
                            name = "simplecloud.connection.vip",
                            value = "true",
                        )
                    )
                ),
                ConnectionConfig(
                    name = "silent-lobby",
                    serverNameMatcher = ServerMatcherConfiguration(
                        operation = MatcherType.STARTS_WITH,
                        value = "silent"
                    ),
                    rules = listOf(
                        RulesConfig(
                            type = RulesConfig.Type.PERMISSION,
                            name = "simplecloud.connection.silent",
                            value = "true",
                        )
                    )
                )
            )

            val defaultTargetConnections = listOf(
                TargetConnectionConfig("lobby", 0),
                TargetConnectionConfig("hub", 0),
                TargetConnectionConfig("premium-lobby", 10),
                TargetConnectionConfig("vip-lobby", 20),
                TargetConnectionConfig("silent-lobby", 20)
            )

            val networkJoinTargets = TargetsConfig(
                enabled = true,
                noTargetConnectionFoundMessage = "<color:#dc2626>Couldn't connect you to the network because\nno target servers are available.",
                targetConnections = defaultTargetConnections
            )

            val fallbackConnectionsConfig = TargetsConfig(
                enabled = true,
                noTargetConnectionFoundMessage = "<color:#dc2626>You have been disconnected from the network\nbecause there are no fallback servers available.",
                targetConnections = defaultTargetConnections
            )

            val defaultCommands = listOf(
                CommandConfig(
                    name = "lobby",
                    alreadyConnectedMessage = "<color:#dc2626>You are already connected to this lobby!",
                    noTargetConnectionFound = "<color:#dc2626>Couldn't find a target server!",
                    targetConnections = defaultTargetConnections,
                    aliases = listOf("l", "hub", "quit", "leave")
                )
            )

            return Config(
                connections = defaultConnections,
                networkJoinTargets = networkJoinTargets,
                fallbackConnectionsConfig = fallbackConnectionsConfig,
                commands = defaultCommands
            )
        }
    }
}
