package app.simplecloud.plugin.connection.shared.registration

interface ServerRegistry {

    fun getRegistered(): Map<String, RegisteredServer>

    fun register(server: RegisteredServer)

    fun unregister(server: RegisteredServer)

}