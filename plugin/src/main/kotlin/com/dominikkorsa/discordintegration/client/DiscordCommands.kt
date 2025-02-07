package com.dominikkorsa.discordintegration.client

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.utils.addOption
import com.dominikkorsa.discordintegration.utils.createApplicationCommand
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.UserInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionReplyEditSpec
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit

class DiscordCommands(private val plugin: DiscordIntegration) {
    companion object {
        private const val linkCommandName = "link"
        private const val executeCommandName = "execute"
        private const val profileInfoCommandName = "Minecraft profile info"
    }

    suspend fun handleChatInputInteraction(event: ChatInputInteractionEvent) {
        when (event.commandName) {
            linkCommandName -> handleLinkMinecraftCommand(event)
            executeCommandName -> handleExecuteCommand(event)
            else -> event.deleteReply().awaitFirstOrNull()
        }
    }

    private suspend fun handleExecuteCommand(event: ChatInputInteractionEvent) {
        val executorId = event.interaction.user.id
        val discordRole = plugin.configManager.chat.discordExecuteRole
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        if (!plugin.client.doesMemberHaveRole(executorId, discordRole)) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle("You don't have permission to execute commands.")
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
            return
        }

        val command = event.getOption("command").get().value.get().asString()
        val result = plugin.runConsoleCommand(command)
        event.editReply("Command executed in Minecraft console:\n```\n$result\n```").awaitFirstOrNull()
    }

    private suspend fun handleLinkMinecraftCommand(event: ChatInputInteractionEvent) {
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        val player = plugin.linking.link(
            event.getOption("code").get().value.get().asString(),
            event.interaction.user
        )

        if (player == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.linkingUnknownCodeTitle)
                            .withDescription(plugin.messages.discord.linkingUnknownCodeContent)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
        } else {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.linkingSuccessTitle)
                            .withThumbnail(plugin.avatarService.getAvatarUrl(player))
                            .withFields(
                                EmbedCreateFields.Field.of(
                                    plugin.messages.discord.linkingSuccessPlayerNameHeader,
                                    player.name,
                                    false
                                )
                            )
                            .withColor(Color.of(0x06d6a0))
                    )
            ).awaitFirstOrNull()
        }
    }

    suspend fun handleUserInteraction(event: UserInteractionEvent) {
        when (event.commandName) {
            profileInfoCommandName -> handleProfileInfoCommand(event)
            else -> event.deleteReply().awaitFirstOrNull()
        }
    }

    private suspend fun handleProfileInfoCommand(event: UserInteractionEvent) {
        event.deferReply().withEphemeral(true).awaitFirstOrNull()
        val playerId = plugin.db.playerIdOfMember(event.targetId)
        if (playerId == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.profileInfoNotLinked)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
            return
        }

        val player = Bukkit.getOfflinePlayer(playerId)
        val name = player.name
        if (name == null) {
            event.editReply(
                InteractionReplyEditSpec.create()
                    .withEmbeds(
                        EmbedCreateSpec.create()
                            .withTitle(plugin.messages.discord.profileInfoError)
                            .withColor(Color.of(0xef476f))
                    )
            ).awaitFirstOrNull()
            return
        }

        event.editReply(
            InteractionReplyEditSpec.create()
                .withEmbeds(
                    EmbedCreateSpec.create()
                        .withTitle(plugin.messages.discord.profileInfoTitle)
                        .withFields(
                            EmbedCreateFields.Field.of(
                                plugin.messages.discord.profileInfoPlayerNameHeader,
                                name,
                                false
                            )
                        )
                        .withThumbnail(plugin.avatarService.getAvatarUrl(playerId, name))
                        .withColor(Color.of(0x06d6a0))
                )
        ).awaitFirstOrNull()
    }

    fun createCommands(): List<ImmutableApplicationCommandRequest> {
        if (!plugin.configManager.linking.enabled) return listOf()

        val linkMinecraftCommand = createApplicationCommand {
            name(linkCommandName)
            description("Link Minecraft account to your Discord account")
            addOption {
                name("code")
                description("One-time code")
                type(ApplicationCommandOption.Type.STRING.value)
                required(true)
            }
        }

        val userInfoCommand = createApplicationCommand {
            type(2)
            name(profileInfoCommandName)
        }

        val executeCommand = createApplicationCommand {
            name(executeCommandName)
            description("Execute a Minecraft command")
            addOption {
                name("command")
                description("Minecraft command")
                type(ApplicationCommandOption.Type.STRING.value)
                required(true)
            }
        }

        return listOf(linkMinecraftCommand, userInfoCommand, executeCommand)
    }

}
