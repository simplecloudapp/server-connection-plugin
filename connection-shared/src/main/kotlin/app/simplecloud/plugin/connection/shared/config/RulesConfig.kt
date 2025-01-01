package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.api.shared.matcher.MatcherType
import app.simplecloud.plugin.connection.shared.PermissionChecker
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class RulesConfig(
    val type: Type = Type.ENV,
    val operation: MatcherType = MatcherType.STARTS_WITH,
    val name: String = "",
    val value: String = "",
    val negate: Boolean = false,
    val bypassPermission: String = ""
) {

    enum class Type {
        ENV,
        PERMISSION
    }

    fun <P> isAllowed(player: P, permissionChecker: PermissionChecker<P>): Boolean {
        if (bypassPermission.isNotEmpty() && permissionChecker.checkPermission(player, bypassPermission)) {
            return true
        }

        when (type) {
            Type.ENV -> {
                val env = System.getenv(name)
                return operation.matches(env, value, negate)
            }

            Type.PERMISSION -> {
                return permissionChecker.checkPermission(player, name).toString().equals(value, true)
            }
        }
    }

}