package app.simplecloud.plugin.connection.bungeecord

import net.md_5.bungee.api.plugin.Plugin
import org.apache.logging.log4j.LogManager

class BungeeCordConnectionPlugin : Plugin() {

    private val logger = LogManager.getLogger(BungeeCordConnectionPlugin::class.java)

    override fun onEnable() {
        logger.info("Initialize bungeecord connection plugin...")
    }
}