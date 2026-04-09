package me.olios.sharedVault.cache

import me.olios.sharedVault.vault.VaultState
import java.util.concurrent.ConcurrentHashMap

class VaultCache {
    private val cache = ConcurrentHashMap<String, VaultState>()

    fun getVault(id: String): VaultState? = cache[id]

    fun putVault(vault: VaultState) {
        cache[vault.id] = vault
    }

    fun removeVault(id: String) {
        cache.remove(id)
    }
}