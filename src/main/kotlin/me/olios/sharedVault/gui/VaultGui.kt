package me.olios.sharedVault.gui

import me.olios.sharedVault.vault.VaultState
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class VaultGui(private val state: VaultState) {

    fun createInventory(): Inventory {
        val inv = Bukkit.createInventory(VaultHolder(state.id), state.size, "Vault: ${state.id}")
        inv.contents = state.items
        return inv
    }

    fun open(player: Player) {
        player.openInventory(createInventory())
    }
}