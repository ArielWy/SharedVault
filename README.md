# 📦 SharedVault

> A high-performance, distributed vault system for Minecraft networks with real-time cross-server synchronization.

SharedVault enables players across multiple servers to access and modify shared inventories seamlessly, with strong guarantees for data consistency, performance, and reliability.

---

# ✨ Features

## ⚡ Real-Time Cross-Server Sync

* Instant inventory updates across all servers using Redis Pub/Sub
* No polling, no delays, true real-time experience

## 🧠 Smart Conflict Resolution

* Version-based optimistic concurrency
* Prevents item duplication and race conditions

## 🗄️ Scalable Multi-Vault System

* Unlimited independent vaults
* Designed for large networks with many players

## 🚀 Performance Optimized

* Local memory cache for zero-latency access
* Redis as distributed runtime layer
* Write-behind debouncing to reduce database load

## 🔄 Self-Healing Data Model

* Automatic reconciliation between Redis and MySQL
* Recovers from crashes or partial writes

## 🛡️ Data Integrity First

* Fail-fast initialization (prevents corruption)
* No "ghost items"
* Strict consistency guarantees

## 🧩 Fully Configurable

* Vault size, title, behavior
* Save intervals and performance tuning

---

# 🎮 Commands

| Command                  | Description                              | Permission          |
| ------------------------ | ---------------------------------------- | ------------------- |
| `/vault <id>`            | Open a shared vault                      | `sharedvault.use`   |
| `/sv create <id>`        | Create a new vault                       | `sharedvault.admin` |
| `/sv delete <id>`        | Delete a vault (network-wide)            | `sharedvault.admin` |
| `/sv reload`             | Reload plugin config & reconnect systems | `sharedvault.admin` |

---

# 🔐 Permissions

| Permission           | Description                |
| -------------------- | -------------------------- |
| `sharedvault.use`    | Access vaults              |
| `sharedvault.admin`  | Full administrative access |
| `sharedvault.reload` | Reload plugin              |

---

# ⚙️ Configuration (`config.yml`)

```yaml
# ==========================================
# SharedVault - Main Configuration
# ==========================================

# -------------------------
# Redis (real-time sync)
# -------------------------
redis:
  host: "127.0.0.1"
  port: 6379
  password: ""        # leave empty if no password

# -------------------------
# MySQL (persistent storage)
# -------------------------
database:
  host: "127.0.0.1"
  port: 3306
  name: "sharedvault"
  user: "root"
  password: ""
  pool-size: 10
  save-delay-ms: 2000

# -------------------------
# Vault Settings
# -------------------------
vault:
  default-name: "global"
  allowed-sizes: [9, 18, 27, 36, 45, 54]
```

---

# 💬 Messages (`messages.yml`)

```yaml
# ============================
#  SharedVault - Messages
#  MiniMessage format
# ============================

general:
  only_players: "<red>Only players can use this command." # USED
  no_permission: "<red>You don't have permission to do that." # USED
  reloading: "<yellow>Reloading configuration..."
  reloaded: "<green>Configuration reloaded."

vault:
  open: "<green>Opening vault: <white>%VAULT%"  # USED
  not_found: "<red>Vault <white>%VAULT%<red> does not exist." # USED
  size_invalid: "<red>Vault size must be <white>9, 18, 27, 36, 45, or 54<red>." # USED

admin:
  # Reload Subcommand
  reload_config: "<green>Configuration files reloaded successfully!"
  reload_all: "<green>Plugin and database systems have been fully reloaded."

  # Create Subcommand
  create_usage: "<red>Usage: <yellow>/sv create <vaultId> [size]"
  already_exists: "<red>Error: A vault with the ID <white>%VAULT%</white> already exists!"
  invalid_size: "<red>Invalid size! Use a multiple of 9 (9, 18, 27, 36, 45, 54)."
  create_success: "<green>Successfully created vault <white>%VAULT%</white> with <yellow>%SIZE%</yellow> slots."
  create_error: "<red>An internal error occurred while creating the vault."

  # Delete Subcommand
  delete_usage: "<red>Usage: <yellow>/sv delete <vaultId>"
  delete_not_found: "<red>Error: Vault <white>%VAULT%</white> does not exist."
  delete_success: "<green>Vault <white>%VAULT%</white> has been deleted."
  delete_error: "<red>An internal error occurred while deleting the vault."

  # List Subcommand
  list_header: "<gray>--- [ <blue>Shared Vaults</blue> ] ---"
  list_item: "<click:run_command:'/sv info <VAULT>'><hover:show_text:'<gray>Click for details on %VAULT%'><white>• %VAULT%</white></hover></click>"
  list_empty: "<gray>No vaults have been created yet."

  # Info Subcommand
  info_usage: "<red>Usage: <yellow>/sv info <vaultId>"
  info_header: "<blue>Vault Info: <white><VAULT>"
  info_size: "<gray>Size: <yellow><SIZE> slots <dark_gray>(<ROWS> rows)"
  info_status: "<gray>Current Status: <status_color><STATUS>"

  # Edit Subcommand
  edit_usage: "<red>Usage: <yellow>/sv edit <id> <newId|size> <value>"
  edit_not_found: "<red>Error: Vault <white><VAULT></white> does not exist."
  edit_id_success: "<green>Vault <white><OLD_ID></white> renamed to <white><NEW_ID></white>."
  edit_size_success: "<green>Vault <white><VAULT></white> size updated to <yellow><SIZE></yellow>."
  edit_invalid_type: "<red>Invalid edit type. Use <white>id</white> or <white>size</white>."

```

---

# 🖼️ Example Usage

## Opening a Vault

![Vault Open Example]

## Real-Time Sync

![Example]

## Admin Delete

![Example]

---

# 🏗️ How It Works

SharedVault uses a **three-layer architecture**:

* **Local Cache** → instant access per server
* **Redis** → real-time distributed state
* **MySQL** → long-term persistence

Updates are:

1. Applied locally
2. Synced to Redis
3. Broadcast to all servers
4. Saved to MySQL asynchronously

---

## 📐 Architecture & Design

This project was built as part of a technical evaluation and focuses heavily on system design, scalability, and data integrity.

For a full breakdown of the architecture, including:

* Storage hierarchy (Cache → Redis → MySQL)
* Real-time synchronization model
* Conflict resolution strategy
* Performance optimizations (JIT reconciliation, debouncing)
* Reliability and fail-fast design

👉 See: [`ARCHITECTURE.md`](./ARCHITECTURE.md)

---

# 🧪 Requirements

* Java 25+
* Spigot / Paper (26.1.1)
* Redis server
* MySQL database

---

# 🚀 Installation

1. Install Redis & MySQL
2. Place plugin in `/plugins`
3. Configure `config.yml`
4. Start server

> Plugin will **disable itself** if dependencies are unavailable

---

# ⚠️ Important Notes

* Redis and MySQL **must be available at startup**
* Plugin uses a **fail-fast policy** to prevent data corruption
* Not intended for offline/local-only usage

---

# 🧠 Design Highlights

* Pub/Sub multiplexing for minimal network overhead
* Version-based consistency model
* Lazy reconciliation instead of full DB scans
* Debounced persistence to reduce load

---

# 🛠️ Developer Notes

* Written in Kotlin
* Async-first architecture
* Clean separation between:

  * Storage
  * Sync
  * Cache
  * GUI

---

## 📈 Future Improvements

- **Unlimited vault capacity with paginated navigation**, allowing vaults to grow beyond the current 54‑slot limit through seamless scrolling pages.  
- **Expanded administrative tooling** for managing, editing, migrating, and inspecting vaults directly from in‑game or console commands.  
- **Per‑player and per‑vault permission controls** to define granular access rules across the network.  
- **Comprehensive audit logging** to track changes, item movements, and version history for debugging or moderation.  

---

## License

This project is open source. See the LICENSE file for details.

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Credits

**Author**: olios

Built with ❤️ for the Minecraft community using:
- [Kotlin](https://kotlinlang.org/)
- [Paper API](https://papermc.io/)
- [Maven](https://maven.apache.org/)

---

**Last Updated**: 2026-17-04
