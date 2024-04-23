package app.simplecloud.plugin.connection.shared

fun interface PermissionChecker<P> {

    fun checkPermission(player: P, permission: String): Boolean

}