package me.olios.sharedVault.vault

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class VaultState(
    val id: String,
    val size: Int = 54, // Default to a double chest if not specified
    var version: Int = 0,
    var lastUpdatedBy: UUID? = null,
    var lastUpdatedAt: Long = System.currentTimeMillis(),
    var isDirty: Boolean = false,
    val items: Array<ItemStack?> = arrayOfNulls(size) // Initialize the array based on the size passed above
) {
    fun cloneItems(): Array<ItemStack?> = items.copyOf()

    // don't accidentally serialize/compare the whole array in every log
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultState) return false
        if (id != other.id) return false
        if (size != other.size) return false
        return items.contentEquals(other.items)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + size
        result = 31 * result + items.contentHashCode()
        return result
    }
}