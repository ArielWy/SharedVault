package me.olios.sharedVault

import io.lettuce.core.RedisClient
import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.commands.VaultCommand
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.gui.GuiListener
import me.olios.sharedVault.storage.MySqlStorage
import me.olios.sharedVault.storage.RedisStorage
import me.olios.sharedVault.sync.RedisPublisher
import me.olios.sharedVault.sync.RedisSubscriber
import me.olios.sharedVault.system.SystemContext
import me.olios.sharedVault.task.SaveDebouncer
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.plugin.java.JavaPlugin

class SharedVault : JavaPlugin() {

    private var system: SystemContext? = null

    override fun onEnable() {
        reloadSystem()
    }

    override fun onDisable() {
        system?.stop()
    }

    private fun registerCommands() {
        val sys = system ?: return
        getCommand("vault")?.setExecutor(VaultCommand(sys.vaultManager))
    }

    private fun registerListeners() {
        val sys = system ?: return
        server.pluginManager.registerEvents(
            GuiListener(sys.vaultService, sys.vaultManager),
            this
        )
    }

    fun reloadSystem() {
        system?.stop()

        MessagesConfig.init(this)
        ConfigManager.init(this)

        val newSystem = SystemContext(this)

        try {
            newSystem.start()
        } catch (ex: Exception) {
            logger.severe("Failed to start system: ${ex.message}")
            ex.printStackTrace()

            // Disable plugin AND STOP EXECUTION
            server.pluginManager.disablePlugin(this)
            return  // <<< CRITICAL
        }

        system = newSystem

        registerCommands()
        registerListeners()

        logger.info("SharedVault connected to Redis and MySQL and Sync services started!")
    }
}
