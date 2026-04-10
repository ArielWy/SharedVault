package me.olios.sharedVault.config

import me.olios.sharedVault.SharedVault
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object MessagesConfig {
    private lateinit var messages: YamlConfiguration
    private val mm = MiniMessage.miniMessage()

    fun init(plugin: SharedVault) {
        val dataFolder = plugin.dataFolder
        val file = File(dataFolder, "messages.yml")
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messages = YamlConfiguration.loadConfiguration(file)
    }

    fun get(path: String, placeholders: Map<String, String> = emptyMap()): Component {
        val messagePath = "messages.$path"
        val raw = messages.getString(messagePath) ?: "<red>Missing message: $messagePath"
        val replaced = applyPlaceholders(raw, placeholders)
        return mm.deserialize(replaced)
    }

    fun send(sender: CommandSender, path: String, placeholders: Map<String, String> = emptyMap()) {
        sender.sendMessage(get(path, placeholders))
    }

    fun broadcast(path: String, placeholders: Map<String, String> = emptyMap()) {
        val msg = get(path, placeholders)
        Bukkit.getOnlinePlayers().forEach { it.sendMessage(msg) }
    }

    private fun applyPlaceholders(msg: String, placeholders: Map<String, String>): String {
        var result = msg
        placeholders.forEach { (key, value) ->
            result = result.replace("%$key%", value)
        }
        return result
    }
}