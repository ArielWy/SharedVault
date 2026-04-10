package me.olios.sharedVault

import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.commands.VaultCommand
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.gui.GuiListener
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.plugin.java.JavaPlugin

class SharedVault : JavaPlugin() {

    private val vaultCache = VaultCache()
    private val vaultManager = VaultManager(vaultCache)
    private val vaultService = VaultService(vaultManager)

    override fun onEnable() {
        MessagesConfig.init(this)

        registerCommands()
        registerListeners()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        getCommand("vault")?.setExecutor(VaultCommand(vaultManager))
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(GuiListener(vaultService), this)
    }
}
