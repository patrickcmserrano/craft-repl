#!/usr/bin/env python3
"""
build_grand_wooden_spiral.py
Constrói a Grande Espiral Aberta de Madeira e Concreto Preto inspirada na Torre:
- Vão amplo e aberto (12x12) com átrio central livre
- Degraus reais de madeira (spruce_stairs) para subida suave
- Estrutura inferior e paredes externas de contorno em cimento preto (black_concrete)
- Patamares de esquina com lanternas góticas e guarda-corpo de madeira
- Backup 100% gravado para reversão instantânea
"""

import socket
import json
import os
import sys

BACKUP_FILE = "/home/pace/dev/craft-repl/scripts/grand_spiral_backup.json"
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

    changes = {}

    def add(x, y, z, block):
        changes[(x, y, z)] = block

    # 1. Quatro Lances de Escada (Flights)
    flights = [
        # (nome, c_start, c_end, y_start, is_x, outer_c, inner_c, wall_c, facing, fence_c)
        # Flight 1: Parede Leste (X=-33..-34) descendo para o Sul (Z: 240 -> 246), Y: 75 -> 81
        ("Flight 1 (Leste -> Sul)", 240, 246, 75, False, -33, -34, -32, "south", -35),
        # Flight 2: Parede Sul (Z=247..246) indo para o Oeste (X: -35 -> -41), Y: 83 -> 89
        ("Flight 2 (Sul -> Oeste)", -35, -41, 83, True, 247, 246, 248, "west", 245),
        # Flight 3: Parede Oeste (X=-42..-41) subindo para o Norte (Z: 246 -> 240), Y: 91 -> 97
        ("Flight 3 (Oeste -> Norte)", 246, 240, 91, False, -42, -41, -43, "north", -40),
        # Flight 4: Parede Norte (Z=238..239) indo para o Leste (X: -40 -> -34), Y: 99 -> 105
        ("Flight 4 (Norte -> Leste)", -40, -34, 99, True, 238, 239, 237, "east", 240)
    ]

    for name, c1, c2, y1, is_x, outer_c, inner_c, wall_c, facing, fence_c in flights:
        step = 1 if c2 >= c1 else -1
        coords = list(range(c1, c2 + step, step))
        for idx, c in enumerate(coords):
            cy = y1 + idx
            # Degraus de madeira e base de concreto preto
            for fc in [outer_c, inner_c]:
                bx, bz = (c, fc) if is_x else (fc, c)
                add(bx, cy, bz, f"minecraft:spruce_stairs[facing={facing},half=bottom]")
                add(bx, cy - 1, bz, "minecraft:black_concrete")
                for hy in [1, 2, 3]:
                    add(bx, cy + hy, bz, "minecraft:air")
            
            # Parede externa de concreto preto ao redor da escada
            wx, wz = (c, wall_c) if is_x else (wall_c, c)
            for wy in range(cy, cy + 3):
                add(wx, wy, wz, "minecraft:black_concrete")
            
            # Guarda-corpo de madeira no lado interno do átrio
            fx, fz = (c, fence_c) if is_x else (fence_c, c)
            add(fx, cy + 1, fz, "minecraft:spruce_fence")

    # 2. Patamares de Esquina (Landings)
    landings = [
        # (nome, x1, x2, z1, z2, y, lantern_pos, corner_wall)
        ("Landing 1 (SE)", -34, -33, 247, 248, 82, (-33, 83, 248), [(-32, 247), (-32, 248), (-33, 249), (-34, 249)]),
        ("Landing 2 (SW)", -42, -41, 247, 248, 90, (-42, 91, 248), [(-43, 247), (-43, 248), (-41, 249), (-42, 249)]),
        ("Landing 3 (NW)", -42, -41, 238, 239, 98, (-42, 99, 238), [(-43, 238), (-43, 239), (-41, 237), (-42, 237)]),
    ]

    for name, x1, x2, z1, z2, ly, (lx, lz_y, lz), walls in landings:
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                add(x, ly - 1, z, "minecraft:black_concrete")
                add(x, ly, z, "minecraft:spruce_planks")
                for hy in [1, 2, 3]:
                    add(x, ly + hy, z, "minecraft:air")
        
        # Paredes externas no patamar
        for wx, wz in walls:
            for wy in range(ly, ly + 4):
                add(wx, wy, wz, "minecraft:black_concrete")
        
        # Lanterna no canto
        add(lx, lz_y, lz, "minecraft:lantern[hanging=false]")

    # 3. Patamar Superior de Chegada na Torre (Y=106)
    # Conecta o topo do Flight 4 diretamente ao piso da torre
    for x in range(-35, -32):
        for z in range(238, 244):
            add(x, 105, z, "minecraft:black_concrete")
            add(x, 106, z, "minecraft:spruce_planks")
            for hy in [1, 2, 3]:
                add(x, 106 + hy, z, "minecraft:air")

    # 4. Passarela de Conexão Inferior (Y=75)
    # Conecta o início da escada (X=-34, Z=240) à biblioteca de pedra do castelo (até X=-28)
    for x in range(-34, -27):
        for z in [240, 241]:
            add(x, 74, z, "minecraft:black_concrete")
            add(x, 75, z, "minecraft:spruce_planks")
            for hy in [1, 2, 3]:
                add(x, 75 + hy, z, "minecraft:air")
        # Guarda-corpo ou balustrada na ponte inferior
        add(x, 76, 239, "minecraft:spruce_fence")
        add(x, 76, 242, "minecraft:spruce_fence")

    print(f"📦 Total de blocos a processar: {len(changes)}")

    # Gravando Backup
    print("💾 Gravando backup de segurança dos blocos originais...")
    backup_data = []
    for (x, y, z) in changes.keys():
        orig = query_block(s, x, y, z)
        backup_data.append({"x": x, "y": y, "z": z, "original": orig})

    with open(BACKUP_FILE, "w", encoding="utf-8") as f:
        json.dump(backup_data, f, indent=2)
    print(f"✅ Backup salvo com sucesso em: {BACKUP_FILE}")

    # Execução das mudanças
    print("🏗️ Construindo a Grande Espiral Aberta de Madeira e Concreto Preto...")
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
    print("🎉 Grande Espiral Aberta construída com sucesso!")

if __name__ == "__main__":
    main()
