package me.olios.sharedVault.commands.sharedvault

object SubCommandManager {
    private val commands: MutableMap<String, SubCommand> = mutableMapOf()

    fun registerCommand(name: String, command: SubCommand) {
        commands[name] = command
    }

    fun getCommand(name: String): SubCommand? {
        return commands[name.lowercase()]
    }

    fun getAllCommands(): Set<String> {
        return commands.keys
    }
}