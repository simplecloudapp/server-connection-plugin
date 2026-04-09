package app.simplecloud.plugin.connection.shared.config

import app.simplecloud.plugin.connection.shared.utilities.ConfigVersion
import app.simplecloud.plugin.connection.shared.utilities.DefaultConfigs
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class MessageConfig(
    val version: Char = ConfigVersion.VERSION,
    @Comment("Variables\nReusable variables that can be used throughout the messages\nUsage: <variable_name> will be replaced with the defined value")
    val variables: Map<String, String> = DefaultConfigs.VARIABLES,
    @Comment("Kick Messages")
    val kick: KickMessages = KickMessages(),
    @Comment("Command Messages")
    val command: ConnectionCommandMessages = ConnectionCommandMessages()
) {
    private val miniMessage = MiniMessage.miniMessage()

    private fun tagResolver(): TagResolver {
        val resolvers = variables.map { (key, value) ->
            TagResolver.resolver(key, Tag.selfClosingInserting(miniMessage.deserialize(value)))
        }
        return TagResolver.resolver(*resolvers.toTypedArray())
    }

    fun send(message: String, vararg tagResolver: TagResolver): Component =
        miniMessage.deserialize(message, TagResolver.resolver(tagResolver(), *tagResolver))
}

@ConfigSerializable
data class KickMessages(
    val noFallbackServers: String = "<color:#dc2626>There is no fallback server available.",
    val noTargetConnection: String = "<color:#dc2626>You have been disconnected from the network<br>because there are no fallback servers available.",
)

@ConfigSerializable
data class ConnectionCommandMessages(
    val commandUsage: String = "<prefix> <color:#ffffff>Usage: /connection reload",
    val configReloading: String = "<prefix> <color:#ffffff>Reloading Connection configurations...",
    val configReloadedSuccess: String = "<prefix> <color:#22c55e>Successfully reloaded all Connection configurations.",
    val configReloadedFailed: String = "<prefix> <color:#dc2626>Failed to reload Connection configurations."
)