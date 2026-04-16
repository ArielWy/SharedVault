package me.olios.sharedVault.storage

import io.lettuce.core.ClientListArgs.Builder.ids
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.serialization.ItemSerializer
import me.olios.sharedVault.vault.VaultState
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.util.UUID

class RedisStorage(
    private val client: RedisClient,
    private val plugin: SharedVault
) : StorageService {

    private val KEY_PREFIX = "sharedvault:vault:"
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val async = connection.async()

    private fun key(vaultId: String) = "$KEY_PREFIX$vaultId"

    override fun load(vaultId: String): VaultState? {
        val data = async.hgetall(key(vaultId)).get()
        if (data.isEmpty()) return null

        val size = data["size"]?.toInt() ?: 54
        val version = data["version"]?.toInt() ?: 0
        val lastUpdatedBy = data["lastUpdatedBy"]?.takeIf { it.isNotBlank() }?.let(UUID::fromString)
        val lastUpdatedAt = data["lastUpdatedAt"]?.toLong() ?: System.currentTimeMillis()

        val items = arrayOfNulls<ItemStack>(size)

        for (slot in 0 until size) {
            val base64 = data["slot:$slot"]
            items[slot] = ItemSerializer.fromBase64(base64)
        }

        return VaultState(
            id = vaultId,
            size = size,
            items = items,
            version = version,
            lastUpdatedBy = lastUpdatedBy,
            lastUpdatedAt = lastUpdatedAt,
            isDirty = false
        )
    }

    override fun save(vault: VaultState) {
        val redisKey = key(vault.id)

        val map = mutableMapOf<String, String>(
            "version" to vault.version.toString(),
            "size" to vault.size.toString(),
            "lastUpdatedAt" to vault.lastUpdatedAt.toString(),
            "lastUpdatedBy" to (vault.lastUpdatedBy?.toString() ?: "")
        )

        vault.items.forEachIndexed { index, item ->
            val encoded = ItemSerializer.toBase64(item)
            if (encoded != "") {
                map["slot:$index"] = encoded
            } else {
                async.hdel(redisKey, "slot:$index")
            }
        }

        async.hset(redisKey, map)
    }

    /**
     * Update only a single slot + metadata.
     * main operation for real-time cross-server sync.
     */
    fun updateSlot(
        vaultId: String,
        slot: Int,
        item: ItemStack?,
        version: Int,
        lastUpdatedBy: UUID?
    ) {
        val redisKey = key(vaultId)

        val encoded = ItemSerializer.toBase64(item)

        if (encoded != "") {
            async.hset(redisKey, "slot:$slot", encoded)
        } else {
            async.hdel(redisKey, "slot:$slot")
        }

        async.hset(redisKey, "version", version.toString())
        async.hset(redisKey, "lastUpdatedBy", lastUpdatedBy?.toString() ?: "")
        async.hset(redisKey, "lastUpdatedAt", System.currentTimeMillis().toString())
    }

    fun loadSingleSlot(vaultId: String, slot: Int): ItemStack? {
        val base64 = connection.sync().hget(key(vaultId), "slot:$slot")
        return ItemSerializer.fromBase64(base64)
    }

    fun getStoredSize(vaultId: String): Int? {
        val data = async.hgetall(key(vaultId)).get()
        val sizeStr = data["size"] ?: return null
        return sizeStr.toIntOrNull()
    }

    private var isHandlingFailure = false // Class-level variable

    fun handleFatalRedisError() {
        // Synchronized ensures only one thread can enter this block at a time
        synchronized(this) {
            if (isHandlingFailure || !plugin.isEnabled) return
            isHandlingFailure = true
        }

        plugin.logger.severe("--------------------------------------------------")
        plugin.logger.severe("[SharedVault] FATAL ERROR: Redis Connection Lost!")
        plugin.logger.severe("The plugin requires Redis for synchronization.")
        plugin.logger.severe("The plugin will now be disabled.")
        plugin.logger.severe("--------------------------------------------------")

        // Jump to Main Thread to disable
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getPluginManager().disablePlugin(plugin)
        })
    }

    fun getAllVaultIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val sync = connection.sync()
        var cursor = ScanCursor.INITIAL

        val pattern = "$KEY_PREFIX*"

        do {
            // scan for all keys
            val scan = sync.scan(cursor, ScanArgs.Builder.matches(pattern))
            cursor = scan

            for (key in scan.keys) {
                ids.add(key.removePrefix(KEY_PREFIX))
            }
        } while (!cursor.isFinished) // isFinished checks if cursor is "0"

        return ids
    }
}