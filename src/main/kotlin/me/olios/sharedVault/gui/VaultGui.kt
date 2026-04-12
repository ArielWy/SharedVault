package me.olios.sharedVault.gui

import me.olios.sharedVault.vault.VaultState
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class VaultGui(private val state: VaultState) {

    fun createInventory(): Inventory {
        val inv = Bukkit.createInventory(VaultHolder(state.id), state.size, "Vault: ${state.id}")
        inv.contents = state.items
        return inv
    }

    fun open(player: Player) {
        player.openInventory(createInventory())
    }

    fun refresh(player: Player) {
        val topInventory = player.openInventory.topInventory
        val holder = topInventory.holder

        // Verify the player is actually looking at a vault
        if (holder is VaultHolder && holder.vaultId == state.id) {
            topInventory.contents = state.items
        }
    }

    fun refreshSlot(player: Player, slot: Int, item: ItemStack?) {
        val topInventory = player.openInventory.topInventory
        val holder = topInventory.holder as? VaultHolder ?: return

        if (holder.vaultId == state.id) {
            topInventory.setItem(slot, item)
        }
    }
}