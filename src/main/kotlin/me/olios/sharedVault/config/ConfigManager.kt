package me.olios.sharedVault.config

import me.olios.sharedVault.SharedVault
import org.bukkit.configuration.file.FileConfiguration
import java.io.File

object ConfigManager {
    private lateinit var plugin: SharedVault
    private lateinit var mainConfig: FileConfiguration

    fun init(plugin: SharedVault) {
        this.plugin = plugin
        load()
    }

    fun load() {
        plugin.saveDefaultConfig()
        mainConfig = plugin.config
    }

    fun reload() {
        plugin.reloadConfig()
        mainConfig = plugin.config

        val messagesFile = File(plugin.dataFolder, "messages.yml")
    }

    // -----------------------------
    // Redis
    // -----------------------------
    val redisHost get() = mainConfig.getString("redis.host")!!
    val redisPort get() = mainConfig.getInt("redis.port")
    val redisPassword get() = mainConfig.getString("redis.password")!!

    // -----------------------------
    // Database
    // -----------------------------
    val dbHost get() = mainConfig.getString("database.host")!!
    val dbPort get() = mainConfig.getInt("database.port")
    val dbName get() = mainConfig.getString("database.name")!!
    val dbUser get() = mainConfig.getString("database.user")!!
    val dbPassword get() = mainConfig.getString("database.password")!!
    val dbPoolSize get() = mainConfig.getInt("database.pool-size").coerceIn(2, 30)
    val dbSaveDelayMs get() = mainConfig.getLong("database.save-delay-ms").coerceAtLeast(2000)

    // -----------------------------
    // Vault settings
    // -----------------------------
    val defaultName get() = mainConfig.getString("vault.default-name")!!
    val allowedSizes get() = mainConfig.getIntegerList("vault.allowed-sizes")
    val autosaveInterval get() = mainConfig.getInt("vault.autosave-interval")
    val lockTimeout get() = mainConfig.getLong("vault.lock-timeout")

    // -----------------------------
    // Sync settings
    // -----------------------------
    val syncEnabled get() = mainConfig.getBoolean("sync.enabled")
    val syncDelay get() = mainConfig.getLong("sync.update-delay")
    val loadOnJoin get() = mainConfig.getBoolean("sync.load-on-join")

    // -----------------------------
    // Debug
    // -----------------------------
    val debugRedis get() = mainConfig.getBoolean("debug.redis")
    val debugMysql get() = mainConfig.getBoolean("debug.mysql")
    val debugSync get() = mainConfig.getBoolean("debug.sync")
}