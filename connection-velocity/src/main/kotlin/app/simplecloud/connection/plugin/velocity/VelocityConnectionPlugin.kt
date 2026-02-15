package app.simplecloud.connection.plugin.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(
    id = "connection-velocity",
    name = "connection-velocity",
    version = "1.0-SNAPSHOT",
    authors = ["Fllip", "hmtill"],
    url = "https://github.com/simplecloudapp/server-connection-plugin"
)
class VelocityConnectionPlugin @Inject constructor(
    @DataDirectory val dataDirectory: Path,
    private val server: ProxyServer,
) {

    private val logger = LogManager.getLogger(VelocityConnectionPlugin::class.java)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        logger.info("Initialize velocity connection plugin...")
    }

}