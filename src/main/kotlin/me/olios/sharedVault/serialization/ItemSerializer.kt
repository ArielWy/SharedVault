package me.olios.sharedVault.serialization

import com.google.gson.Gson
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object ItemSerializer {

    /**
     * Converts an ItemStack to a Base64 String.
     */
    fun toBase64(item: ItemStack?): String {
        if (item == null || item.type.isAir) return ""

        // Paper's newest method to get a compact byte array representation
        val bytes = item.serializeAsBytes()

        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Converts a Base64 String back into an ItemStack.
     */
    fun fromBase64(data: String?): ItemStack? {
        if (data.isNullOrEmpty()) return null

        val bytes = Base64.getDecoder().decode(data)
        return ItemStack.deserializeBytes(bytes)
    }

    /**
     * Converts an entire array of items to a single Base64 string.
     * allow saving entier vault at once in Redis/MySQL.
     */
    fun itemStackArrayToBase64(items: Array<ItemStack?>): String {
        val out = ByteArrayOutputStream()
        val dataOut = java.io.DataOutputStream(out)

        dataOut.writeInt(items.size)

        for (item in items) {
            if (item == null || item.type.isAir) {
                dataOut.writeInt(-1) // mark null/air
            } else {
                val bytes = item.serializeAsBytes()
                dataOut.writeInt(bytes.size)
                dataOut.write(bytes)
            }
        }

        dataOut.flush()
        return Base64.getEncoder().encodeToString(out.toByteArray())
    }

    /**
     * Reconstructs an array of items from a single Base64 string.
     */
    fun itemStackArrayFromBase64(data: String): Array<ItemStack?> {
        val bytes = Base64.getDecoder().decode(data)
        val input = ByteArrayInputStream(bytes)
        val dataIn = java.io.DataInputStream(input)

        val size = dataIn.readInt()
        val items = arrayOfNulls<ItemStack>(size)

        for (i in 0 until size) {
            val len = dataIn.readInt()
            if (len == -1) {
                items[i] = null
            } else {
                val itemBytes = ByteArray(len)
                dataIn.readFully(itemBytes)
                items[i] = ItemStack.deserializeBytes(itemBytes)
            }
        }

        return items
    }
}