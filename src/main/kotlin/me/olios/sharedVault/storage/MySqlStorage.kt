package me.olios.sharedVault.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.serialization.ItemSerializer
import me.olios.sharedVault.vault.VaultState
import org.bukkit.Bukkit
import java.sql.SQLException
import java.util.UUID

class MySqlStorage(
    private val plugin: SharedVault
) : StorageService {

    private val dataSource: HikariDataSource
    private val config: ConfigManager = ConfigManager

    init {
        try {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${config.dbHost}:${config.dbPort}/${config.dbName}"
                username = config.dbUser
                password = config.dbPassword
                maximumPoolSize = config.dbPoolSize

                // Forces Hikari to fail immediately if the connection is wrong
                initializationFailTimeout = 1
                // Prevents waiting 30 seconds for a connection
                connectionTimeout = 5000

                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("useSSL", "false")
            }

            this.dataSource = HikariDataSource(hikariConfig)
            createTables()

        } catch (e: Exception) {
            // Clean message for admins
            plugin.logger.severe("--------------------------------------------------")
            plugin.logger.severe("[SharedVault] DATABASE CONNECTION FAILED")
            plugin.logger.severe("Could not connect to MySQL at ${config.dbHost}:${config.dbPort}")
            plugin.logger.severe("Please check your credentials and ensure the DB is running.")
            plugin.logger.severe("The plugin will now be disabled.")
            plugin.logger.severe("--------------------------------------------------")

            // Shut down the plugin
            Bukkit.getPluginManager().disablePlugin(plugin)

            // Re-throw a simple exception to prevent 'dataSource' from being accessed
            throw RuntimeException("SQL initialization failed.")
        }
    }

    private fun createTables() {
        dataSource.connection.use { conn ->
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS shared_vaults (
                    vault_id VARCHAR(64) PRIMARY KEY,
                    size INT NOT NULL,
                    version INT NOT NULL,
                    items_data LONGTEXT NOT NULL,
                    last_updated_at BIGINT NOT NULL,
                    last_updated_by VARCHAR(36)
                )
            """.trimIndent())
        }
    }

    override fun save(vault: VaultState) {
        // save the WHOLE array as one big Base64 string in MySQL
        // because MySQL is for long-term persistence, not slot-by-slot updates.
        val serializedItems = ItemSerializer.itemStackArrayToBase64(vault.items)

        try {
            dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement("""
                INSERT INTO shared_vaults (vault_id, size, version, items_data, last_updated_at, last_updated_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                size = VALUES(size), version = VALUES(version), 
                items_data = VALUES(items_data), last_updated_at = VALUES(last_updated_at),
                last_updated_by = VALUES(last_updated_by)
            """)
                stmt.setString(1, vault.id)
                stmt.setInt(2, vault.size)
                stmt.setInt(3, vault.version)
                stmt.setString(4, serializedItems)
                stmt.setLong(5, vault.lastUpdatedAt)
                stmt.setString(6, vault.lastUpdatedBy?.toString())
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            // log error
            if (plugin.isEnabled) {
                plugin.logger.severe("--------------------------------------------------")
                plugin.logger.severe("[SharedVault] FATAL ERROR: SQL Connection Lost!")
                plugin.logger.severe("The plugin cannot save data and will be disabled.")
                plugin.logger.severe("--------------------------------------------------")

                // close the datasource immediately
                close()

                // Disable the plugin
                disable()
            }
            throw e
        }
    }

    override fun load(vaultId: String): VaultState? {
        try {
            dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement("SELECT * FROM shared_vaults WHERE vault_id = ?")
                stmt.setString(1, vaultId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    val size = rs.getInt("size")
                    val items = ItemSerializer.itemStackArrayFromBase64(rs.getString("items_data"))

                    return VaultState(
                        id = vaultId,
                        size = size,
                        items = items,
                        version = rs.getInt("version"),
                        lastUpdatedAt = rs.getLong("last_updated_at"),
                        lastUpdatedBy = rs.getString("last_updated_by")?.let(UUID::fromString),
                        isDirty = false
                    )
                }
            }
        } catch (e: SQLException) {
            // log error
            if (plugin.isEnabled) {
                plugin.logger.severe("--------------------------------------------------")
                plugin.logger.severe("[SharedVault] FATAL ERROR: SQL Connection Lost!")
                plugin.logger.severe("Could not load vault data for: $vaultId")
                plugin.logger.severe("The plugin will now be disabled to prevent data corruption.")
                plugin.logger.severe("--------------------------------------------------")

                // Kill the Hikari Pool immediately
                close()

                // Disable the plugin
                disable()
            }
            throw e
        }

        // If we catch an error or find nothing, we return null
        return null
    }

    fun close() {
        dataSource.close()
    }

    private fun disable() {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getPluginManager().disablePlugin(plugin)
        })
    }
}
