package me.olios.sharedVault

import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.commands.VaultCommand
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.plugin.java.JavaPlugin

class SharedVault : JavaPlugin() {

    private val vaultCache = VaultCache()
    private val vaultManager = VaultManager(vaultCache)
    private val vaultService = VaultService(vaultManager)

    override fun onEnable() {
        registerCommands()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        getCommand("vault")?.setExecutor(VaultCommand(vaultManager))
    }
}
