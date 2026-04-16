package me.olios.sharedVault.vault

import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.gui.VaultGui
import me.olios.sharedVault.storage.MySqlStorage
import me.olios.sharedVault.storage.RedisStorage
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture

class VaultManager(
    private val cache: VaultCache,
    private val redisStorage: RedisStorage,
    private val mySqlStorage: MySqlStorage,
    private val plugin: SharedVault
) {
    private val allVaultIds = mutableSetOf<String>()

    fun getVault(id: String, callback: (VaultState?) -> Unit) {
        // Check Local Cache
        val cached = cache.getVault(id)
        if (cached != null) {
            callback(cached)
            return
        }

        // Load from Redis
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val redisVault = redisStorage.load(id)

            if (redisVault != null) {
                // Open vault for player immediately on Main Thread
                Bukkit.getScheduler().runTask(plugin, Runnable { callback(redisVault) })

                // BACKGROUND SYNC: Check MySQL without making player wait
                performBackgroundSync(redisVault)
            } else {
                // Not in Redis? Try MySQL (The fallback)
                val mysqlVault = mySqlStorage.load(id)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (mysqlVault != null) {
                        cache.putVault(mysqlVault)
                        // Also put it in Redis for next time
                        redisStorage.save(mysqlVault)
                    }
                    callback(mysqlVault)
                })
            }
        })
    }

    fun getVaultFromCache(id: String): VaultState? = cache.getVault(id)

    fun getAllVaultsFromCache(): List<VaultState> = cache.getAll()

    // used when MySQL/Redis loads data
    fun registerVault(vault: VaultState) {
        cache.putVault(vault)
    }

    fun createVault(id: String, playerUUID: UUID, size: Int = 54): VaultState? {
        if (allVaultIds.contains(id)) return null

        val newVault = VaultState(
            id = id,
            size = size,
            lastUpdatedBy = playerUUID,
            lastUpdatedAt = System.currentTimeMillis()
        )
        cache.putVault(newVault)
        allVaultIds.add(id)
        redisStorage.save(newVault)
        return newVault
    }

    fun loadAllVaultIds(): CompletableFuture<Set<String>> {
        val future = CompletableFuture<Set<String>>()

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val ids = mutableSetOf<String>()

                // Fetch from MySQL (Synchronous call inside Async task)
                ids.addAll(mySqlStorage.getAllVaultIds())

                // Fetch from Redis (Synchronous call inside Async task)
                ids.addAll(redisStorage.getAllVaultIds())

                // Update local tracker
                synchronized(allVaultIds) {
                    allVaultIds.clear()
                    allVaultIds.addAll(ids)
                }

                // Complete the future
                future.complete(ids.toSet())

            } catch (e: Exception) {
                plugin.logger.severe("Failed to load vault IDs: ${e.message}")
                future.completeExceptionally(e)
            }
        })

        return future
    }

    fun getAllVaultIds(): Set<String> = allVaultIds.toSet()

    fun addVaultId(id: String) {allVaultIds.add(id)}

    private fun performBackgroundSync(redisVault: VaultState) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            // Just fetch the version number, not the whole data (very fast query)
            val mysqlVersion = mySqlStorage.loadVersion(redisVault.id)

            if (mysqlVersion == null) {
                // the vault wasn't saved in MySQL
                mySqlStorage.save(redisVault)
            } else if (mysqlVersion < redisVault.version) {
                // MySQL is outdated (happens after a crash) -> Update MySQL
                mySqlStorage.save(redisVault)
            } else if (mysqlVersion > redisVault.version) {
                // Redis is outdated (happens if Redis was wiped) -> Update Redis & Cache
                val freshVault = mySqlStorage.load(redisVault.id)
                if (freshVault != null) {
                    cache.putVault(freshVault)
                    redisStorage.save(freshVault)

                    // If player has vault open, refresh their UI
                    val gui = VaultGui(freshVault)
                    redisVault.viewers.forEach { uuid ->
                        val player = Bukkit.getPlayer(uuid)
                        if (player != null) {
                            gui.refreshVault(player)
                        }
                    }
                }
            }
        })
    }
}