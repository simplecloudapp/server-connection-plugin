package app.simplecloud.plugin.connection.shared.registration

/**
 * Manages server registration within the simplecloud network.
 */
interface ServerRegistry {

    /**
     * Registers a new server.
     *
     * @param proxyName The resolved name used by the proxy
     * @param server The server to register
     */
    fun register(proxyName: String, server: RegisteredServer)

    /**
     * Unregisters a server.
     *
     * @param proxyName The resolved name used by the proxy
     * @param server The server to unregister
     */
    fun unregister(proxyName: String, server: RegisteredServer)
}
