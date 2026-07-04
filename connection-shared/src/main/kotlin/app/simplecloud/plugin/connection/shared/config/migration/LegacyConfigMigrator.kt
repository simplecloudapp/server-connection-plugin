package app.simplecloud.plugin.connection.shared.config.migration

import app.simplecloud.plugin.api.shared.config.ConfigMigration
import app.simplecloud.plugin.api.shared.config.ConfigMigrator
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.api.shared.matcher.OperationType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import app.simplecloud.plugin.connection.shared.config.AddressConfig
import app.simplecloud.plugin.connection.shared.config.CommandConfig
import app.simplecloud.plugin.connection.shared.config.CommandEntry
import app.simplecloud.plugin.connection.shared.config.CommandMessages
import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionEntry
import app.simplecloud.plugin.connection.shared.config.ConnectionRule
import app.simplecloud.plugin.connection.shared.config.FallbackConfig
import app.simplecloud.plugin.connection.shared.config.FallbackTargetConnection
import app.simplecloud.plugin.connection.shared.config.KickMessages
import app.simplecloud.plugin.connection.shared.config.MessageConfig
import app.simplecloud.plugin.connection.shared.config.NetworkJoinTargetsConfig
import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import app.simplecloud.plugin.connection.shared.config.RuleType
import app.simplecloud.plugin.connection.shared.config.TargetConnection
import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import org.spongepowered.configurate.CommentedConfigurationNode
import java.io.File
import java.util.Locale

object LegacyConfigMigrator {

    fun create(dataDirectory: File): ConfigMigrator {
        return ConfigMigrator.builder(ConfigVersion.VERSION)
            .fallbackVersion(1)
            .migrate(1, ConfigVersion.VERSION, LegacyConfigMigration(dataDirectory))
            .build()
    }

    private class LegacyConfigMigration(
        private val dataDirectory: File
    ) : ConfigMigration {

        override fun migrate(node: CommentedConfigurationNode) {
            val connections = readConnections(node).toMutableList()
            val fallback = readFallback(node, connections)

            val connectionConfig = ConnectionConfig(
                version = ConfigVersion.VERSION,
                registration = RegistrationConfig(),
                address = AddressConfig(),
                connections = connections,
                networkJoinTargets = readNetworkJoinTargets(node),
                fallback = fallback,
            )
            val commandConfig = CommandConfig(
                version = ConfigVersion.VERSION,
                commands = readCommands(node),
            )
            val messageConfig = readMessages(node)

            writeSplitConfig(File(dataDirectory, "commands.yml"), commandConfig, CommandConfig::class.java)
            writeSplitConfig(File(dataDirectory, "messages.yml"), messageConfig, MessageConfig::class.java)
            node.set(ConnectionConfig::class.java, connectionConfig)
        }

        private fun readConnections(root: CommentedConfigurationNode): List<ConnectionEntry> {
            return root.child("connections").childrenList().map { connection ->
                ConnectionEntry(
                    name = connection.string("name"),
                    serverNameMatcher = connection.child("server-name-matcher", "serverNameMatcher").matcher(),
                    rules = connection.child("rules").childrenList().map { it.rule() },
                )
            }
        }

        private fun readNetworkJoinTargets(root: CommentedConfigurationNode): NetworkJoinTargetsConfig {
            val legacyTargets = root.child("network-join-targets", "networkJoinTargets")
            return NetworkJoinTargetsConfig(
                enabled = legacyTargets.boolean("enabled", default = false),
                targetConnections = legacyTargets.child("target-connections", "targetConnections")
                    .childrenList()
                    .map { target ->
                        TargetConnection(
                            name = target.string("name"),
                            priority = target.int("priority"),
                        )
                    },
            )
        }

        private fun readFallback(
            root: CommentedConfigurationNode,
            connections: MutableList<ConnectionEntry>
        ): FallbackConfig {
            val legacyTargets = root.child("fallback-connections-config", "fallbackConnectionsConfig")
            val fromConnectionNames = LinkedHashMap<ServerMatcherConfiguration, String>()

            return FallbackConfig(
                enabled = legacyTargets.boolean("enabled", default = false),
                targetConnections = legacyTargets.child("target-connections", "targetConnections")
                    .childrenList()
                    .map { target ->
                        FallbackTargetConnection(
                            name = target.string("name"),
                            priority = target.int("priority"),
                            from = target.child("from").childrenList().map { fromMatcher ->
                                fromConnectionNames.getOrPut(fromMatcher.matcher()) {
                                    findOrCreateFromConnection(fromMatcher.matcher(), connections)
                                }
                            },
                        )
                    },
            )
        }

        private fun readCommands(root: CommentedConfigurationNode): List<CommandEntry> {
            return root.child("commands").childrenList().map { command ->
                CommandEntry(
                    name = command.string("name"),
                    aliases = command.child("aliases").childrenList().map { it.string() },
                    permission = command.string("permission"),
                    messages = CommandMessages(
                        alreadyConnected = command.string(
                            "already-connected-message",
                            "alreadyConnectedMessage",
                            default = CommandMessages().alreadyConnected
                        ),
                        noTargetConnectionFound = command.string(
                            "no-target-connection-found",
                            "noTargetConnectionFound",
                            default = CommandMessages().noTargetConnectionFound
                        ),
                    ),
                    targetConnections = command.child("target-connections", "targetConnections")
                        .childrenList()
                        .map { target ->
                            FallbackTargetConnection(
                                name = target.string("name"),
                                priority = target.int("priority"),
                                from = listOf(),
                            )
                        },
                )
            }
        }

        private fun readMessages(root: CommentedConfigurationNode): MessageConfig {
            val defaults = MessageConfig()
            val networkJoinTargets = root.child("network-join-targets", "networkJoinTargets")
            val fallback = root.child("fallback-connections-config", "fallbackConnectionsConfig")

            return defaults.copy(
                version = ConfigVersion.VERSION,
                kick = KickMessages(
                    noFallbackServers = fallback.string(
                        "no-target-connection-found-message",
                        "noTargetConnectionFoundMessage",
                        default = defaults.kick.noFallbackServers
                    ),
                    noTargetConnection = networkJoinTargets.string(
                        "no-target-connection-found-message",
                        "noTargetConnectionFoundMessage",
                        default = defaults.kick.noTargetConnection
                    ),
                ),
            )
        }

        private fun findOrCreateFromConnection(
            matcher: ServerMatcherConfiguration,
            connections: MutableList<ConnectionEntry>
        ): String {
            val existingConnection = connections.firstOrNull { it.serverNameMatcher == matcher }
            if (existingConnection != null) return existingConnection.name

            val name = uniqueConnectionName(connectionNameForMatcher(matcher), connections)
            connections += ConnectionEntry(
                name = name,
                serverNameMatcher = matcher,
                rules = listOf(),
            )
            return name
        }

        private fun connectionNameForMatcher(matcher: ServerMatcherConfiguration): String {
            val operation = matcher.operation.name
                .lowercase(Locale.ROOT)
                .replace('_', '-')
            val value = matcher.value
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9-]+"), "-")
                .trim('-')
            val suffix = value.ifEmpty { "matcher" }
            return "legacy-from-$operation-$suffix".take(64).trim('-')
        }

        private fun uniqueConnectionName(
            preferredName: String,
            connections: List<ConnectionEntry>
        ): String {
            val usedNames = connections.mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) }
            if (preferredName.lowercase(Locale.ROOT) !in usedNames) return preferredName

            var index = 2
            while (true) {
                val candidate = "$preferredName-$index"
                if (candidate.lowercase(Locale.ROOT) !in usedNames) return candidate
                index++
            }
        }

        private fun <E> writeSplitConfig(file: File, config: E, javaClass: Class<E>) {
            ConfigurationFactory(file, javaClass).save(config)
        }

    }

}

private fun CommentedConfigurationNode.child(vararg names: String): CommentedConfigurationNode {
    return names
        .asSequence()
        .map { this.node(it) }
        .firstOrNull { !it.virtual() }
        ?: this.node(names.first())
}

private fun CommentedConfigurationNode.string(
    vararg names: String,
    default: String = ""
): String {
    if (names.isEmpty()) return getString(default)
    return child(*names).getString(default)
}

private fun CommentedConfigurationNode.boolean(
    vararg names: String,
    default: Boolean = false
): Boolean {
    return child(*names).getBoolean(default)
}

private fun CommentedConfigurationNode.int(
    vararg names: String,
    default: Int = 0
): Int {
    return child(*names).getInt(default)
}

private fun CommentedConfigurationNode.matcher(): ServerMatcherConfiguration {
    return ServerMatcherConfiguration(
        operation = enumValue(string("operation"), OperationType.STARTS_WITH),
        value = string("value"),
        negate = boolean("negate"),
    )
}

private fun CommentedConfigurationNode.rule(): ConnectionRule {
    return ConnectionRule(
        type = enumValue(string("type"), RuleType.ENV),
        name = string("name"),
        value = string("value"),
        operation = enumValue(string("operation"), OperationType.STARTS_WITH),
        negate = boolean("negate"),
        bypassPermission = string("bypass-permission", "bypassPermission"),
    )
}

private inline fun <reified E : Enum<E>> enumValue(rawValue: String, default: E): E {
    if (rawValue.isBlank()) return default
    val normalized = rawValue
        .replace('-', '_')
        .uppercase(Locale.ROOT)
    return enumValues<E>().firstOrNull { it.name == normalized } ?: default
}
