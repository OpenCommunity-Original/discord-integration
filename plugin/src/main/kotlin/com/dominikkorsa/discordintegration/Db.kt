package com.dominikkorsa.discordintegration

import com.charleskorn.kaml.Yaml
import discord4j.common.util.Snowflake
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import java.io.File
import java.util.*

class Db(private val plugin: DiscordIntegration) {

    private object SnowflakeSerializer : KSerializer<Snowflake> {
        override val descriptor = PrimitiveSerialDescriptor("Snowflake", PrimitiveKind.LONG)
        override fun deserialize(decoder: Decoder): Snowflake = Snowflake.of(decoder.decodeLong())
        override fun serialize(encoder: Encoder, value: Snowflake) {
            encoder.encodeLong(value.asLong())
        }
    }

    private object UUIDSerializer : KSerializer<UUID> {
        override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: UUID) {
            encoder.encodeString(value.toString())
        }
    }

    @Serializable
    data class Player(
        @Serializable(with = SnowflakeSerializer::class)
        val discordId: Snowflake?,
    ) {
        fun withDiscordId(discordId: Snowflake?) = Player(discordId)
    }

    private val playerMapSerializer = MapSerializer(UUIDSerializer, Player.serializer())

    private var players = HashMap<UUID, Player>()
    private val file = File(plugin.dataFolder, "players.yml")

    private suspend fun save() {
        withContext(IO) {
            Yaml.default.encodeToStream(playerMapSerializer, players, file.outputStream())
        }
    }

    private suspend fun load() {
        withContext(IO) {
            if (file.exists()) {
                val text = file.readText()
                players = if (text.trim().isEmpty()) HashMap()
                else HashMap(Yaml.default.decodeFromString(playerMapSerializer, text))
            } else save()
        }
    }

    suspend fun reload() {
        load()
        buildIndexes()
    }

    suspend fun init() {
        load()
        buildIndexes()
    }

    private var playerOfMember = HashMap<Snowflake, UUID>()

    private fun nameFromUUID(uniqueId: UUID) = Bukkit.getOfflinePlayer(uniqueId).name

    private fun buildIndexes() {
        playerOfMember.clear()
        players.forEach {
            it.value.discordId?.let { discordId ->
                playerOfMember.putIfAbsent(discordId, it.key)?.let { existing ->
                    players[it.key] = it.value.withDiscordId(null)
                    plugin.logger.warning("Conflict while loading players database")
                    plugin.logger.warning(
                        "Discord account with ID $discordId is already linked to player ${
                            nameFromUUID(existing) ?: existing.toString()
                        }, unlinking ${nameFromUUID(it.key) ?: it.key.toString()}"
                    )
                }
            }
        }
    }

    private fun getOrCreatePlayer(playerId: UUID) = players.getOrPut(playerId) { Player(null) }

    fun getDiscordId(playerId: UUID) = players[playerId]?.discordId

    fun playerIdOfMember(discordId: Snowflake) = playerOfMember[discordId]

    suspend fun resetDiscordId(playerId: UUID): Snowflake? {
        val previousPlayer = getOrCreatePlayer(playerId)
        if (previousPlayer.discordId == null) return null
        playerOfMember.remove(previousPlayer.discordId)
        players[playerId] = previousPlayer.withDiscordId(null)
        save()
        return previousPlayer.discordId
    }

    suspend fun setDiscordId(playerId: UUID, discordId: Snowflake): Pair<UUID?, Snowflake?>? {
        val previouslyLinkedPlayerId = playerOfMember.put(discordId, playerId)
        if (previouslyLinkedPlayerId == playerId) return null

        val playerBefore = getOrCreatePlayer(playerId)
        playerBefore.discordId?.let(playerOfMember::remove)
        players[playerId] = playerBefore.withDiscordId(discordId)
        save()
        return Pair(previouslyLinkedPlayerId, playerBefore.discordId)
    }
}
