package com.dominikkorsa.discordintegration.listener

import com.dominikkorsa.discordintegration.DiscordIntegration
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class DeathListener(private val plugin: DiscordIntegration) : Listener {
    @EventHandler
    suspend fun onDeath(event: PlayerDeathEvent) {
        var deathMessage = event.deathMessage?.let {
            plugin.configManager.discordDeathMessage
                .replace("%death-message%", it)
        } ?: plugin.configManager.discordDeathFallbackMessage
        deathMessage = deathMessage
            .replace("%player%", event.entity.displayName)
        plugin.client.sendDeathInfo(deathMessage)
    }
}
