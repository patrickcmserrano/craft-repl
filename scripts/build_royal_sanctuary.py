#!/usr/bin/env python3
"""
build_royal_sanctuary.py
Transforms the south courtyard of the Gothic Citadel into an Organized Royal Wildlife Sanctuary:
- Complete removal of any stray animals outside the habitats
- 4 High-Security Themed Habitats (3-block high barriers prevent any escape):
  1. Savanna Imperial (Elephants, Giraffes, Zebras, Ostriches)
  2. Bosque das Capivaras (Capybaras, Deer, Ducks, Hedgehogs, Armadillos with swimming lagoon)
  3. Tundra Pré-Histórica (Mammoths, Tortoises, Boars, Snow Foxes)
  4. Reserva Oriental (Camels, Pandas, Sniffers with bamboo forest)
- Central Paved Avenues, Victorian Lamp Posts, Park Benches & Central Fountain
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_sanctuary.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_sanctuary_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # 1. Clean kill of all loose animals (except players, item frames, armor stands, axolotls)
    mobs_to_clear = [
        "naturalist:elephant", "naturalist:mammoth", "naturalist:giraffe",
        "naturalist:zebra", "naturalist:deer", "naturalist:boar",
        "naturalist:ostrich", "naturalist:tortoise", "naturalist:duck",
        "naturalist:hedgehog", "naturalist:lizard", "naturalist:crab",
        "naturalist:snail", "naturalist:bird", "naturalist:butterfly",
        "naturalist:firefly", "naturalist:dragonfly", "naturalist:capybara",
        "minecraft:camel", "minecraft:sniffer", "minecraft:panda",
        "minecraft:fox", "minecraft:armadillo", "minecraft:frog", "minecraft:allay"
    ]
    for m in mobs_to_clear:
        cmds.append(f"kill @e[type={m}]")
    cmds.append("kill @e[type=item]")

    # 2. Clear sanctuary air envelope (Y: 65 to 76)
    cmds.append("fill -215 65 164 -135 76 197 air replace")
    # Base ground level: lush grass
    cmds.append("fill -215 64 164 -135 64 197 grass_block replace")

    # 3. Grand Paved Pedestrian Avenues (Promenades)
    # North-South Central Promenade (X: -177 to -173, Z: 164 to 197)
    cmds.append("fill -177 64 164 -173 64 197 stone_bricks replace")
    cmds.append("fill -176 64 164 -174 64 197 mossy_stone_bricks replace")
    cmds.append("fill -178 64 164 -178 64 197 polished_deepslate replace")
    cmds.append("fill -172 64 164 -172 64 197 polished_deepslate replace")

    # East-West Cross Promenade (X: -215 to -135, Z: 179 to 182)
    cmds.append("fill -215 64 179 -135 64 182 stone_bricks replace")
    cmds.append("fill -215 64 180 -135 64 181 mossy_stone_bricks replace")
    cmds.append("fill -215 64 178 -135 64 178 polished_deepslate replace")
    cmds.append("fill -215 64 183 -135 64 183 polished_deepslate replace")

    # Central Plaza Fountain at intersection [-177..-173, 179..182]
    cmds.append("fill -177 64 179 -173 65 182 polished_deepslate replace")
    cmds.append("fill -176 65 180 -174 65 181 sea_lantern replace")
    cmds.append("fill -176 66 180 -174 66 181 water replace")
    cmds.append("setblock -175 67 180 sea_lantern replace")
    cmds.append("setblock -175 68 180 water replace")

    # Victorian Lamp Posts along promenades
    for lz in [168, 174, 187, 193]:
        for lx in [-178, -172]:
            cmds.append(f"setblock {lx} 65 {lz} polished_deepslate_wall replace")
            cmds.append(f"setblock {lx} 66 {lz} iron_bars replace")
            cmds.append(f"setblock {lx} 67 {lz} lantern[hanging=false] replace")

    for lx in [-205, -195, -155, -145]:
        for lz in [178, 183]:
            cmds.append(f"setblock {lx} 65 {lz} polished_deepslate_wall replace")
            cmds.append(f"setblock {lx} 66 {lz} iron_bars replace")
            cmds.append(f"setblock {lx} 67 {lz} lantern[hanging=false] replace")

    # Park Benches along central promenade
    for bz in [171, 190]:
        cmds.append(f"setblock -177 65 {bz} dark_oak_stairs[facing=west,half=bottom] replace")
        cmds.append(f"setblock -173 65 {bz} dark_oak_stairs[facing=east,half=bottom] replace")

    # -------------------------------------------------------------------------
    # HABITAT 1: SAVANA REAL (Southeast: X: -171 to -136, Z: 184 to 196)
    # 3-Block High Containment (Stone Wall + 2 Dark Oak Fences)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -171, -136, 184, 196
    # Layer 1: Stone Brick Wall (Y=65)
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 65 {hz1} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz2} {hx2} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz1} {hx1} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx2} 65 {hz1} {hx2} 65 {hz2} stone_brick_wall replace")
    # Layers 2 & 3: Dark Oak Fences (Y=66, 67)
    cmds.append(f"fill {hx1} 66 {hz1} {hx2} 67 {hz1} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz2} {hx2} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx1} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx2} 66 {hz1} {hx2} 67 {hz2} dark_oak_fence replace")
    # Corner Watch Pillars
    for px, pz in [(hx1, hz1), (hx2, hz1), (hx1, hz2), (hx2, hz2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 68 {pz} stone_bricks replace")
        cmds.append(f"setblock {px} 69 {pz} lantern[hanging=false] replace")
    # Gate on West side
    cmds.append(f"setblock {hx1} 65 {hz1+5} dark_oak_fence_gate[facing=west] replace")
    cmds.append(f"setblock {hx1} 66 {hz1+5} dark_oak_fence_gate[facing=west] replace")
    cmds.append(f"setblock {hx1} 67 {hz1+5} dark_oak_fence replace")

    # Savanna Interior: acacia trees, water pool, hay bales, sand & coarse dirt
    cmds.append(f"fill {hx1+2} 64 {hz1+2} {hx1+12} 64 {hz1+6} coarse_dirt replace")
    cmds.append(f"fill {hx2-10} 64 {hz2-6} {hx2-2} 64 {hz2-2} sand replace")
    # Water basin
    cmds.append(f"fill {hx2-8} 64 {hz1+3} {hx2-4} 64 {hz1+7} water replace")
    # Acacia shade tree
    ax, az = hx1 + 16, hz1 + 6
    cmds.append(f"fill {ax} 65 {az} {ax} 70 {az} acacia_log replace")
    cmds.append(f"fill {ax-2} 70 {az-2} {ax+2} 71 {az+2} acacia_leaves replace")
    # Hay bale feeding station
    cmds.append(f"fill {hx1+4} 65 {hz2-3} {hx1+7} 65 {hz2-3} hay_block replace")
    cmds.append(f"setblock {hx1+5} 66 {hz2-3} hay_block replace")
    # Residents
    cmds.append(f"summon naturalist:elephant {hx1+10} 65 {hz1+6} {{CustomName:'\"Elefante Real\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:elephant {hx1+14} 65 {hz1+8} {{CustomName:'\"Elefante Majestoso\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:giraffe {hx2-10} 65 {hz1+6} {{CustomName:'\"Girafa Alta\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:giraffe {hx2-6} 65 {hz1+8} {{CustomName:'\"Girafa Graciosa\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:zebra {hx1+7} 65 {hz2-5} {{CustomName:'\"Zebra das Planicies\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:zebra {hx1+10} 65 {hz2-4} {{CustomName:'\"Zebra Rajada\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:ostrich {hx2-5} 65 {hz2-4} {{CustomName:'\"Avestruz Veloz\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 2: BOSQUE DAS CAPIVARAS & CERVOS (Southwest: X: -214 to -179, Z: 184 to 196)
    # 3-Block High Containment
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -214, -179, 184, 196
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 65 {hz1} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz2} {hx2} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz1} {hx1} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx2} 65 {hz1} {hx2} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx2} 67 {hz1} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz2} {hx2} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx1} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx2} 66 {hz1} {hx2} 67 {hz2} dark_oak_fence replace")
    for px, pz in [(hx1, hz1), (hx2, hz1), (hx1, hz2), (hx2, hz2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 68 {pz} stone_bricks replace")
        cmds.append(f"setblock {px} 69 {pz} lantern[hanging=false] replace")
    # Gate on East side
    cmds.append(f"setblock {hx2} 65 {hz1+5} dark_oak_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 66 {hz1+5} dark_oak_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 67 {hz1+5} dark_oak_fence replace")

    # Large Capybara Swimming Lagoon (X: -206 to -192, Z: 186 to 194, Depth 2)
    cmds.append("fill -206 63 186 -192 63 194 clay replace")
    cmds.append("fill -206 64 186 -192 64 194 water replace")
    cmds.append("setblock -204 65 188 lily_pad replace")
    cmds.append("setblock -200 65 190 lily_pad replace")
    cmds.append("setblock -196 65 189 lily_pad replace")
    # Shady oak tree
    ox, oz = hx1 + 25, hz1 + 7
    cmds.append(f"fill {ox} 65 {oz} {ox} 70 {oz} oak_log replace")
    cmds.append(f"fill {ox-2} 69 {oz-2} {ox+2} 71 {oz+2} flowering_azalea_leaves replace")
    # Wild flowers
    for fx, fz, fl in [(hx1+3, hz1+3, "poppy"), (hx1+4, hz2-3, "cornflower"), (hx2-5, hz2-4, "allium")]:
        cmds.append(f"setblock {fx} 65 {fz} {fl} replace")
    # Residents
    cmds.append(f"summon naturalist:capybara {hx1+14} 65 {hz1+6} {{CustomName:'\"Capivara Fofinha\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:capybara {hx1+17} 65 {hz1+8} {{CustomName:'\"Capivara Nadadora\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:capybara {hx1+12} 65 {hz2-4} {{CustomName:'\"Capivarinha Filhote\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:deer {hx2-8} 65 {hz1+6} {{CustomName:'\"Cervo Real\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:deer {hx2-5} 65 {hz1+8} {{CustomName:'\"Cerva da Floresta\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:duck {hx1+15} 65 {hz1+6} {{CustomName:'\"Patinho Amarelo\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:duck {hx1+18} 65 {hz1+7} {{CustomName:'\"Pato Selvagem\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:hedgehog {hx2-6} 65 {hz2-4} {{CustomName:'\"Ourico Espinhudo\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:armadillo {hx1+4} 65 {hz2-3} {{CustomName:'\"Tatu-Bola\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 3: TUNDRA PRÉ-HISTÓRICA DOS MAMUTES (Northwest: X: -214 to -179, Z: 165 to 177)
    # 3-Block High Containment (Deepslate Tile Wall + 2 Spruce Fences)
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -214, -179, 165, 177
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 65 {hz1} deepslate_tile_wall replace")
    cmds.append(f"fill {hx1} 65 {hz2} {hx2} 65 {hz2} deepslate_tile_wall replace")
    cmds.append(f"fill {hx1} 65 {hz1} {hx1} 65 {hz2} deepslate_tile_wall replace")
    cmds.append(f"fill {hx2} 65 {hz1} {hx2} 65 {hz2} deepslate_tile_wall replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx2} 67 {hz1} spruce_fence replace")
    cmds.append(f"fill {hx1} 66 {hz2} {hx2} 67 {hz2} spruce_fence replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx1} 67 {hz2} spruce_fence replace")
    cmds.append(f"fill {hx2} 66 {hz1} {hx2} 67 {hz2} spruce_fence replace")
    for px, pz in [(hx1, hz1), (hx2, hz1), (hx1, hz2), (hx2, hz2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 68 {pz} polished_deepslate replace")
        cmds.append(f"setblock {px} 69 {pz} soul_lantern[hanging=false] replace")
    cmds.append(f"setblock {hx2} 65 {hz1+6} spruce_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 66 {hz1+6} spruce_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 67 {hz1+6} spruce_fence replace")

    # Tundra ground: snow blocks & packed ice
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} snow_block replace")
    cmds.append(f"fill {hx1+6} 64 {hz1+3} {hx1+14} 64 {hz1+8} packed_ice replace")
    # Evergreen pine tree
    sx, sz = hx1 + 18, hz1 + 5
    cmds.append(f"fill {sx} 65 {sz} {sx} 71 {sz} spruce_log replace")
    cmds.append(f"fill {sx-2} 69 {sz-2} {sx+2} 72 {sz+2} spruce_leaves replace")
    # Residents
    cmds.append(f"summon naturalist:mammoth {hx1+10} 65 {hz1+6} {{CustomName:'\"Mamute Ancestral\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:mammoth {hx2-10} 65 {hz1+6} {{CustomName:'\"Mamute das Neves\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:tortoise {hx1+5} 65 {hz2-4} {{CustomName:'\"Tartaruga Gigante\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:boar {hx2-5} 65 {hz2-4} {{CustomName:'\"Javali Selvagem\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:fox {hx1+14} 65 {hz2-3} {{CustomName:'\"Raposa das Neves\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # HABITAT 4: RESERVA ORIENTAL & BAMBU (Northeast: X: -171 to -136, Z: 165 to 177)
    # 3-Block High Containment
    # -------------------------------------------------------------------------
    hx1, hx2, hz1, hz2 = -171, -136, 165, 177
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 65 {hz1} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz2} {hx2} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 65 {hz1} {hx1} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx2} 65 {hz1} {hx2} 65 {hz2} stone_brick_wall replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx2} 67 {hz1} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz2} {hx2} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx1} 67 {hz2} dark_oak_fence replace")
    cmds.append(f"fill {hx2} 66 {hz1} {hx2} 67 {hz2} dark_oak_fence replace")
    for px, pz in [(hx1, hz1), (hx2, hz1), (hx1, hz2), (hx2, hz2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 68 {pz} stone_bricks replace")
        cmds.append(f"setblock {px} 69 {pz} lantern[hanging=false] replace")
    cmds.append(f"setblock {hx1} 65 {hz1+6} dark_oak_fence_gate[facing=west] replace")
    cmds.append(f"setblock {hx1} 66 {hz1+6} dark_oak_fence_gate[facing=west] replace")
    cmds.append(f"setblock {hx1} 67 {hz1+6} dark_oak_fence replace")

    # Bamboo grove
    for bx in range(hx1+4, hx1+16, 2):
        for bz in range(hz1+3, hz1+7, 2):
            cmds.append(f"setblock {bx} 65 {bz} bamboo[age=1,stage=0] replace")
    # Sand dune for camel
    cmds.append(f"fill {hx2-8} 64 {hz1+3} {hx2-3} 64 {hz1+8} sand replace")
    # Residents
    cmds.append(f"summon minecraft:camel {hx2-6} 65 {hz1+5} {{CustomName:'\"Camelo Real\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:panda {hx1+8} 65 {hz1+5} {{CustomName:'\"Panda Bambu\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:panda {hx1+12} 65 {hz1+5} {{CustomName:'\"Panda Alegre\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:sniffer {hx2-6} 65 {hz2-4} {{CustomName:'\"Sniffer Ancestral\"',PersistenceRequired:1b}}")

    # -------------------------------------------------------------------------
    # WELCOME PORTAL & SIGNAGE (Entrance archway at X: -178..-172, Z: 164)
    # -------------------------------------------------------------------------
    cmds.append("fill -178 65 164 -178 68 164 stone_bricks replace")
    cmds.append("fill -172 65 164 -172 68 164 stone_bricks replace")
    cmds.append("fill -178 69 164 -172 69 164 deepslate_tile_slab[type=bottom] replace")
    cmds.append("setblock -177 68 164 stone_brick_stairs[facing=east,half=top] replace")
    cmds.append("setblock -173 68 164 stone_brick_stairs[facing=west,half=top] replace")
    cmds.append("setblock -176 69 164 lantern[hanging=true] replace")
    cmds.append("setblock -174 69 164 lantern[hanging=true] replace")
    cmds.append("setblock -178 67 163 purple_banner replace")
    cmds.append("setblock -172 67 163 purple_banner replace")

    return cmds

def main():
    print("[*] Generating Royal Wildlife Sanctuary commands...")
    cmds = build_sanctuary_commands()
    print(f"[*] Total commands: {len(cmds)}")

    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# Royal Wildlife Sanctuary Blueprint\n")
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Saved to {FUNCTION_PATH}")

    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Limpando animais soltos e reconstruindo os cercados com barreiras de 3 blocos...","color":"gold","bold":true}]')

    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)

    print("[*] Executing /function blueprints:build_sanctuary...")
    t0 = time.time()
    res = run_cmd("function blueprints:build_sanctuary")
    elapsed = time.time() - t0
    print(f"[*] Finished in {elapsed:.2f}s: {res}")

    # Teleport Pace directly to the entrance promenade
    run_cmd("tp Pace -175 65 162 0 0")
    run_cmd("execute as @a at pace run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Santuario Reconstruido! Animais soltos removidos e cada especie trancada no seu habitat!","color":"green","bold":true}]')
    print("[*] Done!")

if __name__ == "__main__":
    main()
