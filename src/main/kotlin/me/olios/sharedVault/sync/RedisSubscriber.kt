package me.olios.sharedVault.sync

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.vault.VaultManager
import me.olios.sharedVault.vault.VaultService
import org.bukkit.Bukkit

class RedisSubscriber(
    private val client: RedisClient,
    private val vaultService: VaultService,
    private val plugin: SharedVault
) {
    fun subscribe(): StatefulRedisPubSubConnection<String, String> {
        val pubSubConnection = client.connectPubSub()

        pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                if (channel != "vault_updates") return

                val parts = message.split(":")
                if (parts.isEmpty()) return

                when (parts[0]) {

                    "update_slot" -> {
                        if (parts.size == 4) {
                            val vaultId = parts[1]
                            val slot = parts[2].toInt()
                            val version = parts[3].toInt()
                            vaultService.handleExternalSlotUpdate(vaultId, slot, version)
                            // plugin.logger.info("Updated slot: $slot, version: $version")
                        }
                    }

                    "create_vault" -> {
                        if (parts.size == 2) {
                            val vaultId = parts[1]
                                vaultService.registerExistingId(vaultId)
                        }
                    }

                    "delete_vault" -> {
                        if (parts.size == 2) {
                            val vaultId = parts[1]
                            Bukkit.getScheduler().runTask(plugin, Runnable {
                                vaultService.handleExternalVaultDelete(vaultId)
                                // plugin.logger.info("Successfully handled deletion of $vaultId on main thread.")
                            })
                        }
                    }

                    "resize_vault" -> {
                        if (parts.size == 3) {
                            val vaultId = parts[1]
                            val newSize = parts[2].toInt()
                            // vaultService.handleExternalVaultResize(vaultId, newSize)
                        }
                    }

                    "rename_vault" -> {
                        if (parts.size == 3) {
                            val oldId = parts[1]
                            val newId = parts[2]
                            // vaultService.handleExternalVaultRename(oldId, newId)
                        }
                    }
                }
            }
        })
        pubSubConnection.async().subscribe("vault_updates")
        return pubSubConnection
    }


    fun unsubscribe(pubSubConnection: StatefulRedisPubSubConnection<String, String>) {
        pubSubConnection.async().unsubscribe("vault_updates")
        pubSubConnection.close()
    }

}