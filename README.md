# ⚡ craft-repl

> **Zero-Install nREPL & Clojure Runtime Injector for Live Minecraft / Fabric Environments.**

`craft-repl` transforma qualquer instância viva de Minecraft (Fabric ou Vanilla) em uma **Lisp Machine**: um ambiente de desenvolvimento interativo em tempo de execução conectado diretamente ao seu editor (VS Code/Calva, Emacs/CIDER, Neovim/Conjure) via **nREPL**, sem exigir modificações prévias no disco, sem alterar a pasta `mods/` e sem reiniciar o jogo.

---

## 🎯 Por Que Existe?

Normalmente, o desenvolvimento e automação de Minecraft exigem compilar `.jar`s estáticos, colocá-los na pasta de mods e reiniciar o jogo a cada alteração. 

O `craft-repl` usa a **Java Attach API** para perfurar as barreiras de isolamento de `ClassLoaders` (`KnotClassLoader`) em tempo de execução, acoplando um runtime Clojure completo e um servidor nREPL no processo em execução.

```text
┌───────────────────────┐
│  Seu Editor / IDE     │  (Calva / CIDER / Neovim Conjure)
│  (Emacs / VS Code)    │
└───────────┬───────────┘
            │  nREPL Protocol (TCP porta 7888)
            ▼
┌────────────────────────────────────────────────────────┐
│ Minecraft JVM (PID ativo)                              │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ AppClassLoader (Bootstrap do Agente)             │  │
│  │  * craft-repl-agent.jar via Java Attach API      │  │
│  └──────────────────────────┬───────────────────────┘  │
│                             │ Inversão Contextual      │
│                             ▼                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ KnotClassLoader (Fabric)                         │  │
│  │  * Clojure 1.12 + nREPL Server (porta 7888)      │  │
│  │  * Macro (in-server-thread & body)               │  │
│  │  * Manipulação de mundo e jogador como EDN puro  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 🚀 Início Rápido

### 1. Pré-requisitos
* Linux (Arch, Debian, etc.)
* Java 21+ ou OpenJDK 25
* Clojure CLI (`clj`) e/ou Babashka (`bb`)
* Um cliente ou servidor de Minecraft rodando

### 2. Compilar e Injetar com um Comando

Se tiver **Babashka** instalado:
```bash
./scripts/attach.clj
```
O script descobre automaticamente o PID do processo Minecraft aberto e realiza o acoplamento na porta padrão `7888`.

Ou manualmente via **Make**:
```bash
# Descubra o PID:
jps -v | grep net.fabricmc

# Injete o agente:
make attach PID=195362 PORT=7888
```

### 3. Conectar seu Editor ao nREPL

* **VS Code (Calva)**: `Ctrl+Shift+P` $\rightarrow$ `Calva: Connect to a running REPL in your project` $\rightarrow$ `Generic` $\rightarrow$ `localhost:7888`.
* **Emacs (CIDER)**: `M-x cider-connect-clj` $\rightarrow$ `localhost` $\rightarrow$ `7888`.
* **Terminal CLI**:
```bash
make repl PORT=7888
```

---

## 🎮 Exemplo de Uso Interativo

```clojure
(ns user
  (:require [craft.core :refer [in-server-thread]]
            [craft.player :as p]
            [craft.world :as w]))

;; 1. Inspecionar as coordenadas de um jogador (retorna mapa EDN puro):
(p/player-coords "Pace")
;; => {:name "Pace", :x -26.5, :y 68.0, :z -48.2, :dimension "minecraft:overworld"}

;; 2. Teleporte seguro (despachado atômica e thread-safe no ServerThread):
(p/teleport! "Lizzie_Melo" -26.0 68.0 -48.0)

;; 3. Adicionar XP:
(p/add-xp! "Pace" 585)

;; 4. Consultar o bloco onde o jogador está pisando:
(w/get-block-at [-26 67 -48])
;; => {:pos [-26 67 -48], :state "Block{minecraft:stone}"}
```

---

## 🛡️ A Regra de Ouro: `in-server-thread`

O Minecraft opera sob uma variante estrita de **Single-Threaded Event Loop**. Qualquer mutação de blocos, inventários ou entidades disparada fora da thread `ServerThread` causará `ConcurrentModificationException` ou corromperá chunks.

O `craft-repl` encapsula essa barreira com a macro:
```clojure
(in-server-thread
  ;; Código mutável executado com segurança dentro do tick loop
  ...)
```

---

## 🗺️ Roadmap

- [x] Agente de injeção Attach API com transposição para `KnotClassLoader`.
- [x] Inicializador do `nrepl.server` na JVM hospedeira.
- [x] Abstrações básicas de servidor, mundo e jogador.
- [ ] Barramento de eventos reativos em canais `core.async` (ticks, danos e chat).
- [ ] Schemas declarativos em **Malli** para validação de comandos *fail-closed*.
- [ ] Integração com **XTDB** para telemetria bitemporal de exploração e inventários.
- [ ] Conexão com LLM local via gateway **`small-router`** para agentes in-game.

---

## 📄 Licença

MIT License. Desenvolvido por Patrick Serrano & Antigravity.
