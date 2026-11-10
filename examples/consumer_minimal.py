#!/usr/bin/env python3
"""Consumidor minimo do canal do craft-repl.

Mostra, num arquivo so e sem dependencia, o ciclo inteiro de quem consome o
nREPL que o craft-repl injeta na JVM viva do Minecraft:

    craft-repl (make attach)  ->  porta 7888  ->  este script  ->  o jogo

Ele e para ser COPIADO, nao importado: cada regra abaixo foi medida em
2026-11-10 e custou voltas para ser descoberta.

REGRA 1 — nao mande o campo `id`.
    O nREPL 1.3.0 aceita a conexao, cria a sessao (visivel no jstack) e NUNCA
    devolve bytes se o payload trouxer `id`. O MESMO payload sem `id` responde
    na hora. Por isso este cliente casa as respostas por `session`.

REGRA 2 — clone antes de eval, sempre.
    Mensagem enderecada a uma sessao inexistente e descartada em SILENCIO.

REGRA 3 — bencode usa o comprimento em BYTES, nao em caracteres.

REGRA 4 — o ns `user` nao tem clojure.core referenciado.
    `(println ...)` falha com "Unable to resolve symbol"; qualifique
    (`clojure.core/println`) ou faca o require.

REGRA 5 — o erro vem no campo `err` com status `eval-error`.
    Um cliente que so olha value/done sai antes de ler e devolve vazio.

Uso:
    examples/consumer_minimal.py '(+ 1 2)'
    examples/consumer_minimal.py --port 7888 '(craft.core/... )'
    examples/consumer_minimal.py --file sonda.clj
"""
import argparse
import socket
import sys
import time

HOST = "127.0.0.1"


# --------------------------------------------------------------------------
# bencode / bdecode — REGRA 3: o comprimento e medido em bytes.
# --------------------------------------------------------------------------
def ben(o):
    if isinstance(o, bytes):
        return b"%d:" % len(o) + o
    if isinstance(o, str):
        return ben(o.encode("utf-8"))
    if isinstance(o, int):
        return b"i%de" % o
    if isinstance(o, list):
        return b"l" + b"".join(ben(x) for x in o) + b"e"
    if isinstance(o, dict):
        return b"d" + b"".join(ben(k) + ben(v) for k, v in o.items()) + b"e"
    raise TypeError(type(o))


def bdec(buf, i=0):
    c = buf[i : i + 1]
    if c == b"i":
        j = buf.index(b"e", i)
        return int(buf[i + 1 : j]), j + 1
    if c == b"l":
        i += 1
        out = []
        while buf[i : i + 1] != b"e":
            v, i = bdec(buf, i)
            out.append(v)
        return out, i + 1
    if c == b"d":
        i += 1
        out = {}
        while buf[i : i + 1] != b"e":
            k, i = bdec(buf, i)
            v, i = bdec(buf, i)
            out[k.decode() if isinstance(k, bytes) else k] = v
        return out, i + 1
    j = buf.index(b":", i)
    n = int(buf[i:j])
    return buf[j + 1 : j + 1 + n], j + 1 + n


def _s(v):
    return v.decode("utf-8", "replace") if isinstance(v, bytes) else str(v)


# --------------------------------------------------------------------------
# O ciclo do consumidor.
# --------------------------------------------------------------------------
def clone(sock, timeout):
    """Abre uma sessao. REGRA 1: sem `id`."""
    sock.sendall(ben({"op": "clone"}))
    sock.settimeout(timeout)
    buf, deadline = b"", time.time() + timeout
    while time.time() < deadline:
        chunk = sock.recv(65536)
        if not chunk:
            raise RuntimeError("conexao fechada durante o clone")
        buf += chunk
        while buf:
            try:
                msg, n = bdec(buf)
            except (ValueError, IndexError):
                break
            buf = buf[n:]
            if "new-session" in msg:
                return _s(msg["new-session"])
    raise RuntimeError("timeout esperando o clone")


def eval_form(sock, session, code, timeout):
    """Avalia `code` na sessao e devolve (out, err, value).

    REGRA 4/5: lemos `out`, `err` e `value`; paramos no primeiro status que
    traga `done` ou `eval-error` — e a resposta e casada por `session`.
    """
    sock.sendall(ben({"op": "eval", "session": session, "code": code}))
    sock.settimeout(timeout)
    out, err, val, buf = [], [], None, b""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            chunk = sock.recv(65536)
        except socket.timeout:
            break
        if not chunk:
            break
        buf += chunk
        while buf:
            try:
                msg, n = bdec(buf)
            except (ValueError, IndexError):
                break
            buf = buf[n:]
            if _s(msg.get("session", "")) != session:
                continue
            if "out" in msg:
                out.append(_s(msg["out"]))
            if "err" in msg:
                err.append(_s(msg["err"]))
            if "value" in msg:
                val = _s(msg["value"])
            st = msg.get("status")
            if st and ("done" in st or "eval-error" in st):
                return "".join(out), "".join(err), val
    return "".join(out), "".join(err), val


def evaluate(host, port, code, timeout=25):
    s = socket.create_connection((host, port), timeout=timeout)
    try:
        session = clone(s, timeout)  # REGRA 2
        return eval_form(s, session, code, timeout)
    finally:
        s.close()


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--host", default=HOST)
    ap.add_argument("--port", type=int, default=7888)
    ap.add_argument("--timeout", type=float, default=25)
    ap.add_argument("--file", help="le o codigo de um arquivo .clj")
    ap.add_argument("code", nargs="*")
    a = ap.parse_args()

    code = open(a.file).read() if a.file else " ".join(a.code)
    if not code.strip():
        ap.print_help()
        return 1

    try:
        out, err, val = evaluate(a.host, a.port, code, a.timeout)
    except (ConnectionRefusedError, OSError) as e:
        print("[-] Nao consegui falar com %s:%d — o agente esta injetado? "
              "(make attach)" % (a.host, a.port), file=sys.stderr)
        print("    %s" % e, file=sys.stderr)
        return 2

    if err.strip():
        print("--- ERR ---")
        print(err.rstrip())
    if out.strip():
        print(out.rstrip())
    if val is not None:
        print("=> %s" % val)
    return 0


if __name__ == "__main__":
    sys.exit(main())
