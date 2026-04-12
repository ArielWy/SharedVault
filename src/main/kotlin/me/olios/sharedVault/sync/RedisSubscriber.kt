package me.olios.sharedVault.sync

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import me.olios.sharedVault.vault.VaultService

class RedisSubscriber(
    private val client: RedisClient,
    private val vaultService: VaultService
) {
    fun subscribe() {
        val pubSubConnection = client.connectPubSub()
        pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                if (channel == "vault_updates") {
                    val parts = message.split(":")
                    if (parts.size == 3) {
                        val vaultId = parts[0]
                        val slot = parts[1].toInt()
                        val version = parts[2].toInt()

                        // Tell the service to fetch only this slot from Redis
                        vaultService.handleExternalSlotUpdate(vaultId, slot, version)
                    }
                }
            }
        })
        pubSubConnection.async().subscribe("vault_updates")
    }
}