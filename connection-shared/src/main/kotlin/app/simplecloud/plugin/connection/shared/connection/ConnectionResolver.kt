package app.simplecloud.plugin.connection.shared.connection

import app.simplecloud.plugin.connection.shared.config.ConnectionConfig
import app.simplecloud.plugin.connection.shared.config.ConnectionEntry
import app.simplecloud.plugin.connection.shared.config.TargetConnection

/**
 * Resolves a list of [TargetConnection]s to the best matching [ConnectionEntry]
 */
class ConnectionResolver(
    private val config: ConnectionConfig
) {

    /**
     * Resolves the best [ConnectionEntry] for the given [targetConnections].
     *
     * @param targetConnections the list of target connections to try
     * @param currentServerName the name of the server the player is currently on
     * @return the resolved [ConnectionEntry]
     */
    fun resolve(
        targetConnections: List<TargetConnection>,
        currentServerName: String?
    ): ConnectionEntry? {
        val groupedByPriority = targetConnections
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

                val entry = config.connections.find { it.name == target.name }
                    ?: continue

                return entry
            }
        }

        return null
    }

}