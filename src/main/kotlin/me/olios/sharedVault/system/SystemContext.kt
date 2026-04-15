package me.olios.sharedVault.system

import io.lettuce.core.RedisClient
import io.lettuce.core.event.connection.DisconnectedEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.resource.DefaultClientResources
import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.storage.MySqlStorage
import me.olios.sharedVault.storage.RedisStorage
import me.olios.sharedVault.sync.RedisPublisher
import me.olios.sharedVault.sync.RedisSubscriber
import me.olios.sharedVault.task.SaveDebouncer
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.Bukkit

class SystemContext(
    private val plugin: SharedVault
) {

    lateinit var redisClient: RedisClient
    lateinit var redisStorage: RedisStorage
    lateinit var redisPublisher: RedisPublisher
    lateinit var redisSubscriber: RedisSubscriber
    lateinit var subscribe: StatefulRedisPubSubConnection<String, String>

    lateinit var mySqlStorage: MySqlStorage
    lateinit var saveDebouncer: SaveDebouncer

    lateinit var vaultCache: VaultCache
    lateinit var vaultManager: VaultManager
    lateinit var vaultService: VaultService

    fun start() {

        // --- Redis ---
        val resources = DefaultClientResources.create()

        resources.eventBus().get().subscribe { event ->
            if (event is DisconnectedEvent) {
                redisStorage.handleFatalRedisError()
            }
        }

        val uri = "redis://${ConfigManager.redisHost}:${ConfigManager.redisPort}"
        redisClient = RedisClient.create(resources, uri)
        redisPublisher = RedisPublisher(redisClient)
        redisStorage = RedisStorage(redisClient, plugin)

        // --- Vault ---
        vaultCache = VaultCache
        vaultManager = VaultManager(vaultCache, redisStorage)

        // --- MySQL ---
        mySqlStorage = MySqlStorage(plugin)

        // --- Debouncer ---
        saveDebouncer = SaveDebouncer(
            plugin,
            vaultManager,
            mySqlStorage,
            ConfigManager.dbSaveDelayMs
        )

        // --- Service ---
        vaultService = VaultService(
            vaultManager,
            redisStorage,
            redisPublisher,
            saveDebouncer
        )

        // --- Subscriber ---
        redisSubscriber = RedisSubscriber(redisClient, vaultService)
        subscribe = redisSubscriber.subscribe()
    }

    fun stop() {
        plugin.logger.info("Initiating system shutdown...")

        // 1. Force Save
        try {
            plugin.logger.info("Forcing final save of all cached vaults...")
            saveDebouncer.forceSaveAll()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to complete final save: ${e.message}")
        }

        // 2. Close Inventories
        try {
            plugin.logger.info("Closing all active vault inventories...")
            vaultService.closeAllVaults()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to close all vault inventories: ${e.message}")
        }

        // 3. Unsubscribe Redis
        try {
            plugin.logger.info("Unsubscribing from Redis channels...")
            redisSubscriber.unsubscribe(subscribe)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to unsubscribe from Redis: ${e.message}")
        }

        // 4. Shutdown Redis Client
        try {
            plugin.logger.info("Shutting down Redis client...")
            redisClient.shutdown()
        } catch (e: Throwable) {
            // Using Throwable here to catch the NoClassDefFoundError we saw earlier
            plugin.logger.warning("Redis client shutdown encountered an error (likely ClassLoader cleanup): ${e.message}")
        }

        // 5. Close MySQL
        try {
            plugin.logger.info("Closing MySQL connection pool...")
            mySqlStorage.close()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to close MySqlStorage: ${e.message}")
        }

        plugin.logger.info("System shutdown sequence complete.")
    }
}