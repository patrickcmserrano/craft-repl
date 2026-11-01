#!/usr/bin/env python3
"""
rebuild_arctic_habitat.py
Completely reconstructs the Arctic Tundra Wildlife Habitat:
- Footprint: X: -214..-179, Z: 165..177, Y: 64..73
- 3-Block Reinforced Containment (Deepslate tile walls + Spruce fences + Corner soul lantern towers)
- Solid snow block ground + packed ice glacier pond + blue ice crystals
- 2 Snowy taiga spruce trees + sweet berry bushes
- Repopulates with:
  * 2x Mammoths
  * 1x Giant Tortoise
  * 2x Tundra Boars
  * 2x White Arctic Foxes
"""

import sys
import json
import time
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/rebuild_arctic.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_arctic_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    hx1, hx2 = -214, -179
    hz1, hz2 = 165, 177

    # 1. Clear interior air volume (Y: 65 to 74) to clean up any blast debris or loose blocks
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 74 {hz2} air replace")

    # 2. Solid Foundation Bed (Y = 64)
    # Foundation border
    cmds.append(f"fill {hx1} 64 {hz1} {hx2} 64 {hz1} polished_deepslate replace")
    cmds.append(f"fill {hx1} 64 {hz2} {hx2} 64 {hz2} polished_deepslate replace")
    cmds.append(f"fill {hx1} 64 {hz1} {hx1} 64 {hz2} polished_deepslate replace")
    cmds.append(f"fill {hx2} 64 {hz1} {hx2} 64 {hz2} polished_deepslate replace")

    # Snow block ground cover
    cmds.append(f"fill {hx1+1} 64 {hz1+1} {hx2-1} 64 {hz2-1} snow_block replace")

    # Frozen glacier lake: packed ice & blue ice
    cmds.append(f"fill {hx1+6} 64 {hz1+3} {hx1+18} 64 {hz1+9} packed_ice replace")
    cmds.append(f"fill {hx1+8} 64 {hz1+4} {hx1+15} 64 {hz1+7} blue_ice replace")

    # 3. Reinforced Containment Walls (3 blocks high)
    # Layer 1: Deepslate Tile Wall (Y=65)
    cmds.append(f"fill {hx1} 65 {hz1} {hx2} 65 {hz1} deepslate_tile_wall replace")
    cmds.append(f"fill {hx1} 65 {hz2} {hx2} 65 {hz2} deepslate_tile_wall replace")
    cmds.append(f"fill {hx1} 65 {hz1} {hx1} 65 {hz2} deepslate_tile_wall replace")
    cmds.append(f"fill {hx2} 65 {hz1} {hx2} 65 {hz2} deepslate_tile_wall replace")

    # Layers 2 & 3: Spruce Fences (Y=66, 67)
    cmds.append(f"fill {hx1} 66 {hz1} {hx2} 67 {hz1} spruce_fence replace")
    cmds.append(f"fill {hx1} 66 {hz2} {hx2} 67 {hz2} spruce_fence replace")
    cmds.append(f"fill {hx1} 66 {hz1} {hx1} 67 {hz2} spruce_fence replace")
    cmds.append(f"fill {hx2} 66 {hz1} {hx2} 67 {hz2} spruce_fence replace")

    # 4 Corner Pillars with Soul Lanterns
    for px, pz in [(hx1, hz1), (hx2, hz1), (hx1, hz2), (hx2, hz2)]:
        cmds.append(f"fill {px} 65 {pz} {px} 68 {pz} polished_deepslate replace")
        cmds.append(f"setblock {px} 69 {pz} soul_lantern[hanging=false] replace")

    # Gate on East Promenade side (X: -179, Z: 171)
    cmds.append(f"setblock {hx2} 65 {hz1+6} spruce_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 66 {hz1+6} spruce_fence_gate[facing=east] replace")
    cmds.append(f"setblock {hx2} 67 {hz1+6} spruce_fence replace")

    # 4. Glacier Ice Formations (Small Natural Ice Spikes)
    cmds.append(f"fill {hx1+10} 65 {hz1+5} {hx1+10} 67 {hz1+5} packed_ice replace")
    cmds.append(f"setblock {hx1+10} 68 {hz1+5} blue_ice replace")
    cmds.append(f"fill {hx1+14} 65 {hz1+7} {hx1+14} 66 {hz1+7} packed_ice replace")

    # 5. Grand Evergreen Taiga Trees
    # Tree 1: West side (X: -195, Z: 170)
    tx1, tz1 = hx1 + 19, hz1 + 5
    cmds.append(f"fill {tx1} 65 {tz1} {tx1} 71 {tz1} spruce_log replace")
    cmds.append(f"fill {tx1-2} 68 {tz1-2} {tx1+2} 69 {tz1+2} spruce_leaves replace")
    cmds.append(f"fill {tx1-1} 70 {tz1-1} {tx1+1} 71 {tz1+1} spruce_leaves replace")
    cmds.append(f"setblock {tx1} 72 {tz1} spruce_leaves replace")

    # Tree 2: North side (X: -205, Z: 173)
    tx2, tz2 = hx1 + 9, hz1 + 8
    cmds.append(f"fill {tx2} 65 {tz2} {tx2} 70 {tz2} spruce_log replace")
    cmds.append(f"fill {tx2-2} 67 {tz2-2} {tx2+2} 68 {tz2+2} spruce_leaves replace")
    cmds.append(f"fill {tx2-1} 69 {tz2-1} {tx2+1} 70 {tz2+1} spruce_leaves replace")
    cmds.append(f"setblock {tx2} 71 {tz2} spruce_leaves replace")

    # Sweet berry bushes for foraging
    cmds.append(f"setblock {hx1+4} 65 {hz1+3} sweet_berry_bush[age=3] replace")
    cmds.append(f"setblock {hx1+5} 65 {hz2-3} sweet_berry_bush[age=3] replace")
    cmds.append(f"setblock {hx2-4} 65 {hz1+3} sweet_berry_bush[age=3] replace")

    # Natural snow layers
    for sx, sz in [(hx1+3, hz1+4), (hx1+7, hz2-2), (hx2-3, hz1+4), (hx2-6, hz2-3), (hx1+12, hz2-2)]:
        cmds.append(f"setblock {sx} 65 {sz} snow[layers=2] replace")

    # 6. Repopulate Arctic Fauna (All with PersistenceRequired)
    cmds.append(f"summon naturalist:mammoth {hx1+11} 65 {hz1+5} {{CustomName:'\"Mamute Ancestral\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:mammoth {hx2-9} 65 {hz1+5} {{CustomName:'\"Mamute das Neves\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:tortoise {hx1+6} 65 {hz2-4} {{CustomName:'\"Tartaruga Gigante do Gelo\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:boar {hx2-5} 65 {hz2-4} {{CustomName:'\"Javali Polar\"',PersistenceRequired:1b}}")
    cmds.append(f"summon naturalist:boar {hx2-8} 65 {hz2-4} {{CustomName:'\"Javali das Neves\"',PersistenceRequired:1b}}")
    # 2 White Snow Foxes
    cmds.append(f"summon minecraft:fox {hx1+15} 65 {hz2-3} {{Type:\"snow\",CustomName:'\"Raposinha do Ártico\"',PersistenceRequired:1b}}")
    cmds.append(f"summon minecraft:fox {hx1+17} 65 {hz2-3} {{Type:\"snow\",CustomName:'\"Raposa das Neves\"',PersistenceRequired:1b}}")

    # Sound effects
    cmds.append(f"playsound minecraft:block.snow.place block @a[distance=..30] {hx1+18} 65 {hz1+6} 1 0.8")
    cmds.append(f"playsound minecraft:entity.player.levelup ambient @a[distance=..30] {hx1+18} 65 {hz1+6} 1 1.2")

    return cmds

def main():
    cmds = build_arctic_commands()
    with open(FUNCTION_PATH, "w") as f:
        f.write("\n".join(cmds) + "\n")

    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")
    res_reload = run_cmd("reload")
    print("[*] Reload result:", res_reload)
    time.sleep(0.3)
    res_func = run_cmd("function blueprints:rebuild_arctic")
    print("[+] Function execution result:", res_func)

if __name__ == "__main__":
    main()
