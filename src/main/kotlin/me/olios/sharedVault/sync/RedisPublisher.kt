package me.olios.sharedVault.sync

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

class RedisPublisher(
    private val client: RedisClient
) {
    /**
     * Publishes a message to notify other servers of a single slot update.
     */
    fun publishSlotUpdate(vaultId: String, slot: Int, version: Int) {
        val payload = "$vaultId:$slot:$version" // Simple colon-separated format
        client.connect().async().publish("vault_updates", payload)
    }
}