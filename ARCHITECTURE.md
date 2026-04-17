# 🏗️ SharedVault: System Architecture & Design

## 1. Overview
SharedVault is a high-performance, distributed storage solution for Minecraft networks. It enables seamless item synchronization across multiple server instances using a layered storage approach. The architecture prioritizes **data integrity** and **resource efficiency**, ensuring that the network overhead remains low even with hundreds of active users.
In addition to real‑time synchronization, the system supports an unlimited number of independent vaults per network, allowing players and servers to operate multiple concurrent storage spaces without performance degradation.

---

## 2. The Storage Hierarchy
To balance speed and persistence, SharedVault employs a three-tier storage model:

| Layer | Type | Purpose |
| :--- | :--- | :--- |
| **L1: Local Cache** | ConcurrentMap | Immediate, zero-latency access for the local server thread. |
| **L2: Distributed Cache** | Redis (Lettuce) | The "Active Workspace." Handles cross-server synchronization. |
| **L3: Persistent Store** | MySQL (HikariCP) | The "Source of Truth." Long-term, ACID-compliant durability. |

---

## 3. Real-Time Synchronization Logic

### Pub/Sub Multiplexing
SharedVault utilizes **Redis Pub/Sub** for instant state propagation. To minimize overhead, the system uses a single channel (`vault_updates`) with a header-based protocol.
* **Payload Format**: `action:vaultId:metadata...` (e.g., `update_slot:global:14:102`)
* This multiplexing approach allows the plugin to handle updates, deletions, and administrative changes through a single network listener.

### Conflict Resolution (Optimistic Concurrency)
To prevent "Split-Brain" scenarios or item duplication during network lag, every vault state is versioned with an atomic integer.
* **The Rule**: Incoming updates are only applied if the `remoteVersion > localVersion`.
* If a conflict is detected, the local server discards its stale state and fetches the ground truth from Redis.

---

## 4. Efficiency & Performance Design

### Just-In-Time (JIT) Reconciliation
Unlike standard implementations that scan entire databases on startup (which scales poorly), SharedVault uses a **Lazy Reconciliation** model:
1. When a vault is requested, the system pulls from Redis.
2. In the background, it performs a targeted version comparison with MySQL.
3. If a discrepancy is found (e.g., Redis was wiped or MySQL didn't finish a save before a crash), the system self-heals by promoting the newer version and refreshing the active GUI for all viewers.

### Write-Behind Debouncing
To prevent database "choking" during rapid item movement (like player sorting), the plugin implements a **Save Debouncer**:
* Vaults are marked `isDirty` on change.
* A configurable countdown (e.g., 5 seconds) waits for the player to stop interacting.
* Only then is the full `VaultState` persisted to MySQL in a single asynchronous batch operation.

### Lazy ID Propagation
When a vault is created, only the **Vault ID** is broadcast to the network. The heavy `ItemStack` data remains in Redis/MySQL and is only loaded into a specific server's memory if a player on that instance actually opens the inventory.

---

## 5. Reliability & Fail-Fast Policy
Data integrity is the highest priority. SharedVault adopts a **Fail-Fast** approach:
* **Hard Dependencies**: Both Redis and MySQL must be reachable during the initialization phase.
* **Safety Lockdown**: If the primary storage layers are unreachable, the plugin disables itself to prevent the creation of "Ghost Items" (items that exist in memory but cannot be saved), protecting the player economy from corruption.

---

## 6. Technical Implementation Details
* **Language**: Kotlin (for null-safety and expressive syntax).
* **Redis Client**: **Lettuce** (Non-blocking, Netty-based asynchronous I/O).
* **Connection Pooling**: **HikariCP** for high-performance MySQL connection management.
* **Serialization**: Custom `ItemSerializer` using `ByteArrayOutputStream` + Base64, ensuring 100% preservation of complex NBT data, enchantments, and custom item metadata.
* **Thread Safety**: All Redis/MySQL I/O is handled on asynchronous threads, with a specific "Sync-Jump" back to the Bukkit Main Thread for all inventory and player manipulations.

---

## 7. Administrative Toolset
The `/sv` command system is fully integrated into the distributed layer:
* **Network-Wide Deletion**: Instantly clears memory and DB entries across the entire cluster.
* **Dynamic Resizing**: Modifies vault dimensions on the fly, triggering instant UI updates for all online viewers via the Pub/Sub layer.
