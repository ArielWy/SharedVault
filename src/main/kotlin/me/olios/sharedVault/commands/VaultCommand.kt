package me.olios.sharedVault.commands

import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.gui.VaultGui
import me.olios.sharedVault.serialization.ItemSerializer
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.storage.MySqlStorage
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.text.startsWith

class VaultCommand(
    private val vaultManager: VaultManager,
    private val mySqlStorage: MySqlStorage
): CommandExecutor, TabCompleter {

    private val messages = MessagesConfig
    override fun onCommand(
        sender: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<out String>
    ): Boolean {
        if (sender !is Player) { // check for player
            messages.send(sender, "general.only_players")
            return true
        }

        if (!sender.hasPermission("sharedvault.use")) { // permission check
            messages.send(sender, "general.no_permission")
            return true
        }

        val vaultId = if (p3.isNotEmpty()) p3[0] else ConfigManager.defaultName // determine vault ID (default to "global")
        val vaultSize : Int = if (p3.size > 1) p3[1].toIntOrNull() ?: 54 else 54 // determine vault size (default 54)

        if (vaultSize !in 9..54 || vaultSize % 9 != 0) {
            messages.send(sender, "vault.size_invalid")
            return true
        }

        // testing serializer
        // if (handleVaultTest(sender, vaultId, vaultSize)) return true

        // get vault from manager
        val vaultState = vaultManager.getVault(vaultId) { vaultState ->
            if (vaultState == null) {
                messages.send(sender, "vault.not_found", mapOf("VAULT" to vaultId))
                return@getVault
            }
            // open gui
            val gui = VaultGui(vaultState)
            gui.open(sender)

            messages.send(sender, "vault.open", mapOf("VAULT" to vaultId))
        }
        return true
    }


    // debug serialization
    private var testItemStorage: String = ""

    fun handleVaultTest(sender: Player, vaultId: String, vaultSize: Int): Boolean {
        if (vaultId != "item") return false

        if (vaultSize == 9) {
            val item = sender.inventory.itemInMainHand

            // Save to the class-level variable, not a local one
            testItemStorage = ItemSerializer.toBase64(item)

            sender.sendMessage("§aItem serialized and saved to memory!")
            println("Serialized: $testItemStorage")
        }
        else if (vaultSize == 18) {
            if (testItemStorage.isEmpty()) {
                sender.sendMessage("§cNothing is saved in memory yet! Use size 9 first.")
                return true
            }

            val item = ItemSerializer.fromBase64(testItemStorage) ?: ItemStack(Material.AIR)
            sender.inventory.addItem(item)
            sender.sendMessage("§aItem deserialized and given!")
        }
        return true
    }

    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        args: Array<out String>
    ): List<String?>? {
        if (args.isEmpty()) return emptyList()
        if (args.size != 1) return emptyList()
        val vaultsId = vaultManager.getAllVaultIds()

        return vaultsId.filter { it.startsWith(args[0], ignoreCase = true) }
            .toMutableList()
    }

}