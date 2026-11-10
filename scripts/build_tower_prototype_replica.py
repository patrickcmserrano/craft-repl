#!/usr/bin/env python3
"""
build_tower_prototype_replica.py
Constrói a verdadeira Torre de Escada Helicoidal no local correto (eixo da torre/buraco),
replicando com perfeição o padrão criado por Pace e Lizzie no protótipo:
- Caixa exterior 8x8 de Concreto Preto (black_concrete) com janelas/aberturas verticais
- Núcleo central 2x2 de Concreto Preto
- Escadas de 2 degraus por face em madeira (spruce_stairs) com largura 2
- Patamares de canto 2x2 em tábuas de pinheiro (spruce_planks)
- Subida de 12 blocos por rotação de 360° (headroom livre de 9 blocos)
- Conexão da ponte em Y=75 partindo da biblioteca (X=-28) até a entrada Leste
- Chegada em Y=106 direto no piso/abertura da torre
- Gravação completa de backup para reversão instantânea
"""

import socket
import json
import os
import sys

BACKUP_FILE = "/home/pace/dev/craft-repl/scripts/tower_prototype_replica_backup.json"
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

    # 1. Núcleo central 2x2 de Concreto Preto (Y=64 até Y=108)
    for y in range(64, 109):
        for x in [-39, -38]:
            for z in [242, 243]:
                add(x, y, z, "minecraft:black_concrete")

    # 2. Paredes Externas de Concreto Preto (X: [-42..-35], Z: [239..246])
    # De Y=74 até Y=108
    for y in range(74, 108):
        for x in range(-42, -34):
            for z in [239, 246]:
                # Paredes Norte e Sul com frestas/janelas góticas verticais
                if x in [-39, -38] and (y % 6 in [2, 3]) and y < 105:
                    add(x, y, z, "minecraft:air")
                else:
                    add(x, y, z, "minecraft:black_concrete")
        
        for z in range(240, 246):
            for x in [-42, -35]:
                # Porta de entrada na parede Leste (X=-35) em Y=75..77
                if x == -35 and z in [240, 241] and y in [75, 76, 77]:
                    add(x, y, z, "minecraft:air")
                # Janelas na parede Oeste e Leste
                elif z in [242, 243] and (y % 6 in [2, 3]) and y < 105:
                    add(x, y, z, "minecraft:air")
                else:
                    add(x, y, z, "minecraft:black_concrete")

    # 3. Lances Helicoidais e Patamares (subindo de Y=75 até Y=106)
    cur_y = 75
    while cur_y <= 106:
        # A. Patamar NE (X in [-37, -36], Z in [240, 241])
        if cur_y <= 106:
            for x in [-37, -36]:
                for z in [240, 241]:
                    add(x, cur_y - 1, z, "minecraft:black_concrete")
                    add(x, cur_y, z, "minecraft:spruce_planks")
                    for hy in [1, 2, 3]:
                        add(x, cur_y + hy, z, "minecraft:air")
            # Lanterna em patamar intermediário
            add(-36, cur_y + 1, 240, "minecraft:lantern[hanging=false]")

        # B. Lance Sul (descendo em Z ao longo do Leste, facing=south)
        if cur_y + 1 <= 106:
            for x in [-37, -36]:
                # Degrau 1
                add(x, cur_y, 242, "minecraft:black_concrete")
                add(x, cur_y + 1, 242, "minecraft:spruce_stairs[facing=south,half=bottom]")
                for hy in [1, 2, 3]:
                    add(x, cur_y + 1 + hy, 242, "minecraft:air")
        if cur_y + 2 <= 106:
            for x in [-37, -36]:
                # Degrau 2
                add(x, cur_y + 1, 243, "minecraft:black_concrete")
                add(x, cur_y + 2, 243, "minecraft:spruce_stairs[facing=south,half=bottom]")
                for hy in [1, 2, 3]:
                    add(x, cur_y + 2 + hy, 243, "minecraft:air")

        # C. Patamar SE (X in [-37, -36], Z in [244, 245])
        if cur_y + 3 <= 106:
            for x in [-37, -36]:
                for z in [244, 245]:
                    add(x, cur_y + 2, z, "minecraft:black_concrete")
                    add(x, cur_y + 3, z, "minecraft:spruce_planks")
                    for hy in [1, 2, 3]:
                        add(x, cur_y + 3 + hy, z, "minecraft:air")
            add(-36, cur_y + 4, 245, "minecraft:lantern[hanging=false]")

        # D. Lance Oeste (indo em X ao longo do Sul, facing=west)
        if cur_y + 4 <= 106:
            for z in [244, 245]:
                add(-38, cur_y + 3, z, "minecraft:black_concrete")
                add(-38, cur_y + 4, z, "minecraft:spruce_stairs[facing=west,half=bottom]")
                for hy in [1, 2, 3]:
                    add(-38, cur_y + 4 + hy, z, "minecraft:air")
        if cur_y + 5 <= 106:
            for z in [244, 245]:
                add(-39, cur_y + 4, z, "minecraft:black_concrete")
                add(-39, cur_y + 5, z, "minecraft:spruce_stairs[facing=west,half=bottom]")
                for hy in [1, 2, 3]:
                    add(-39, cur_y + 5 + hy, z, "minecraft:air")

        # E. Patamar SW (X in [-41, -40], Z in [244, 245])
        if cur_y + 6 <= 106:
            for x in [-41, -40]:
                for z in [244, 245]:
                    add(x, cur_y + 5, z, "minecraft:black_concrete")
                    add(x, cur_y + 6, z, "minecraft:spruce_planks")
                    for hy in [1, 2, 3]:
                        add(x, cur_y + 6 + hy, z, "minecraft:air")
            add(-41, cur_y + 7, 245, "minecraft:lantern[hanging=false]")

        # F. Lance Norte (subindo em Z ao longo do Oeste, facing=north)
        if cur_y + 7 <= 106:
            for x in [-41, -40]:
                add(x, cur_y + 6, 243, "minecraft:black_concrete")
                add(x, cur_y + 7, 243, "minecraft:spruce_stairs[facing=north,half=bottom]")
                for hy in [1, 2, 3]:
                    add(x, cur_y + 7 + hy, 243, "minecraft:air")
        if cur_y + 8 <= 106:
            for x in [-41, -40]:
                add(x, cur_y + 7, 242, "minecraft:black_concrete")
                add(x, cur_y + 8, 242, "minecraft:spruce_stairs[facing=north,half=bottom]")
                for hy in [1, 2, 3]:
                    add(x, cur_y + 8 + hy, 242, "minecraft:air")

        # G. Patamar NW (X in [-41, -40], Z in [240, 241])
        if cur_y + 9 <= 106:
            for x in [-41, -40]:
                for z in [240, 241]:
                    add(x, cur_y + 8, z, "minecraft:black_concrete")
                    add(x, cur_y + 9, z, "minecraft:spruce_planks")
                    for hy in [1, 2, 3]:
                        add(x, cur_y + 9 + hy, z, "minecraft:air")
            add(-41, cur_y + 10, 240, "minecraft:lantern[hanging=false]")

        # H. Lance Leste (indo em X ao longo do Norte, facing=east)
        if cur_y + 10 <= 106:
            for z in [240, 241]:
                add(-39, cur_y + 9, z, "minecraft:black_concrete")
                add(-39, cur_y + 10, z, "minecraft:spruce_stairs[facing=east,half=bottom]")
                for hy in [1, 2, 3]:
                    add(-39, cur_y + 10 + hy, z, "minecraft:air")
        if cur_y + 11 <= 106:
            for z in [240, 241]:
                add(-38, cur_y + 10, z, "minecraft:black_concrete")
                add(-38, cur_y + 11, z, "minecraft:spruce_stairs[facing=east,half=bottom]")
                for hy in [1, 2, 3]:
                    add(-38, cur_y + 11 + hy, z, "minecraft:air")

        cur_y += 12

    # 4. Saída no Topo da Torre (Y=106..108)
    # Abre o teto na chegada do lance superior
    for x in [-41, -40, -39]:
        for z in [241, 242, 243]:
            add(x, 106, z, "minecraft:spruce_planks")
            add(x, 107, z, "minecraft:air")
            add(x, 108, z, "minecraft:air")
            add(x, 109, z, "minecraft:air")

    # 5. Ponte de Conexão na Base (Y=75) da Biblioteca até a Entrada
    # De X=-28 até X=-35 ao longo de Z=240, 241
    for x in range(-34, -27):
        for z in [240, 241]:
            add(x, 74, z, "minecraft:black_concrete")
            add(x, 75, z, "minecraft:spruce_planks")
            for hy in [1, 2, 3]:
                add(x, 75 + hy, z, "minecraft:air")
        # Guarda-corpo na ponte
        add(x, 76, 239, "minecraft:spruce_fence")
        add(x, 76, 242, "minecraft:spruce_fence")

    print(f"📦 Total de blocos a processar: {len(changes)}")

    # Backup de Segurança
    print("💾 Gravando backup de segurança dos blocos originais...")
    backup_data = []
    for (x, y, z) in changes.keys():
        orig = query_block(s, x, y, z)
        backup_data.append({"x": x, "y": y, "z": z, "original": orig})

    with open(BACKUP_FILE, "w", encoding="utf-8") as f:
        json.dump(backup_data, f, indent=2)
    print(f"✅ Backup salvo com sucesso em: {BACKUP_FILE}")

    # Execução das mudanças
    print("🏗️ Construindo a Torre de Escada Helicoidal no local correto...")
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
    print("🎉 Construção concluída com sucesso!")

if __name__ == "__main__":
    main()
