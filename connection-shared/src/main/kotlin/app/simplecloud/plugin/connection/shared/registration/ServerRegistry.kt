package app.simplecloud.plugin.connection.shared.registration

interface ServerRegistry {

    fun register(proxyName: String, server: RegisteredServer)

    fun unregister(proxyName: String, server: RegisteredServer)

}
