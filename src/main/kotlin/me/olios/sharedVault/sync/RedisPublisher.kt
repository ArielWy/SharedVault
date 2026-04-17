package me.olios.sharedVault.sync

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

class RedisPublisher(
    private val client: RedisClient
) {
    private val connection = client.connect()
    private val async = connection.async()
    /**
     * Publishes a message to notify other servers of a single slot update.
     */
    fun publishSlotUpdate(vaultId: String, slot: Int, version: Int) {
        val payload = "update_slot:$vaultId:$slot:$version"
        async.publish("vault_updates", payload)
    }

    fun publishDelete(vaultId: String) {
        val payload = "delete_vault:$vaultId"
        async.publish("vault_updates", payload)
    }

    fun publishCreate(vaultId: String) {
        val payload = "create_vault:$vaultId"
        async.publish("vault_updates", payload)
    }
}