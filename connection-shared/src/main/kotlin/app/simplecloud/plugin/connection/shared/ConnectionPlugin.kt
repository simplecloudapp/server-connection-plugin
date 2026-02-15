package app.simplecloud.plugin.connection.shared

import org.apache.logging.log4j.LogManager
import java.nio.file.Path

class ConnectionPlugin<P>(
    private val dir: Path
) {

    private val logger = LogManager.getLogger(ConnectionPlugin::class.java)
}