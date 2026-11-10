# Consumindo o `craft-repl`: injetando Clojure na JVM viva

O `craft-repl` não é só um REPL para humano. Ele é uma **biblioteca de
injeção**: o `CraftAgent` põe um runtime Clojure dentro do `KnotClassLoader` de
uma JVM Minecraft viva e sobe um nREPL. Qualquer ferramenta — um agente, um
serviço, um script — pode se conectar nesse canal e **avaliar código dentro do
jogo**, sem reiniciar nada.

Este documento é para quem vai **consumir** esse canal, não só digitar nele.

> **Um consumidor real já existe:** o [`craft-agents`](https://github.com/patrickcmserrano/craft-agents)
> sobe duas portas nREPL próprias reaproveitando esta mesma receita. O
> `craft-agents` **não importa** o jar nem a classe daqui — ele **replica o
> mecanismo**, porque a barreira que ele precisa atravessar é a mesma.

---

## 1. O que o `craft-repl` entrega, e o que fica com o consumidor

| Peça | Quem faz |
| :--- | :--- |
| Achar o processo e anexar via **Java Attach API** | **`craft-repl`** (`make attach`) |
| Atravessar o **`KnotClassLoader`** (registrar os jars, carregar o `clojure.lang.RT` dentro dele) | **`craft-repl`** (`CraftAgent`) |
| Subir o **nREPL** e o `src/clj` dele (os `craft.core`, `in-server-thread`) | **`craft-repl`** (porta `7888`) |
| **O seu código** dentro do jogo | **você** |
| O **seu** classpath e o **seu** `src/clj` no mesmo loader | **você** |

A linha de corte: o `craft-repl` garante que **existe** um Clojure vivo na JVM.
O que **esse** Clojure enxerga é responsabilidade do consumidor.

---

## 2. A receita do `KnotClassLoader` (o que é preciso replicar)

Se o seu consumidor sobe a **própria** porta (em vez de se conectar na `7888`),
ele precisa repetir os três passos que o `CraftAgent.java` executa. Nenhum é
opcional — cada um foi medido.

### 2.1 Encontrar o loader e fixar o contexto

```java
ClassLoader knot = null;
for (Thread t : Thread.getAllStackTraces().keySet()) {
    ClassLoader cl = t.getContextClassLoader();
    if (cl != null && cl.getClass().getName().contains("Knot")) { knot = cl; break; }
}
Thread.currentThread().setContextClassLoader(knot);
```

**Por que importa:** o `clojure.lang.RT` só existe dentro do Knot. Carregar pelo
loader do Attach Listener (System) morre com `ClassNotFoundException:
clojure.lang.RT`.

### 2.2 Registrar o classpath — e usar `appendToSystemClassLoaderSearch`

```java
inst.appendToSystemClassLoaderSearch(new JarFile(jar));  // ← OBRIGATÓRIO
vpcs.add(jar.toPath());                                   // validParentCodeSources
addUrlFwd.invoke(knot, jar.toURI().toURL());              // registro de URL
```

**A armadilha medida (2026-11-10):** o `addUrlFwd` do Knot **registra o URL sem
torná-lo buscável** pelo `loadClass`. Medido: 30 dependências "registradas" e o
`Class.forName("clojure.lang.RT", true, knot)` respondia **`ClassNotFoundException`**.
O `appendToSystemClassLoaderSearch` é o que realmente alimenta a busca.

**E o `Class.forName` precisa do loader explícito:**

```java
Class<?> rt = Class.forName("clojure.lang.RT", true, knot);   // knot, não o default
```

### 2.3 Fixar o contexto **antes de criar o servidor**

```java
Thread.currentThread().setContextClassLoader(knot);   // de novo, aqui
// ... só então montar e carregar o script que sobe o nREPL
```

**A armadilha medida:** o nREPL cria as threads de handler num
`ThreadPoolExecutor`, e **elas herdam o `contextClassLoader` da thread que cria o
servidor**. Se a thread criadora não tem o Knot no contexto, os handlers nascem
sem acesso ao Clojure e o envio da resposta morre com
`ClassNotFoundException: clojure.lang.RT` (observado em `print.clj:94`) — o
servidor escuta, aceita conexão e **nunca responde**.

---

## 3. Falando com o nREPL 1.3.0 pelo fio

Se o consumidor fala bencode direto (sem `nrepl.cmdline`), há **uma regra que
custa horas** se descoberta por tentativa:

> **Não envie o campo `id`.**

Medido em 2026-11-10, em três JVMs diferentes (servidor, engine e um
`clojure -M` puro):

| payload | resultado |
| :--- | :--- |
| `{"op":"clone","id":"c"}` | **timeout, zero bytes** — a sessão é criada no `jstack`, mas a resposta nunca sai |
| `{"op":"clone"}` | responde na hora: `d11:new-session36:...6:statusl4:doneee` |
| `{"op":"eval","session":S,"code":"..."}` | `5:value...` |

Case a resposta pela **`session`**, não por id. Detalhes do protocolo que também
mordem:

- `clone` **antes** de `eval` é obrigatório: mensagem endereçada a uma sessão
  inexistente é **descartada em silêncio** (nem erro, nem resposta).
- O bencode usa o comprimento em **bytes**, não caracteres.
- O namespace `user` da sessão **não tem `clojure.core` referenciado** —
  `(println ...)` falha com `Unable to resolve symbol`. Qualifique
  (`clojure.core/println`, `java.lang.Class/forName`) ou faça o `require`.
- Erro vem no campo `err` com status `eval-error`: um cliente que só olha
  `value`/`done` **sai antes de ler** e devolve vazio.

Um cliente mínimo que já implementa tudo isso está em
[`scripts/repl-probe.py`](../scripts/repl-probe.py) (porta `7888`).

---

## 4. Um consumidor completo, de ponta a ponta

O exemplo [`examples/consumer_minimal.py`](../examples/consumer_minimal.py) sobe
uma **segunda porta nREPL** apontando para uma cópia própria do código, e mostra
o ciclo inteiro: injetar → clonar → avaliar → ler. Ele é escrito para ser
copiado, não importado.

```bash
# 1. O craft-repl põe o Clojure na JVM (porta 7888)
make attach PID=$(pgrep -f fabric-server-launcher | head -1)

# 2. O consumidor fala com a porta dele
python3 examples/consumer_minimal.py --port 7888 '(+ 1 2)'
```

## 5. O mapa das portas

Um mesmo servidor pode hospedar várias portas nREPL, cada uma no seu
classloader. **Cada uma enxerga só o seu próprio código** — por desenho, não por
defeito.

| Porta | Dono | Classloader | Alcança |
| :--- | :--- | :--- | :--- |
| `7888` | **craft-repl** | Knot | `craft.core`, `craft.world`, `craft.player` (o `src/clj` dele) |
| `7890` | craft-agents | Knot | as classes Java nativas do servidor (`SyntheticPlayerFactory`) |
| `7891` | craft-agents | engine (`clojure -M`) | os namespaces `craft.agents.*` |

Um erro de namespace (`No namespace: X found`) **não** quer dizer que o nREPL
está quebrado: naquele classloader, aquele namespace realmente não existe.
