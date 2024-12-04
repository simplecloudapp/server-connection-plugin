package app.simplecloud.plugin.connection.shared.matcher

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object EqualsOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.equals(value, ignoreCase = true)
    }

}