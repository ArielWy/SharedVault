package me.olios.sharedVault.vault

import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.storage.RedisStorage

class VaultManager(
    private val cache: VaultCache,
    private val redisStorage: RedisStorage,
) {

    fun getOrLoadVault(id: String, requestedSize: Int = 54): VaultState {
        val cached = cache.getVault(id)
        if (cached != null) return cached

        // Check Redis for existing metadata FIRST
        val existingSize = redisStorage.getStoredSize(id)
        val finalSize = existingSize ?: requestedSize

        val redisVault = redisStorage.load(id)
        if (redisVault != null) {
            cache.putVault(redisVault)
            return redisVault
        }

        val newVault = VaultState(id = id, size = finalSize)
        cache.putVault(newVault)
        return newVault
    }

    fun getVaultFromCache(id: String): VaultState? = cache.getVault(id)

    // used when MySQL/Redis loads data
    fun registerVault(vault: VaultState) {
        cache.putVault(vault)
    }
}