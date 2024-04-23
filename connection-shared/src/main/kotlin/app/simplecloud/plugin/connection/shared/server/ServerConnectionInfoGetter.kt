package app.simplecloud.plugin.connection.shared.server

fun interface ServerConnectionInfoGetter {

    fun get(): List<ServerConnectionInfo>

}