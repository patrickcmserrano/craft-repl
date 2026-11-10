#!/usr/bin/env python3
"""
revert_tower_prototype_replica.py
Restaura os blocos originais gravados em tower_prototype_replica_backup.json,
revertendo 100% da construcao.
"""

import socket
import json
import os
import sys

BACKUP_FILE = "/home/pace/dev/craft-repl/scripts/tower_prototype_replica_backup.json"
HOST = "127.0.0.1"
PORT = 25595

def main():
    if not os.path.exists(BACKUP_FILE):
        print(f"❌ Arquivo de backup não encontrado: {BACKUP_FILE}")
        sys.exit(1)

    with open(BACKUP_FILE, "r", encoding="utf-8") as f:
        backup_data = json.load(f)

    print(f"🔄 Restaurando {len(backup_data)} blocos originais...")
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(5.0)
    s.connect((HOST, PORT))

    reverted = 0
    for item in backup_data:
        x, y, z = item["x"], item["y"], item["z"]
        orig = item["original"]
        if not orig or orig == "null" or "air" in orig:
            cmd = f"setblock {x} {y} {z} minecraft:air replace"
        else:
            cmd = f"setblock {x} {y} {z} {orig} replace"
        s.sendall(f"CMD|{cmd}\n".encode("utf-8"))
        s.recv(1024)
        reverted += 1
        if reverted % 50 == 0:
            sys.stdout.write(f"\r  Progresso: {reverted}/{len(backup_data)} blocos restaurados...")
            sys.stdout.flush()

    sys.stdout.write(f"\r  Progresso: {reverted}/{len(backup_data)} blocos restaurados... CONCLUÍDO!\n")
    s.close()
    print("✨ Reversão 100% concluída com sucesso!")

if __name__ == "__main__":
    main()
