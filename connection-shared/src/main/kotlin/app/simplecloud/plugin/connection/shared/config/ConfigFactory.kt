package app.simplecloud.plugin.connection.shared.config

import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.kotlin.toNode
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path

object ConfigFactory {

    fun loadOrCreate(dataDirectory: Path): Config {
        val path = dataDirectory.resolve("config.yml")
        val loader = YamlConfigurationLoader.builder()
            .path(path)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers {
                    it.registerAnnotatedObjects(objectMapperFactory()).build()
                }
            }
            .build()

        if (!Files.exists(path)) {
            return create(path, loader)
        }

        val configurationNode = loader.load()
        return configurationNode.get() ?: throw IllegalStateException("Config could not be loaded")
    }


    private fun create(path: Path, loader: YamlConfigurationLoader): Config {
        val config = Config()
        if (!Files.exists(path)) {
            path.parent?.let { Files.createDirectories(it) }
            Files.createFile(path)

            val configurationNode = loader.load()
            config.toNode(configurationNode)
            loader.save(configurationNode)
        }

        return config
    }

}