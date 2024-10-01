package app.simplecloud.plugin.connection.shared.config

data class TargetConnectionConfig (
    val name: String = "",
    val priority: Int = 0,
    val from: List<MatcherConfig> = emptyList()
)
