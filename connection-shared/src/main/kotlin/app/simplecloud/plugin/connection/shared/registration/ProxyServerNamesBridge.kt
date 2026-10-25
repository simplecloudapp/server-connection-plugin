package app.simplecloud.plugin.connection.shared.registration

import app.simplecloud.api.proxy.ProxyServerNames
import java.util.Optional
import java.util.function.Function

/**
 * Holds every reference to [ProxyServerNames], which cloud-api 0.1.0-platform.58 and .59 don't have.
 *
 * Loading a class that calls it works on those versions, but reflection or eager resolution of the class
 * (`getDeclaredFields`, `getDeclaredMethods`) fails. Keeping the references here keeps the always-loaded classes
 * safe. On those versions [register] throws [NoClassDefFoundError], which the caller catches.
 */
internal object ProxyServerNamesBridge {

    fun register(proxyNameOfServerId: Function<String, Optional<String>>): AutoCloseable =
        ProxyServerNames.register(proxyNameOfServerId)
}
