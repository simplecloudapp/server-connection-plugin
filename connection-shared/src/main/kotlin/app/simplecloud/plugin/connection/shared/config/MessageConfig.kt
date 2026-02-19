package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    val version: Char = ConfigVersion.VERSION,
    val variables: Map<String, String> = mapOf(),
    val kick: KickMessages = KickMessages()
) {
    private fun buildVariableResolver(): TagResolver {
        val placeholders = variables.map { (key, value) ->
            Placeholder.parsed(key, value)
        }
        return TagResolver.resolver(placeholders)
    }

    fun deserialize(raw: String, vararg extra: TagResolver): Component {
        val resolver = TagResolver.resolver(buildVariableResolver(), *extra)
        return MiniMessage.miniMessage().deserialize(raw, resolver)
    }
}

@ConfigSerializable
data class KickMessages(
    val noFallbackServers: String = "<rose>There is no fallback server available.",
    val noTargetConnection: String = "<rose>You have been disconnected from the network<br>because there are no fallback servers available."
)