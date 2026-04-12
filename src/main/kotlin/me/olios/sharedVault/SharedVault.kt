package me.olios.sharedVault

import io.lettuce.core.RedisClient
import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.commands.VaultCommand
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.gui.GuiListener
import me.olios.sharedVault.storage.RedisStorage
import me.olios.sharedVault.sync.RedisPublisher
import me.olios.sharedVault.sync.RedisSubscriber
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.plugin.java.JavaPlugin

class SharedVault : JavaPlugin() {

    private lateinit var redisClient: RedisClient
    private lateinit var redisStorage: RedisStorage
    private lateinit var redisPublisher: RedisPublisher
    private lateinit var redisSubscriber: RedisSubscriber

    private lateinit var vaultCache: VaultCache
    private lateinit var vaultManager: VaultManager
    private lateinit var vaultService: VaultService

    override fun onEnable() {
        MessagesConfig.init(this)
        ConfigManager.init(this)

        val uri = "redis://${ConfigManager.redisHost}:${ConfigManager.redisPort}"
        redisClient = RedisClient.create(uri)

        redisPublisher = RedisPublisher(redisClient)
        redisStorage = RedisStorage(redisClient)

        vaultCache = VaultCache
        vaultManager = VaultManager(vaultCache, redisStorage)

        vaultService = VaultService(vaultManager, redisStorage, redisPublisher)

        redisSubscriber = RedisSubscriber(redisClient, vaultService)
        redisSubscriber.subscribe()

        registerCommands()
        registerListeners()

        logger.info("SharedVault connected to Redis and Sync services started!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        getCommand("vault")?.setExecutor(VaultCommand(vaultManager))
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(GuiListener(vaultService, vaultManager), this)
    }
}
