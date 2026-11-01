#!/usr/bin/env python3
"""
build_predator_sanctuary.py
Constructs the High-Security Apex Predator Sanctuary right in front of Pace:
- Footprint: X: -268..-216, Z: 35..85, Y: 64..72
- 4 Reinforced Habitats with Iron Bars & Armored Tinted Glass:
  1. Reino dos Leões (Lions & Vultures with rocky outcrop & acacia)
  2. Selva dos Tigres (Tigers & Komodo Dragons with jungle canopy & pond)
  3. Pântano dos Jacarés (Alligators & Snakes with deep swamp lagoon)
  4. Caverna dos Ursos (Grizzly & Polar Bears, Wolves with mountain stream)
- Elevated Suspended Catwalk (Y=68) allowing visitors to walk directly over the habitats safely!
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_predator_sanctuary.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # Overall bounds:
    # X: -268 to -216 (53 blocks wide)
    # Z: 35 to 85 (51 blocks deep)
    # Altitude: Y: 64 (ground) to Y: 72 (catwalk & towers)
    x1, x2 = -268, -216
    z1, z2 = 35, 85
    base_y = 64

    # Clear air space for the predator sanctuary
    cmds.append(f"fill {x1} {base_y+1} {z1} {x2} 75 {z2} air replace")
    # Base ground plinth
    cmds.append(f"fill {x1} {base_y} {z1} {x2} {base_y} {z2} grass_block replace")

    # Outer Fortress Perimeter (Reinforced 4-block high deepslate & iron bars)
    cmds.append(f"fill {x1} 65 {z1} {x2} 65 {z1} deepslate_bricks replace")
    cmds.append(f"fill {x1} 65 {z2} {x2} 65 {z2} deepslate_bricks replace")
    cmds.append(f"fill {x1} 65 {z1} {x1} 65 {z2} deepslate_bricks replace")
    cmds.append(f"fill {x2} 65 {z1} {x2} 65 {z2} deepslate_bricks replace")
    # Iron bars upper barrier
    cmds.append(f"fill {x1} 66 {z1} {x2} 68 {z1} iron_bars replace")
    cmds.append(f"fill {x1} 66 {z2} {x2} 68 {z2} iron_bars replace")
    cmds.append(f"fill {x1} 66 {z1} {x1} 68 {z2} iron_bars replace")
    cmds.append(f"fill {x2} 66 {z1} {x2} 68 {z2} iron_bars replace")
    # Top security overhang
    cmds.append(f"fill {x1} 69 {z1} {x2} 69 {z1} deepslate_tile_slab[type=bottom] replace")
    cmds.append(f"fill {x1} 69 {z2} {x2} 69 {z2} deepslate_tile_slab[type=bottom] replace")
    cmds.append(f"fill {x1} 69 {z1} {x1} 69 {z2} deepslate_tile_slab[type=bottom] replace")
    cmds.append(f"fill {x2} 69 {z1} {x2} 69 {z2} deepslate_tile_slab[type=bottom] replace")

    # Corner Guard Watchtowers
    for px, pz in [(x1, z1), (x2, z1), (x1, z2), (x2, z2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 71 {pz} deepslate_bricks replace")
        cmds.append(f"setblock {px} 72 {pz} sea_lantern replace")

    # Central Elevated Visitor Catwalk (Width 5: Z: 58..62, running from X: -216 to -268 at Y=68)
    # Allows safe high-altitude walking above all predator pits!
    # Floor of catwalk
    cmds.append(f"fill {x1} 68 58 {x2} 68 62 polished_deepslate_slab[type=top] replace")
    # Safety parapets along both sides of catwalk
    cmds.append(f"fill {x1} 69 58 {x2} 69 58 iron_bars replace")
    cmds.append(f"fill {x1} 69 62 {x2} 69 62 iron_bars replace")
    # Support pillars underneath catwalk every 8 blocks
    for cx in range(x1 + 4, x2, 8):
        cmds.append(f"fill {cx} 65 58 {cx} 68 58 deepslate_brick_wall replace")
        cmds.append(f"fill {cx} 65 62 {cx} 68 62 deepslate_brick_wall replace")
        cmds.append(f"setblock {cx} 70 58 lantern[hanging=false] replace")
        cmds.append(f"setblock {cx} 70 62 lantern[hanging=false] replace")

    # Catwalk Grand Entrance Stairway at East end (X: -216, connecting to ground at X: -212)
    cmds.append("fill -215 65 59 -215 65 61 polished_deepslate replace")
    cmds.append("fill -214 65 59 -214 65 61 polished_deepslate_stairs[facing=west,half=bottom] replace")
    cmds.append("fill -213 66 59 -213 66 61 polished_deepslate_stairs[facing=west,half=bottom] replace")
    cmds.append("fill -214 65 59 -213 65 61 polished_deepslate replace")
    cmds.append("fill -215 66 59 -215 67 61 polished_deepslate replace")
    cmds.append("fill -216 68 59 -216 68 61 polished_deepslate replace")

    # North-South Dividing Wall between East and West quadrants (X: -242..-240)
    cmds.append(f"fill -242 65 {z1} -240 68 {z2} deepslate_bricks replace")
    cmds.append(f"fill -242 69 {z1} -240 69 {z2} iron_bars replace")

    # -------------------------------------------------------------------------
    # HABITAT 1: REINO DOS LEÕES & URUBUS (Northeast: X: -239 to -217, Z: 36 to 57)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -239, -217, 36, 57
    # Ground: coarse dirt, sand, granite rocks
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} coarse_dirt replace")
    cmds.append(f"fill {hx1+4} 64 {hz1+3} {hx1+12} 64 {hz1+10} sand replace")
    # Pride Rock: Elevated rocky outcrop for lions to climb
    cmds.append(f"fill {hx1+8} 65 {hz1+8} {hx1+14} 67 {hz1+14} granite replace")
    cmds.append(f"fill {hx1+9} 68 {hz1+9} {hx1+13} 68 {hz1+13} polished_granite replace")
    cmds.append(f"fill {hx1+10} 69 {hz1+10} {hx1+12} 69 {hz1+12} smooth_stone_slab[type=bottom] replace")
    # Bone blocks and dried shrubbery
    cmds.append(f"setblock {hx1+4} 65 {hz2-4} bone_block[axis=x] replace")
    cmds.append(f"setblock {hx1+5} 65 {hz2-4} bone_block[axis=y] replace")
    cmds.append(f"setblock {hx2-4} 65 {hz1+4} acacia_leaves replace")
    # Tinted glass viewing window facing the catwalk (Z: 57)
    cmds.append(f"fill {hx1+2} 66 57 {hx2-2} 68 57 tinted_glass replace")
    # Residents
    cmds.append(f"summon naturalist:lion {hx1+11} 70 {hz1+11} {{CustomName:'\"Rei Leao\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:lion {hx1+6} 65 {hz1+6} {{CustomName:'\"Leoa Cacadora\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:vulture {hx1+11} 72 {hz1+11} {{CustomName:'\"Urubu da Savana\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:vulture {hx2-5} 70 {hz2-5} {{CustomName:'\"Abutre Vigia\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 2: SELVA DOS TIGRES & KOMODO (Northwest: X: -267 to -243, Z: 36 to 57)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -267, -243, 36, 57
    # Jungle canopy ground: grass, moss, jungle trees
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} grass_block replace")
    cmds.append(f"fill {hx1+4} 64 {hz1+4} {hx1+10} 64 {hz1+10} moss_block replace")
    # Jungle tree
    jx, jz = hx1 + 12, hz1 + 10
    cmds.append(f"fill {jx} 65 {jz} {jx} 72 {jz} jungle_log replace")
    cmds.append(f"fill {jx-3} 70 {jz-3} {jx+3} 73 {jz+3} jungle_leaves replace")
    # Bathing pool for tigers
    cmds.append(f"fill {hx1+3} 64 {hz2-8} {hx1+8} 64 {hz2-3} water replace")
    # Bamboo thicket
    for bx in range(hx2-8, hx2-2, 2):
        for bz in range(hz1+3, hz1+9, 2):
            cmds.append(f"setblock {bx} 65 {bz} bamboo[age=1,stage=0] replace")
    # Viewing window facing catwalk (Z: 57)
    cmds.append(f"fill {hx1+2} 66 57 {hx2-2} 68 57 tinted_glass replace")
    # Residents
    cmds.append(f"summon naturalist:tiger {hx1+8} 65 {hz1+6} {{CustomName:'\"Tigre de Bengala\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:tiger {hx1+14} 65 {hz2-5} {{CustomName:'\"Tigre Siberiano\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:komodo_dragon {hx2-5} 65 {hz1+6} {{CustomName:'\"Dragao de Komodo\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:komodo_dragon {hx2-8} 65 {hz2-5} {{CustomName:'\"Varanus Rex\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 3: PÂNTANO DOS JACARÉS & COBRAS (Southwest: X: -267 to -243, Z: 63 to 84)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -267, -243, 63, 84
    # Deep swamp lagoon (Y=63..64)
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} mud replace")
    cmds.append(f"fill {hx1+4} 63 {hz1+4} {hx2-4} 64 {hz2-4} water replace")
    cmds.append(f"setblock {hx1+6} 65 {hz1+7} lily_pad replace")
    cmds.append(f"setblock {hx1+10} 65 {hz1+9} lily_pad replace")
    cmds.append(f"setblock {hx2-8} 65 {hz2-6} lily_pad replace")
    # Mangrove/mangue roots and fallen mossy log
    cmds.append(f"fill {hx1+5} 65 {hz2-6} {hx1+11} 65 {hz2-6} mangrove_log[axis=x] replace")
    # Viewing window facing catwalk (Z: 63)
    cmds.append(f"fill {hx1+2} 66 63 {hx2-2} 68 63 tinted_glass replace")
    # Residents
    cmds.append(f"summon naturalist:allator naturalist:alligator {hx1+7} 65 {hz1+6} {{CustomName:'\"Jacare Imperial\"',PersistenceRequired:1b}}".replace("naturalist:allator ", ""))
    cmds.append(f"summon naturalist:alligator {hx2-7} 65 {hz2-6} {{CustomName:'\"Crocodilo do Nilo\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:snake {hx1+12} 65 {hz1+4} {{CustomName:'\"Cascavel Negra\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:snake {hx2-5} 65 {hz1+5} {{CustomName:'\"Cobra Coral\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:snake {hx1+4} 65 {hz2-4} {{CustomName:'\"Vibora Venenosa\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 4: MONTANHA DOS URSOS & LOBOS (Southeast: X: -239 to -217, Z: 63 to 84)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -239, -217, 63, 84
    # Mountain terrain with stone cave and spruce trees
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} grass_block replace")
    cmds.append(f"fill {hx1+3} 64 {hz1+4} {hx1+8} 64 {hz2-4} stone replace")
    # Bear Cave
    cmds.append(f"fill {hx1+2} 65 {hz1+4} {hx1+6} 67 {hz1+9} stone hollow")
    cmds.append(f"fill {hx1+6} 65 {hz1+6} {hx1+6} 66 {hz1+7} air replace") # Cave mouth
    # Mountain stream
    cmds.append(f"fill {hx1+8} 64 {hz1+6} {hx2-4} 64 {hz1+8} water replace")
    # Spruce trees
    bx, bz = hx2 - 5, hz2 - 5
    cmds.append(f"fill {bx} 65 {bz} {bx} 71 {bz} spruce_log replace")
    cmds.append(f"fill {bx-2} 69 {bz-2} {bx+2} 72 {bz+2} spruce_leaves replace")
    # Viewing window facing catwalk (Z: 63)
    cmds.append(f"fill {hx1+2} 66 63 {hx2-2} 68 63 tinted_glass replace")
    # Residents
    cmds.append(f"summon naturalist:bear {hx1+8} 65 {hz1+10} {{CustomName:'\"Urso Pardo Grizzly\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:bear {hx1+14} 65 {hz2-6} {{CustomName:'\"Urso Selvagem\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:polar_bear {hx2-6} 65 {hz1+6} {{CustomName:'\"Urso Polar\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:wolf {hx2-8} 65 {hz2-5} {{CustomName:'\"Lobo Alfa\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:wolf {hx2-5} 65 {hz2-7} {{CustomName:'\"Lobo Cinzento\"',PersistenceRequired:1b}}")

    # Welcome Banner & Sign at Catwalk Entrance
    cmds.append("setblock -214 68 60 lantern[hanging=false] replace")
    cmds.append("setblock -214 67 63 red_banner replace")
    cmds.append("setblock -214 67 57 red_banner replace")

    return cmds

def main():
    print("[*] Generating Apex Predator Sanctuary commands...")
    cmds = build_commands()
    print(f"[*] Total commands: {len(cmds)}")

    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# Apex Predator Sanctuary Blueprint\n")
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Saved to {FUNCTION_PATH}")

    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Construindo o Santuario de Predadores de Alta Seguranca com passarela suspensa...","color":"gold","bold":true}]')

    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)

    print("[*] Executing /function blueprints:build_predator_sanctuary...")
    t0 = time.time()
    res = run_cmd("function blueprints:build_predator_sanctuary")
    elapsed = time.time() - t0
    print(f"[*] Finished in {elapsed:.2f}s: {res}")

    # Teleport Pace directly to the entrance of the elevated catwalk
    run_cmd("tp Pace -214 67 60 90 0")
    run_cmd("execute as @a at pace run playsound minecraft:entity.ender_dragon.growl master @s ~ ~ ~ 0.8 1.4")
    run_cmd("execute as @a at pace run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Santuario de Apex Predadores CONCLUIDO! Suba a escadaria e caminhe pela passarela suspensa!","color":"green","bold":true}]')
    print("[*] Done!")

if __name__ == "__main__":
    main()
