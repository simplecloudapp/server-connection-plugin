package app.simplecloud.plugin.connection.shared.connection

import app.simplecloud.plugin.connection.shared.config.ConnectionEntry
import app.simplecloud.plugin.connection.shared.config.ConnectionRule
import app.simplecloud.plugin.connection.shared.config.RuleType

object ConnectionResolver {

    fun findConnection(
        name: String,
        connections: List<ConnectionEntry>
    ): ConnectionEntry? {
        return connections.find { it.name.equals(name, ignoreCase = true) }
    }

    fun findMatchingServerNames(
        connection: ConnectionEntry,
        servers: List<String>,
    ): List<String> {
        return servers.filter { connection.serverNameMatcher.matches(it) }
    }

    fun checkRules(
        connection: ConnectionEntry,
        permissionChecker: (String) -> Boolean,
    ): ConnectionRule? {
        for (rule in connection.rules) {
            if (rule.bypassPermission.isNotEmpty() && permissionChecker(rule.bypassPermission)) {
                continue
            }

            val failed = when (rule.type) {
                RuleType.PERMISSION -> {
                    val hasPermission = permissionChecker(rule.name)
                    hasPermission != rule.value.toBoolean()
                }

                RuleType.ENV -> {
                    val envValue = System.getenv(rule.name) ?: ""
                    val matches = rule.operation.matches(envValue, rule.value, rule.negate)
                    !matches
                }
            }

            if (failed) return rule
        }
        return null
    }

    fun isServerInConnection(
        serverName: String,
        connectionName: String,
        connections: List<ConnectionEntry>,
        servers: List<String>,
    ): Boolean {
        val connection = findConnection(connectionName, connections) ?: return false
        val matchingNames = findMatchingServerNames(connection, servers)
        return matchingNames.any { it.equals(serverName, ignoreCase = true) }
    }

}
