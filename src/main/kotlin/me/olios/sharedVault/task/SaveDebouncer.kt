package me.olios.sharedVault.task

import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.storage.StorageService
import me.olios.sharedVault.vault.VaultManager
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

class SaveDebouncer(
    private val plugin: SharedVault,
    private val vaultManager: VaultManager,
    private val mysqlStorage: StorageService,
    private val delayTicks: Long // e.g., 100 ticks = 5 seconds
) {
    // Keeps track of active "pending" saves
    private val pendingSaves = ConcurrentHashMap<String, BukkitTask>()

    fun scheduleSave(vaultId: String) {
        // Cancel existing task for this vault if it exists (reset the timer)
        pendingSaves.remove(vaultId)?.cancel()

        // schedule a new async task
        val task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            executeSave(vaultId)
        }, delayTicks)

        pendingSaves[vaultId] = task
    }

    private fun executeSave(vaultId: String) {
        val vault = vaultManager.getVaultFromCache(vaultId) ?: return

        // Double-check if it's still dirty
        if (!vault.isDirty) return

        try {
            mysqlStorage.save(vault)
            vault.isDirty = false // Mark as clean after successful MySQL save
            pendingSaves.remove(vaultId)

            plugin.logger.info("Successfully persisted vault $vaultId to MySQL.")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save vault $vaultId to MySQL: ${e.message}")
            // Optional: Reschedule a retry
        }
    }

    /**
     * call onDisable
     */
    fun forceSaveAll() {
        pendingSaves.values.forEach { it.cancel() }
        pendingSaves.clear()

        // For each vault in cache, if dirty, save synchronously (since server is stopping)
        // vaultManager.getAllCachedVaults().filter { it.isDirty }.forEach { mysqlStorage.save(it) }
    }
}