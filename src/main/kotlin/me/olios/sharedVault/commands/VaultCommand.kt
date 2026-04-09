package me.olios.sharedVault.commands

import me.olios.sharedVault.gui.VaultGui
import me.olios.sharedVault.vault.VaultManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class VaultCommand(private val vaultManager: VaultManager): CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<out String>
    ): Boolean {
        if (sender !is Player) { // check for player
            sender.sendMessage("§cOnly players can use this command!")
            return true
        }

        if (!sender.hasPermission("sharedvault.use")) { // permission check
            sender.sendMessage("§cYou don't have permission to open the vault.")
            return true
        }

        val vaultId = if (p3.isNotEmpty()) p3[0] else "global" // determine vault ID (default to "global")
        val vaultSize = if (p3.size > 1) p3[1].toInt() else 54 // determine vault size (default 54)

        if (vaultSize !in 9..54 || vaultSize % 9 != 0) {
            sender.sendMessage("§cVault size must be 9, 18, 27, 36, 45, or 54.")
            return true
        }


        // get vault from manager
        val vaultState = vaultManager.getOrLoadVault(vaultId, vaultSize)

        // open the GUI
        val gui = VaultGui(vaultState)
        gui.open(sender)

        sender.sendMessage("§aOpening vault: §f$vaultId")
        return true
    }
}