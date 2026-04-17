package me.olios.sharedVault.vault

import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.gui.VaultGui
import me.olios.sharedVault.storage.MySqlStorage
import me.olios.sharedVault.storage.RedisStorage
import me.olios.sharedVault.sync.RedisPublisher
import me.olios.sharedVault.task.SaveDebouncer
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.util.UUID

class VaultService(
    private val manager: VaultManager,
    private val redisStorage: RedisStorage,
    private val redisPublisher: RedisPublisher,
    private val saveDebouncer: SaveDebouncer,
    private val plugin: SharedVault
    ) {

    /**
     * Used when an update comes from ANOTHER server via Redis.
     */
    fun handleExternalSlotUpdate(vaultId: String, slot: Int, remoteVersion: Int) {
        val vault = manager.getVaultFromCache(vaultId) ?: return

        // plugin.logger.info("remoteVersion: $remoteVersion, vaultVersion: ${vault.version}")

        // only update if the remote version is newer
        if (remoteVersion <= vault.version) return

        // plugin.logger.info("EXTERNAL | Updated slot: $slot, remoteVersion: $remoteVersion")

        // fetch only the specific slot from Redis
        val newItem = redisStorage.loadSingleSlot(vaultId, slot)

        // Update local state
        vault.items[slot] = newItem
        vault.version = remoteVersion

        // Refresh GUI for all active viewers on THIS server
        refreshViewers(vault, slot, newItem)
    }

    fun handleExternalVaultDelete(vaultId: String) {
        closeVault(manager.getVaultFromCache(vaultId) ?: return)
        manager.removeVaultFromCache(vaultId)
    }

    /**
     * Called by the GuiListener whenever a slot is modified.
     */
    fun handleSlotUpdate(vaultId: String, slot: Int, newItem: ItemStack?, updater: UUID) {
        val vault: VaultState = manager.getVaultFromCache(vaultId) ?: return

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

        // run debouncer to save DB
        saveDebouncer.scheduleSave(vaultId)
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

    fun registerExistingId(vaultId: String) {
        manager.addVaultId(vaultId)
    }

    fun closeAllVaults() {
        for (vault in manager.getAllVaultsFromCache()) {
            val gui = VaultGui(vault) // Create a temporary view controller
            val viewersCopy = vault.viewers.toList() // avoid concurrent modification

            for (uuid in viewersCopy) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                // Close the inventory
                gui.close(player)
            }

            vault.viewers.clear()
        }
    }

    fun closeVault(vault: VaultState) {
        val gui = VaultGui(vault) // Create a temporary view controller
        val viewersCopy = vault.viewers.toList() // avoid concurrent modification

        for (uuid in viewersCopy) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            // Close the inventory
            gui.close(player)
        }
    }
}