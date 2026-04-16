package me.olios.sharedVault.commands.sharedvault.subcommands

import me.olios.sharedVault.SharedVault
import me.olios.sharedVault.commands.sharedvault.SubCommand
import me.olios.sharedVault.config.ConfigManager
import me.olios.sharedVault.config.MessagesConfig
import me.olios.sharedVault.storage.RedisStorage
import org.bukkit.command.CommandSender

class ReloadCommand(
    private val redisStorage: RedisStorage,
    private val plugin: SharedVault
): SubCommand {
    val config = ConfigManager
    val messages = MessagesConfig

    val CONFIG_SUBCOMMAND = "config"

    override fun execute(
        sender: CommandSender,
        args: Array<out String>
    ): Boolean {
        // Check if user specified 'config' or wants a full reload
        val isConfigOnly = args.isNotEmpty() && args[0].equals(CONFIG_SUBCOMMAND, ignoreCase = true)

        if (isConfigOnly) {
            // Logic for just configuration files
            config.reload()
            messages.reload(plugin)
            messages.send(sender, "admin.reload_config")
        } else {
            // Logic for entire plugin (Database, Vaults, Config)
            redisStorage.restart()
            plugin.reloadSystem()
            messages.send(sender, "admin.reload_all")
        }

        return true    }

    override fun tabComplete(
        sender: CommandSender,
        args: Array<out String>
    ): List<String> {
        if (args.size != 1) return emptyList()
        return listOf(CONFIG_SUBCOMMAND)
    }
}