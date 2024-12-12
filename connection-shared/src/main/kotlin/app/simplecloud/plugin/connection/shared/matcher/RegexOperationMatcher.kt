package app.simplecloud.plugin.connection.shared.matcher

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object RegexOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.equals(Regex.fromLiteral(value))
    }

}