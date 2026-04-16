package me.olios.sharedVault.gui

import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.InventoryHolder

class VaultHolder(val vaultId: String) : InventoryHolder {
    override fun getInventory() = null!! // allow it to now if it's a sharedvault.
}

class GuiListener(
    private val vaultService: VaultService,
    private val manager: VaultManager
): Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? VaultHolder ?: return
        val player = event.whoClicked as Player

        // check the slot AFTER the click has been processed
        org.bukkit.Bukkit.getScheduler().runTask(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(
                this::class.java), Runnable {
            val vaultId = holder.vaultId

            // Sync all slots that could have changed
            // For a click, we check the clicked slot and the cursor
            for (slot in 0 until event.inventory.size) {
                val item = event.inventory.getItem(slot)
                vaultService.handleSlotUpdate(vaultId, slot, item, player.uniqueId)
            }
        })
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder as? VaultHolder ?: return
        val player = event.whoClicked as Player

        // check the slot AFTER the click has been processed
        org.bukkit.Bukkit.getScheduler().runTask(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(
                this::class.java), Runnable {
            for (slot in event.rawSlots) {
                if (slot < event.inventory.size) {
                    val item = event.inventory.getItem(slot)
                    vaultService.handleSlotUpdate(holder.vaultId, slot, item, player.uniqueId)
                }
            }
        })
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val holder = event.inventory.holder as? VaultHolder ?: return
        val vault = manager.getVaultFromCache(holder.vaultId)
        vault?.viewers?.add(event.player.uniqueId)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? VaultHolder ?: return
        val vault = manager.getVaultFromCache(holder.vaultId)
        vault?.viewers?.remove(event.player.uniqueId)
    }
}