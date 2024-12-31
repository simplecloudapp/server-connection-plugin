package app.simplecloud.plugin.connection.shared.matcher

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object RegexOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return Regex(value).matches(name)
    }

}