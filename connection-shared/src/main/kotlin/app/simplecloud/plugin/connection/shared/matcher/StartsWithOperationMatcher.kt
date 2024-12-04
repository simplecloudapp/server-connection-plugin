package app.simplecloud.plugin.connection.shared.matcher

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
object StartsWithOperationMatcher : OperationMatcher {

    override fun matches(name: String, value: String): Boolean {
        return name.startsWith(value, ignoreCase = true)
    }

}