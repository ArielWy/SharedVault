package me.olios.sharedVault.sync

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import me.olios.sharedVault.vault.VaultService

class RedisSubscriber(
    private val client: RedisClient,
    private val vaultService: VaultService
) {
    fun subscribe(): StatefulRedisPubSubConnection<String, String> {
        val pubSubConnection = client.connectPubSub()
        pubSubConnection.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                if (channel == "vault_updates") {
                    val parts = message.split(":")
                    if (parts.size == 3) {
                        val vaultId = parts[0]
                        val slot = parts[1].toInt()
                        val version = parts[2].toInt()

                        vaultService.handleExternalSlotUpdate(vaultId, slot, version)
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