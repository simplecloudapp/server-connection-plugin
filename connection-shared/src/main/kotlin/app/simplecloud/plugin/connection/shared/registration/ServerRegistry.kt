package app.simplecloud.plugin.connection.shared.registration

/**
 * Manages server registration within the simplecloud network.
 */
interface ServerRegistry {

    /**
     * Retrieves all currently registered servers.
     *
     * @return map of server IDs to their registered server instances
     */
    fun getRegistered(): Map<String, RegisteredServer>

    /**
     * Registers a new server.
     *
     * @param server The server to register
     */
    fun register(server: RegisteredServer)

    /**
     * Unregisters a server.
     *
     * @param server The server to unregister
     */
    fun unregister(server: RegisteredServer)
}