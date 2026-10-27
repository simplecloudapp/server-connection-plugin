package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.proxy.ProxyServerNames
import java.util.Optional

/**
 * Holds every reference to [ProxyServerNames], which cloud-api before 0.1.0-platform.61 doesn't have.
 *
 * Loading a class that calls it works on those versions, but reflection or eager resolution of the class
 * (`getDeclaredFields`, `getDeclaredMethods`) fails. Keeping the references here keeps the always-loaded classes
 * safe. That is also why [register] takes a plain function and builds the [ProxyServerNames.Lookup] itself.
 * On those versions [register] throws [NoClassDefFoundError], which the caller catches.
 */
internal object ProxyServerNamesBridge {

    fun register(findProxyName: (serverId: String) -> Optional<String>): AutoCloseable {
        val lookup = ProxyServerNames.Lookup { serverId -> findProxyName(serverId) }
        return ProxyServerNames.register(lookup)
    }
}
