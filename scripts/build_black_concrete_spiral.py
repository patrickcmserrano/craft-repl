#!/usr/bin/env python3
"""
build_black_concrete_spiral.py
Constroi a Grande Escada Espiral em Concreto Preto conectando o chao do castelo (Y=75)
ao piso da torre (Y=106/107) em torno do eixo central (X=-38, Z=243).
Gera backup automatico para reversao 100% segura.
"""

import socket
import json
import math
import os
import sys
import time

BACKUP_FILE = "/home/pace/dev/craft-repl/scripts/spiral_staircase_backup.json"
HOST = "127.0.0.1"
PORT = 25595

def get_socket():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(5.0)
    s.connect((HOST, PORT))
    return s

def query_block(s, x, y, z):
    s.sendall(f"BLOCK|{x}|{y}|{z}\n".encode("utf-8"))
    res = s.recv(1024).decode("utf-8").strip()
    parts = res.split("|")
    return parts[1] if len(parts) > 1 else res

def send_cmd(s, cmd):
    s.sendall(f"CMD|{cmd}\n".encode("utf-8"))
    res = s.recv(1024).decode("utf-8").strip()
    return res

def main():
    print("🔌 Conectando ao craft-repl (porta 25595)...")
    s = get_socket()
    
    cx, cz = -38, 243
    y_ground = 64
    y_start = 75
    y_end = 106
    steps_per_turn = 16
    total_steps = y_end - y_start # 31

    changes = {} # (x, y, z) -> new_block

    # 1. Pilar central de Black Concrete (Y=64 ate Y=108)
    for y in range(y_ground, y_end + 3):
        changes[(cx, y, cz)] = "minecraft:black_concrete"

    # 2. Degraus em espiral (Y=75 ate Y=106)
    headroom_air = set()
    for y in range(y_start, y_end + 1):
        step_idx = y - y_start
        theta = (step_idx - total_steps) * (2 * math.pi / steps_per_turn)
        
        # Wedge de degrau: raio 1.1 ate 2.4
        step_blocks = set()
        for r in [1.1, 1.6, 2.2]:
            for d_th in [-0.12, 0.0, 0.12]:
                bx = int(round(cx + r * math.cos(theta + d_th)))
                bz = int(round(cz + r * math.sin(theta + d_th)))
                if (bx, bz) != (cx, cz):
                    step_blocks.add((bx, bz))
        
        for (bx, bz) in step_blocks:
            changes[(bx, y, bz)] = "minecraft:black_concrete"
            # Assegura folga para a cabeca (3 blocos de ar acima)
            for dy in [1, 2, 3]:
                headroom_air.add((bx, y + dy, bz))

    # Nao apaga degraus futuros com headroom
    for pos in headroom_air:
        if pos not in changes:
            changes[pos] = "minecraft:air"

    # 3. Passarela de conexao no piso inferior (Y=75) em direcao a biblioteca (X=-37 ate X=-28)
    for x in range(-36, -27):
        for z in [243, 244]:
            changes[(x, y_start, z)] = "minecraft:black_concrete"
            changes[(x, y_start + 1, z)] = "minecraft:air"
            changes[(x, y_start + 2, z)] = "minecraft:air"
            changes[(x, y_start + 3, z)] = "minecraft:air"

    # 4. Desobstrucao suave no topo da torre (Y=106..109) na saida da escada
    for x in [-37, -36, -35]:
        for z in [242, 243, 244]:
            changes[(x, y_end + 1, z)] = "minecraft:air"
            changes[(x, y_end + 2, z)] = "minecraft:air"
            changes[(x, y_end + 3, z)] = "minecraft:air"

    # 5. Iluminacao anti-mob discreta a cada 4 degraus
    for y in range(y_start + 2, y_end, 4):
        step_idx = y - y_start
        theta = (step_idx - total_steps) * (2 * math.pi / steps_per_turn)
        bx = int(round(cx + 1.8 * math.cos(theta)))
        bz = int(round(cz + 1.8 * math.sin(theta)))
        if (bx, y + 1, bz) in changes and changes[(bx, y + 1, bz)] == "minecraft:air":
            changes[(bx, y + 1, bz)] = "minecraft:light[level=14]"

    print(f"📦 Total de blocos a serem processados: {len(changes)}")
    
    # Leitura e gravacao de Backup
    backup_data = []
    print("💾 Gravando backup dos blocos originais...")
    for (x, y, z) in changes.keys():
        orig = query_block(s, x, y, z)
        backup_data.append({"x": x, "y": y, "z": z, "original": orig})

    with open(BACKUP_FILE, "w", encoding="utf-8") as f:
        json.dump(backup_data, f, indent=2)
    print(f"✅ Backup salvo com sucesso em: {BACKUP_FILE}")

    # Execucao das mudancas
    print("🏗️ Construindo escada espiral em concreto preto no Minecraft...")
    applied = 0
    for (x, y, z), block in changes.items():
        cmd = f"setblock {x} {y} {z} {block} replace"
        send_cmd(s, cmd)
        applied += 1
        if applied % 50 == 0:
            sys.stdout.write(f"\r  Progresso: {applied}/{len(changes)} blocos...")
            sys.stdout.flush()

    sys.stdout.write(f"\r  Progresso: {applied}/{len(changes)} blocos... CONCLUÍDO!\n")
    s.close()
    print("🎉 Escada espiral construída com sucesso!")

if __name__ == "__main__":
    main()
