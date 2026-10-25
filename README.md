# ⚡ craft-repl

[![Clojure](https://img.shields.io/badge/Clojure-1.12-5881d8.svg?logo=clojure&logoColor=white)](https://clojure.org)
[![Java](https://img.shields.io/badge/Java-21%2B%20%7C%2025-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Fabric](https://img.shields.io/badge/Fabric-1.21.1%2B-dbb98f.svg)](https://fabricmc.net)
[![nREPL](https://img.shields.io/badge/nREPL-1.3-43b02a.svg)](https://nrepl.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Zero-Install nREPL & Clojure Runtime Injector for Live Minecraft / Fabric Environments.**  
> *Transform any running Minecraft instance into an interactive Lisp Machine in real-time — with zero downtime, no static mod installations, and no server restarts.*

---

## 💡 The Vision

Modding and automating Minecraft has traditionally followed an archaic batch cycle:  
`Write Java/Kotlin code` $\rightarrow$ `Compile JAR` $\rightarrow$ `Drop into .minecraft/mods/` $\rightarrow$ `Restart the entire game` $\rightarrow$ `Wait 1 minute` $\rightarrow$ `Test` $\rightarrow$ `Catch an exception` $\rightarrow$ `Restart again`.

`craft-repl` eliminates this boundary completely. 

Using the **Java Attach API** and **dynamic bytecode/ClassLoader transposition**, it connects to any active Minecraft process (client or server, Vanilla or Fabric 1.21.1+ on modern Java 21/25) directly from the OS, injects the Clojure runtime into the engine's isolated `KnotClassLoader`, and boots an **nREPL server** inside the living game loop.

Your editor becomes a command bridge into a persistent simulated universe. Blocks, chunks, entities, and inventories become inspectable, immutable EDN data structures.

---

## 🏛️ Architecture

```text
┌─────────────────────────────────┐
│  Editor / IDE (Calva / CIDER)   │   (VS Code, Emacs, Neovim)
└────────────────┬────────────────┘
                 │ nREPL Protocol (TCP 7888)
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ HotSpot JVM (Minecraft PID in execution)                    │
│                                                             │
│   ┌──────────────────────────────────────────────────────┐  │
│   │ AppClassLoader (System ClassLoader)                  │  │
│   │ * craft-repl-agent.jar loaded via VirtualMachine     │  │
│   └──────────────────────────┬───────────────────────────┘  │
│                              │ Context Inversion Bridge     │
│                              ▼                              │
│   ┌──────────────────────────────────────────────────────┐  │
│   │ KnotClassLoader (Fabric) / Server Context            │  │
│   │ * clojure.lang.RT & Compiler (Clojure 1.12)          │  │
│   │ * nrepl.server/start-server (:port 7888)             │  │
│   │ * Namespace craft.core & in-server-thread macro      │  │
│   │ * Pure EDN World & Player Abstractions               │  │
│   └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Why Standard Java Agents Fail in Modern Minecraft
In modern Fabric-based environments, game classes and mod code do not live in the `AppClassLoader` (where standard Java Attach agents land). Instead, they run in a strict, isolated ClassLoader named `KnotClassLoader`. 

If an agent attempts `Class.forName("net.minecraft.server.MinecraftServer")`, the JVM throws an immediate `ClassNotFoundException`. `craft-repl` bypasses this by discovering active runtime references via `Instrumentation.getAllLoadedClasses()` and anchoring its Clojure compiler directly within the `KnotClassLoader` context.

---

## 🚀 Quickstart

### Prerequisites
* **OS**: Linux (tested on Arch Linux / Debian / Ubuntu) or macOS.
* **JDK**: OpenJDK 21 or 25.
* **Clojure CLI** (`clj`) and/or **Babashka** (`bb`).
* A running Minecraft instance (client or dedicated server, Fabric 1.21.1+).

### 1. Compile & Attach in One Command

If you have **Babashka** installed:
```bash
./scripts/attach.clj
```
*The script automatically inspects `jps -v`, identifies your Minecraft process PID, builds the lightweight native agent, and injects the nREPL server listening on `127.0.0.1:7888`.*

Or manually with **Make**:
```bash
# 1. Find the PID
jps -v | grep net.fabricmc

# 2. Attach specifying the PID and port
make attach PID=195362 PORT=7888
```

### 2. Connect Your Favorite Editor

* **VS Code (Calva)**:
  1. Open command palette (`Ctrl+Shift+P` / `Cmd+Shift+P`).
  2. Select `Calva: Connect to a running REPL in your project`.
  3. Choose `Generic`, host `localhost`, port `7888`.
* **Emacs (CIDER)**:
  `M-x cider-connect-clj` $\rightarrow$ `localhost` $\rightarrow$ `7888`.
* **Neovim (Conjure)**:
  Open any `.clj` file; Conjure auto-detects `localhost:7888`.
* **Terminal CLI**:
  ```bash
  make repl PORT=7888
  ```

---

## 🎮 Interactive Live Coding Examples

Once connected, you evaluate code directly inside the living game state:

### 1. Inspect Players as Pure Data (EDN)
```clojure
(ns user
  (:require [craft.core :refer [in-server-thread]]
            [craft.player :as p]
            [craft.world :as w]))

;; Read real-time player telemetry:
(p/player-coords "Pace")
;; => {:name "Pace", 
;;     :x -26.54, :y 68.0, :z -48.21, 
;;     :yaw -90.2, :pitch 12.4, 
;;     :dimension "minecraft:overworld"}
```

### 2. Thread-Safe Teleportation & Experience
```clojure
;; Teleport player safely inside the ServerThread tick loop:
(p/teleport! "Lizzie_Melo" -26.0 68.0 -48.0)

;; Grant Level 30 XP directly:
(p/add-xp! "Pace" 585)
```

### 3. Inspect & Manipulate Blocks
```clojure
;; Check the block below the player's feet:
(w/get-block-at [-26 67 -48])
;; => {:pos [-26 67 -48], :state "Block{minecraft:deepslate_tile_wall}"}
```

### 4. Algorithmic World Generation (Lisp Machine Style)
Spawn mathematical geometries live into the world:

```clojure
(defn spiral-staircase! [[cx cy cz] height radius]
  (in-server-thread
    (let [world (w/get-overworld)]
      (doseq [h (range height)]
        (let [angle (* h 0.35)
              x (+ cx (* radius (Math/cos angle)))
              z (+ cz (* radius (Math/sin angle)))
              pos (net.minecraft.util.math.BlockPos. (int x) (+ cy h) (int z))]
          (.setBlockState world pos (.getDefaultState net.minecraft.block.Blocks/OBSIDIAN) 3))))))

;; Evaluate: a 30-block obsidian spiral helix manifests instantly in the sky:
(spiral-staircase! [-26 70 -48] 30 5.0)
```

---

## 🛡️ The Golden Rule: `in-server-thread`

Minecraft is strictly **not thread-safe**. It implements a single-threaded event loop (`ServerThread`). Mutating blocks, entities, or inventories outside of this event loop causes `ConcurrentModificationException` and chunk data corruption (`.mca` files).

`craft-repl` provides the `in-server-thread` macro, ensuring that state-mutating expressions are scheduled atomically onto the game's authoritative event queue:

```clojure
(in-server-thread
  ;; Safely executed within the engine's tick loop
  (.setBlockState world pos state 3))
```

---

## 🧩 Project Structure

```text
craft-repl/
├── Makefile                     # Build, jar packaging, attach, and REPL targets
├── README.md                    # Project documentation
├── deps.edn                     # Clojure 1.12, nREPL, core.async, malli
├── mise.toml                    # Toolchain pinning (Java 25, Clojure 1.12)
├── scripts/
│   └── attach.clj               # Babashka PID auto-detector & injector script
├── src/
│   ├── clj/
│   │   └── craft/
│   │       ├── core.clj         # Server discovery & in-server-thread macro
│   │       ├── player.clj       # Player telemetry, XP & safe teleportation
│   │       └── world.clj        # Block inspection & world state manipulation
│   └── java/
│       └── com/craft/agent/
│           ├── CraftAgent.java  # Dynamic Java Attach Agent (Knot bridge)
│           └── Injector.java    # CLI harness for com.sun.tools.attach
└── test/
    └── craft/
        └── core_test.clj        # Unit tests
```

---

## 🗺️ Roadmap

- [x] Lightweight Java Attach Agent with dynamic `KnotClassLoader` transposition.
- [x] In-process `nrepl.server` bootstrapping on configurable ports.
- [x] Basic abstractions for `ServerWorld`, `ServerPlayerEntity`, and coordinates.
- [ ] Reactive event bus over `core.async` (capturing tick signals, combat damage, and chat messages).
- [ ] Declarative **Malli** schemas for *fail-closed* command validation.
- [ ] Full inventory serialization to immutable **EDN** files for atomic backup/restoration.
- [ ] Bitemporal telemetry integration with **XTDB** (querying historical world state).
- [ ] Local AI integration via **`small-router`** / LLM tool calling for autonomous in-game agents.

---

## 📚 Technical Deep-Dive

For a complete architectural analysis detailing how this was born during a live recovery incident (including bytecode ASM injection, Guava cache invalidation, and Cloudflare WAF workarounds), read the engineering report:
* [Hackeando o Runtime do Minecraft ao Vivo: Hotpatching de JVM, ASM e Manipulação de Estado sem Reiniciar o Servidor](https://patrickcmserrano.github.io/blog/2026-10-25-hackeando-runtime-jvm-minecraft-hotpatch-asm/)

---

## 📄 License

MIT License. Copyright (c) 2026 Patrick Serrano.
