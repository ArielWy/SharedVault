package me.olios.sharedVault.vault

import me.olios.sharedVault.cache.VaultCache

class VaultManager(private val cache: VaultCache) {

    fun getOrLoadVault(id: String, size: Int = 54): VaultState {
        //  Check Cache
        val cached = cache.getVault(id)
        if (cached != null) return cached

        // if not in cache: now - create a fresh one (load from DB later)
        val newVault = VaultState(id = id, size = size)
        cache.putVault(newVault)
        return newVault
    }

    // used later when MySQL/Redis loads data
    fun registerVault(vault: VaultState) {
        cache.putVault(vault)
    }
}