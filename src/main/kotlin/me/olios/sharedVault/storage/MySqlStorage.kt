package me.olios.sharedVault.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.olios.sharedVault.cache.VaultCache
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.serialization.ItemSerializer
import me.olios.sharedVault.vault.VaultState
import java.util.UUID

class MySqlStorage() : StorageService {

    private val dataSource: HikariDataSource
    private val config: ConfigManager = ConfigManager

    init {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${config.dbHost}:${config.dbPort}/${config.dbName}"
            username = config.dbUser
            password = config.dbPassword
            maximumPoolSize = config.dbPoolSize
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useSSL", "false")

        }
        dataSource = HikariDataSource(hikariConfig)
        createTables()
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
    }

    override fun load(vaultId: String): VaultState? {
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
        return null
    }
}