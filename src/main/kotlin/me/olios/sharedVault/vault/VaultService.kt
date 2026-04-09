package me.olios.sharedVault.vault

import org.bukkit.inventory.ItemStack
import java.util.UUID

class VaultService(private val manager: VaultManager) {

    /**
     * The primary way to modify a vault.
     * Handles versioning and dirty flags automatically.
     */
    fun updateSlot(vaultId: String, slot: Int, newItem: ItemStack?, updater: UUID) {
        val vault = manager.getOrLoadVault(vaultId)

        vault.items[slot] = newItem
        vault.version++
        vault.lastUpdatedBy = updater
        vault.lastUpdatedAt = System.currentTimeMillis()
        vault.isDirty = true

        // TODO: triggerSync() -> Redis Publisher
        // TODO: scheduleSave() -> MySQL Debouncer
    }

    /**
     * Used when an update comes from ANOTHER server via Redis.
     * We don't mark it dirty because it's already saved elsewhere.
     */
    fun handleExternalUpdate(updatedVault: VaultState) {
        manager.registerVault(updatedVault.apply { isDirty = false })
        // TODO: Refresh open GUIs for players
    }
}