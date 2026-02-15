package app.simplecloud.plugin.connection.waterdog

import dev.waterdog.waterdogpe.plugin.Plugin
import org.apache.logging.log4j.LogManager

class WaterDogConnectionPlugin : Plugin() {

    private val logger = LogManager.getLogger(WaterDogConnectionPlugin::class.java)

    override fun onEnable() {
        logger.info("Initialize waterdog connection plugin...")
    }
}