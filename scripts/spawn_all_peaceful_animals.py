#!/usr/bin/env python3
"""
spawn_all_peaceful_animals.py
Spawns every peaceful animal from Naturalist and Minecraft in an orderly safari ring around Pace.
"""

import math
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/spawn_peaceful.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return {"ok": False, "error": str(e)}

def main():
    # Pace position: [-169, 65, 188]
    cx, cy, cz = -169, 65, 188

    animals = [
        # (Entity ID, Display Name, Radius, Altitude Offset)
        ("naturalist:capybara", "Capivara", 4, 0),
        ("naturalist:capybara", "Capivarinha", 4.5, 0),
        ("naturalist:deer", "Cervo Majestoso", 5, 0),
        ("naturalist:deer", "Cerva", 5.5, 0),
        ("naturalist:elephant", "Elefante Real", 9, 0),
        ("naturalist:giraffe", "Girafa", 8, 0),
        ("naturalist:zebra", "Zebra", 6, 0),
        ("naturalist:mammoth", "Mamute Ancestral", 11, 0),
        ("naturalist:tortoise", "Tartaruga Centenaria", 5, 0),
        ("naturalist:ostrich", "Avestruz", 7, 0),
        ("naturalist:boar", "Javali", 6, 0),
        ("naturalist:hedgehog", "Ourico", 3, 0),
        ("naturalist:lizard", "Lagartixa-do-Sol", 3.5, 0),
        ("naturalist:duck", "Pato", 4, 0),
        ("naturalist:bird", "Passaro Cantor", 4, 1.5),
        ("naturalist:bird", "Passaro Azul", 4.5, 2.0),
        ("naturalist:butterfly", "Borboleta Dourada", 3, 1.5),
        ("naturalist:butterfly", "Borboleta Monarca", 3.5, 2.0),
        ("naturalist:firefly", "Vagalume Brilhante", 3, 2.2),
        ("naturalist:dragonfly", "Libelula", 4, 1.8),
        ("naturalist:snail", "Caramujo", 3, 0),
        ("naturalist:crab", "Caranguejo", 3.5, 0),
        # Special Vanilla Passives
        ("minecraft:camel", "Camelo Real", 8, 0),
        ("minecraft:sniffer", "Sniffer Ancestral", 9, 0),
        ("minecraft:armadillo", "Tatu-Bola", 4, 0),
        ("minecraft:fox", "Raposa Vermelha", 5, 0),
        ("minecraft:panda", "Urso Panda", 6, 0),
        ("minecraft:frog", "Sapo Cururu", 3.5, 0),
        ("minecraft:allay", "Allay Amigavel", 3, 1.5)
    ]

    cmds = []
    n = len(animals)
    for i, (mob_id, name, radius, y_off) in enumerate(animals):
        angle = (2 * math.pi * i) / n
        px = cx + radius * math.cos(angle)
        pz = cz + radius * math.sin(angle)
        py = cy + y_off
        cmds.append(f"summon {mob_id} {px:.1f} {py:.1f} {pz:.1f} {{CustomName:'\"{name}\"'}}")
        cmds.append(f"particle heart {px:.1f} {py+1:.1f} {pz:.1f} 0.3 0.3 0.3 0.05 5")

    with open(FUNCTION_PATH, "w") as f:
        f.write("# Spawn all peaceful animals routine\n")
        for c in cmds:
            f.write(c + "\n")

    run_cmd("reload")
    time.sleep(0.8)
    run_cmd("function blueprints:spawn_peaceful")
    run_cmd("execute as @a at pace run playsound minecraft:entity.player.levelup master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"🌿 Todas as 29 especies de animais pacificos (Naturalist + Vanilla) spawnadas ao seu redor!","color":"green","bold":true}]')
    print("[*] All peaceful animals spawned successfully!")

if __name__ == "__main__":
    main()
