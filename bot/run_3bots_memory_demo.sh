#!/usr/bin/env bash
# run_3bots_memory_demo.sh - Sobe os 3 bots legados Node.js para demonstrar o consumo de >600MB RAM no htop

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=========================================================="
echo " 🎬 MINEFLAYER 3-BOT MEMORY DEMO (HTOP / PS RECORDING)    "
echo "=========================================================="

cleanup() {
    echo ""
    echo "[!] Encerrando os 3 bots Mineflayer..."
    kill -9 $PIDS 2>/dev/null || true
    echo "[+] Restaurando o serviço moderno craft-agents (Server-Native)..."
    systemctl --user start craft-agents.service
    echo "[✅] Sistema restaurado!"
    exit 0
}

trap cleanup INT TERM

echo "[1/3] Pausando craft-agents.service..."
systemctl --user stop craft-agents.service
sleep 1

# Garante que os slots de jogador no Fabric fiquem 100% livres para o Mineflayer
curl -s -X POST http://localhost:8090/api/run -H "Content-Type: application/json" -d '{"cmd": "kick Karlach Liberando slot para demo Mineflayer"}' >/dev/null 2>&1 || true
curl -s -X POST http://localhost:8090/api/run -H "Content-Type: application/json" -d '{"cmd": "kick Picard Liberando slot para demo Mineflayer"}' >/dev/null 2>&1 || true
curl -s -X POST http://localhost:8090/api/run -H "Content-Type: application/json" -d '{"cmd": "kick Antigravity Liberando slot para demo Mineflayer"}' >/dev/null 2>&1 || true
sleep 1

echo "[2/3] Subindo Karlach, Picard e Antigravity via Node.js..."
node npc_karlach_bugged.js &
PID1=$!
node npc_picard.js &
PID2=$!
node npc_antigravity.js &
PID3=$!
PIDS="$PID1 $PID2 $PID3"

echo "[3/3] Bots online! Monitorando memória RSS a cada 3 segundos..."
echo "Pressione Ctrl+C para encerrar."
echo "----------------------------------------------------------"

while true; do
    echo "--- $(date +%H:%M:%S) ---"
    ps -o pid,rss,comm,args -p $PIDS 2>/dev/null || true
    total_rss=$(ps -o rss= -p $PIDS 2>/dev/null | awk '{sum+=$1} END {print sum/1024 " MB"}')
    echo ">> Consumo Total de RAM dos 3 bots Node: $total_rss"
    sleep 3
done
