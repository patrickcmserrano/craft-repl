#!/usr/bin/env bash
# run_bugged_demo.sh - Automatizador do Take 1.1 da Karlach Bugada (Mineflayer)

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================================="
echo " 🎬 PREPARANDO AMBIENTE PARA O TAKE 1.1 (KARLACH BUGADA) "
echo "=========================================================="

cleanup() {
    echo ""
    echo "[!] Encerrando bot Mineflayer..."
    kill -9 "$NODE_PID" 2>/dev/null || true
    echo "[+] Restaurando o serviço moderno craft-agents (Server-Native)..."
    systemctl --user start craft-agents.service
    echo "[✅] Sistema restaurado com sucesso! Karlach Server-Native reassumiu."
    exit 0
}

trap cleanup INT TERM

echo "[1/3] Pausando craft-agents.service (liberando slot da Karlach)..."
systemctl --user stop craft-agents.service
sleep 1

# Garante que qualquer jogador Karlach anterior seja desconectado
curl -s -X POST http://localhost:8090/api/run -H "Content-Type: application/json" -d '{"cmd": "kick Karlach Liberando slot para demo Mineflayer"}' >/dev/null 2>&1 || true
sleep 1

echo "[2/3] Verificando se o slot de jogador está livre..."
curl -s -X POST http://localhost:8090/api/run -H "Content-Type: application/json" -d '{"cmd": "list"}' || true
echo ""

echo "[3/3] Iniciando npc_karlach_bugged.js..."
echo "----------------------------------------------------------"
echo " OBS: No Minecraft, vá para as coordenadas [-171, 65, 171]"
echo " Pressione Ctrl+C para encerrar e restaurar o craft-agents"
echo "----------------------------------------------------------"

node npc_karlach_bugged.js &
NODE_PID=$!

wait "$NODE_PID"
cleanup
