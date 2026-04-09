package me.olios.sharedVault.storage

import me.olios.sharedVault.vault.VaultState

interface StorageService {
    /**
     * Loads a vault state from the storage source.
     * Returns null if no data exists.
     */
    fun load(vaultId: String): VaultState?

    /**
     * Persists the entire vault state to the storage source.
     */
    fun save(vault: VaultState)
}