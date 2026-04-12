package me.olios.sharedVault.vault

import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.gui.VaultGui
import me.olios.sharedVault.gui.VaultHolder
import me.olios.sharedVault.storage.RedisStorage
import me.olios.sharedVault.sync.RedisPublisher
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class VaultService(
    private val manager: VaultManager,
    private val redisStorage: RedisStorage,
    private val redisPublisher: RedisPublisher) {

    /**
     * Used when an update comes from ANOTHER server via Redis.
     * don't mark it dirty because it's already saved elsewhere.
     */
    fun handleExternalSlotUpdate(vaultId: String, slot: Int, remoteVersion: Int) {
        val vault = manager.getVaultFromCache(vaultId) ?: return

        // only update if the remote version is newer
        if (remoteVersion <= vault.version) return

        // fetch only the specific slot from Redis
        val newItem = redisStorage.loadSingleSlot(vaultId, slot)

        // Update local state
        vault.items[slot] = newItem
        vault.version = remoteVersion

        // Refresh GUI for all active viewers on THIS server
        refreshViewers(vault, slot, newItem)
    }

    /**
     * Called by the GuiListener whenever a slot is modified.
     */
    fun handleSlotUpdate(vaultId: String, slot: Int, newItem: ItemStack?, updater: UUID) {
        val vault = manager.getOrLoadVault(vaultId)

        // Only update if the item actually changed
        if (vault.items[slot] == newItem) return

        vault.items[slot] = newItem
        vault.version++
        vault.lastUpdatedBy = updater
        vault.lastUpdatedAt = System.currentTimeMillis()
        vault.isDirty = true

        // Debug log
        // println("Vault ${vault.id} updated slot $slot. New Version: ${vault.version}")

        // push to redis storage
        redisStorage.updateSlot(vaultId, slot, newItem, vault.version, updater)

        // notify other servers via Pub/Sub
        redisPublisher.publishSlotUpdate(vaultId, slot, vault.version)

        // FUTURE: saveDebouncer.schedule(vaultId)
    }

    private fun refreshViewers(vault: VaultState, slot: Int, newItem: ItemStack?) {
        val gui = VaultGui(vault) // Create a temporary view controller

        vault.viewers.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                gui.refreshSlot(player, slot, newItem)
            }
        }
    }
}