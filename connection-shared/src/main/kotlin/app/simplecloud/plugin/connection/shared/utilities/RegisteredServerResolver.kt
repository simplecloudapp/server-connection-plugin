package app.simplecloud.plugin.connection.shared.utilities

import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object RegisteredServerResolver {

    private val serializer = PlainTextComponentSerializer.plainText()
    private val invalidPlaceholderNameCharacters = Regex("[^a-z0-9_]")

    fun resolve(
        serverId: String,
        numericalId: Int,
        serverBaseName: String,
        properties: Map<String, Any>,
        persistent: Boolean,
        config: RegistrationConfig,
    ): String {
        val pattern = if (!persistent) config.serverNamePattern else config.persistentServerNamePattern

        val resolver = TagResolver.resolver(
            listOf(
                Placeholder.unparsed("group", serverBaseName),
                Placeholder.unparsed("name", serverBaseName),
                Placeholder.unparsed("numerical_id", numericalId.toString()),
                Placeholder.unparsed("id", serverId),
            ) + properties.map {
                Placeholder.unparsed(it.key.toPlaceholderName(), it.value.toString())
            }
        )

        val component = MiniMessage.miniMessage().deserialize(pattern, resolver)
        return serializer.serialize(component)
    }

    private fun String.toPlaceholderName(): String =
        lowercase().replace(invalidPlaceholderNameCharacters, "_")
}