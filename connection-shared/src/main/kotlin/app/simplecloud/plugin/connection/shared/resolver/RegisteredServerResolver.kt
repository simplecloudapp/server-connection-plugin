package app.simplecloud.plugin.connection.shared.resolver

import app.simplecloud.plugin.connection.shared.config.RegistrationConfig
import app.simplecloud.plugin.connection.shared.registration.RegisteredServer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object RegisteredServerResolver {

    private val serializer = PlainTextComponentSerializer.plainText()

    fun resolve(server: RegisteredServer, config: RegistrationConfig): String {
        val pattern = if (!server.persistent) config.serverNamePattern else config.persistentServerNamePattern

        val resolver = TagResolver.resolver(
            Placeholder.unparsed("group", server.serverBaseName),
            Placeholder.unparsed("name", server.serverBaseName),
            Placeholder.unparsed("numerical_id", server.numericalId.toString()),
            Placeholder.unparsed("id", server.serverId),
        )

        val component = MiniMessage.miniMessage().deserialize(pattern, resolver)
        return serializer.serialize(component)
    }
}