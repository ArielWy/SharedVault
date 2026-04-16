package me.olios.sharedVault.commands.sharedvault.subcommands

import me.olios.sharedVault.commands.sharedvault.SubCommand
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultState
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class CreateCommand(private val vaultManager: VaultManager): SubCommand {
    private val config = ConfigManager
    private val messages = MessagesConfig
    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        // Check for minimum arguments: /sv create <vaultId>
        if (args.isEmpty()) {
            messages.send(sender, "admin.create_usage")
            return true
        }

        val vaultId = args[0]

        // Check if vault already exists
        if (vaultManager.getAllVaultIds().contains(vaultId)) {
            messages.send(sender, "admin.already_exists", mapOf("VAULT" to vaultId))
            return true
        }

        //  Determine size (Default 54)
        val size = if (args.size >= 2) {
            val inputSize = args[1].toIntOrNull()
            if (inputSize == null || inputSize !in 9..54 || inputSize % 9 != 0) {
                messages.send(sender, "admin.invalid_size")
                return true
            }
            inputSize
        } else {
            54
        }

        // Create the vault
        val uuid: UUID = if (sender is Player) sender.uniqueId else UUID.nameUUIDFromBytes(vaultId.toByteArray())
        val success = vaultManager.createVault(vaultId, uuid, size)

        if (success is VaultState) {
            messages.send(sender, "admin.create_success", mapOf(
                "VAULT" to vaultId,
                "SIZE" to size.toString()
            ))
        } else {
            messages.send(sender, "admin.create_error")
        }

        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        args: Array<out String>
    ): List<String> {
        if (args.isEmpty()) return emptyList()
        if (args.size == 2) return config.allowedSizes.map { it.toString() }
        return emptyList()
    }
}