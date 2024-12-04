package app.simplecloud.plugin.connection.shared.matcher

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object EndsWithOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.endsWith(value, ignoreCase = true)
    }

}