package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MatcherConfig(
    val operation: MatcherOperation = MatcherOperation.STARTS_WITH,
    val value: String = "",
    val negate: Boolean = false,
) {

    fun matches(name: String): Boolean {
        return operation.matches(name, value, negate)
    }

    fun anyMatches(names: List<String>): Boolean{
        return operation.anyMatches(names, value, negate)
    }

    fun allMatches(names: List<String>): Boolean{
        return operation.allMatches(names, value, negate)
    }

}