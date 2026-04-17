package me.olios.sharedVault.commands.sharedvault.subcommands

import me.olios.sharedVault.commands.sharedvault.SubCommand
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.command.CommandSender
import kotlin.text.startsWith

class DeleteCommand(
    val vaultManager: VaultManager,
    val vaultService: VaultService
): SubCommand {
    private val messages = MessagesConfig
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        // Validation: /sv delete <vaultId>
        if (args.isEmpty()) {
            messages.send(sender, "admin.delete_usage")
            return true
        }

        val vaultId = args[0]

        // Check existence
        if (!vaultManager.getAllVaultIds().contains(vaultId)) {
            messages.send(sender, "admin.delete_not_found", mapOf("VAULT" to vaultId))
            return true
        }

        val vaultState = vaultManager.getVault(vaultId) { vaultState ->
            if (vaultState == null) {
                messages.send(sender, "vault.not_found", mapOf("VAULT" to vaultId))
                return@getVault
            }

            // close all opened vault
            vaultService.closeVault(vaultState)

            // Execute deletion
            vaultManager.deleteVault(vaultId) { success ->
                if (success) {
                    messages.send(sender, "admin.delete_success", mapOf("VAULT" to vaultId))
                } else {
                    messages.send(sender, "admin.delete_error")
                }
            }
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        args: Array<out String>
    ): List<String> {
        if (args.isEmpty()) return emptyList()
        if (args.size != 1) return emptyList()
        val vaultsId = vaultManager.getAllVaultIds()

        return vaultsId.filter { it.startsWith(args[0], ignoreCase = true) }
            .toMutableList()    }
}