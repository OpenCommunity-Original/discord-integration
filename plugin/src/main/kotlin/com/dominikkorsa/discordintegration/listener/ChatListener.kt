package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import com.dominikkorsa.discordintegration.config.ConfigManager.Debug.CancelledChatEventsMode.ALL
import com.dominikkorsa.discordintegration.config.ConfigManager.Debug.CancelledChatEventsMode.AUTO
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val plugin: DiscordIntegration) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    suspend fun onPlayerChat(event: AsyncChatEvent) {
        plugin.webhooks.sendChatMessage(event.player, LegacyComponentSerializer.legacyAmpersand().serialize(event.message()))
    }
}
