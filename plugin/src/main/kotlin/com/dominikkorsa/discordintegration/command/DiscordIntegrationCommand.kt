package com.dominikkorsa.discordintegration.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.compatibility.Compatibility
import com.github.shynixn.mccoroutine.bukkit.launch
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("discordintegration|di|discord")
class DiscordIntegrationCommand(val plugin: DiscordIntegration): BaseCommand() {
    @Default
    fun onDefault() {
        val discordUrl = plugin.configManager.chat.discordUrl
        val discordTitle = plugin.messages.discord.discordJoinTitle
        currentCommandIssuer.sendMessage("\n${ChatColor.GREEN}${ChatColor.BOLD} $discordTitle $discordUrl\n ")
    }
    @Subcommand("reload")
    @CommandPermission("discordintegration.command.reload")
    fun onReload() {
        plugin.launch { plugin.reload() }
    }

    @Subcommand("link")
    fun onLink(sender: Player) {
        if (!plugin.configManager.linking.enabled) {
            sender.sendMessage(plugin.messages.commands.linkDisabled)
            return
        }
        val linkingCode = plugin.linking.generateLinkingCode(sender)
        val parts = plugin.minecraftFormatter.formatLinkCommandMessage(linkingCode.code).toTypedArray()
        Compatibility.sendSystemMessage(sender, *parts)
    }

    @Subcommand("unlink")
    fun onUnlink(sender: Player) {
        plugin.launch {
            sender.sendMessage(
                when (plugin.linking.unlink(sender)) {
                    true -> plugin.messages.commands.unlinkSuccess
                    false -> plugin.messages.commands.alreadyUnlinked
                }
            )
        }
    }

    @Subcommand("help")
    fun onHelp(sender: CommandSender) {
        sender.sendMessage(plugin.minecraftFormatter.formatHelpHeader())
        sendHelpCommandItem(sender, "/di help", "help")
        sendHelpCommandItem(sender, "/di reload", "reload")
        sendHelpCommandItem(sender, "/di link", "link")
        sendHelpCommandItem(sender, "/di unlink", "unlink")
    }

    @CatchUnknown
    fun onUnknown(sender: CommandSender) {
        sender.sendMessage(plugin.messages.commands.unknown)
    }

    private fun sendHelpCommandItem(
        sender: CommandSender,
        command: String,
        code: String
    ) {
        sender.sendMessage(plugin.minecraftFormatter.formatHelpCommand(command, code))
    }
}
