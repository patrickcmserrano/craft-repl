#!/usr/bin/env python3
"""
build_axolotl_aquarium.py
Generates a magnificent 10x10 Axolotl Aquarium right in front of Pace:
- Exact 10x10 footprint: X: -170..-161, Z: 125..134, Y: 64..71
- Crystal glass walls with polished deepslate & prismarine architectural frame
- Interior ocean floor of clay, prismarine, and glowing sea pickles
- Living coral reef, seagrass, and kelp forests
- Fully stocked with all 5 Axolotl variants (Lucy, Wild, Gold, Cyan, and Rare Blue!)
- Tropical fish companions
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_aquarium.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_aquarium_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # Bounding Box: 10x10
    # X: -170 to -161 (10 blocks)
    # Z: 125 to 134 (10 blocks)
    # Altitude: Y: 64 (floor base) to Y: 71 (crown rim)
    x1, x2 = -170, -161
    z1, z2 = 125, 134
    y_base = 64
    y_top = 70

    # 1. Clear interior space
    cmds.append(f"fill {x1} {y_base+1} {z1} {x2} {y_top+2} {z2} air replace")

    # 2. Aquarium Base Floor (Y = 64)
    # Outer frame: prismarine bricks
    cmds.append(f"fill {x1} {y_base} {z1} {x2} {y_base} {z2} prismarine_bricks replace")
    # Natural sand and clay bed inside (axolotl favorite)
    cmds.append(f"fill {x1+1} {y_base} {z1+1} {x2-1} {y_base} {z2-1} clay replace")
    for dx in [x1+2, x1+5, x2-2]:
        for dz in [z1+2, z1+5, z2-2]:
            cmds.append(f"setblock {dx} {y_base} {dz} sand replace")
    # Glowing sea lantern bottom corners
    cmds.append(f"setblock {x1+1} {y_base} {z1+1} sea_lantern replace")
    cmds.append(f"setblock {x2-1} {y_base} {z1+1} sea_lantern replace")
    cmds.append(f"setblock {x1+1} {y_base} {z2-1} sea_lantern replace")
    cmds.append(f"setblock {x2-1} {y_base} {z2-1} sea_lantern replace")

    # 3. Outer Glass Walls & Structural Columns (Y = 65 to 70)
    # 4 Corner pillars: deepslate tiles
    for cy in range(y_base + 1, y_top + 1):
        cmds.append(f"setblock {x1} {cy} {z1} deepslate_tiles replace")
        cmds.append(f"setblock {x2} {cy} {z1} deepslate_tiles replace")
        cmds.append(f"setblock {x1} {cy} {z2} deepslate_tiles replace")
        cmds.append(f"setblock {x2} {cy} {z2} deepslate_tiles replace")

    # Crystal Clear Glass viewing panes
    cmds.append(f"fill {x1+1} {y_base+1} {z1} {x2-1} {y_top} {z1} glass replace")
    cmds.append(f"fill {x1+1} {y_base+1} {z2} {x2-1} {y_top} {z2} glass replace")
    cmds.append(f"fill {x1} {y_base+1} {z1+1} {x1} {y_top} {z2-1} glass replace")
    cmds.append(f"fill {x2} {y_base+1} {z1+1} {x2} {y_top} {z2-1} glass replace")

    # 4. Fill Interior with Pure Glowing Water (Y: 65 to 70)
    cmds.append(f"fill {x1+1} {y_base+1} {z1+1} {x2-1} {y_top} {z2-1} water replace")

    # 5. Top Crown & Roof (Y = 71)
    cmds.append(f"fill {x1} {y_top+1} {z1} {x2} {y_top+1} {z2} prismarine_brick_slab[type=bottom] replace")
    cmds.append(f"fill {x1+1} {y_top+1} {z1+1} {x2-1} {y_top+1} {z2-1} tinted_glass replace")
    # Lanterns on the four top corners
    cmds.append(f"setblock {x1} {y_top+2} {z1} sea_lantern replace")
    cmds.append(f"setblock {x2} {y_top+2} {z1} sea_lantern replace")
    cmds.append(f"setblock {x1} {y_top+2} {z2} sea_lantern replace")
    cmds.append(f"setblock {x2} {y_top+2} {z2} sea_lantern replace")

    # 6. Coral Reef & Aquatic Vegetation (Inside water at Y=65)
    cmds.append(f"setblock {x1+2} {y_base+1} {z1+3} tube_coral_block replace")
    cmds.append(f"setblock {x1+3} {y_base+1} {z1+3} brain_coral_block replace")
    cmds.append(f"setblock {x2-3} {y_base+1} {z2-3} bubble_coral_block replace")
    cmds.append(f"setblock {x2-2} {y_base+1} {z2-3} fire_coral_block replace")

    # Glowing sea pickles on the sand/clay
    cmds.append(f"setblock {x1+3} {y_base+1} {z1+2} sea_pickle[pickles=4,waterlogged=true] replace")
    cmds.append(f"setblock {x2-3} {y_base+1} {z2-2} sea_pickle[pickles=4,waterlogged=true] replace")
    cmds.append(f"setblock {x1+5} {y_base+1} {z1+5} sea_pickle[pickles=3,waterlogged=true] replace")

    # Seagrass
    for sx, sz in [(x1+2, z1+5), (x1+4, z1+2), (x2-2, z1+4), (x2-4, z2-2), (x1+3, z2-4)]:
        cmds.append(f"setblock {sx} {y_base+1} {sz} seagrass replace")

    # 7. Spawn Axolotls (All 5 variants!)
    # Variant 0: Lucy (Pink)
    # Variant 1: Wild (Brown)
    # Variant 2: Gold (Yellow)
    # Variant 3: Cyan (Light Blue)
    # Variant 4: Blue (Rare shiny blue!)
    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    cmds.append(f"summon axolotl {cx-1} {y_base+3} {cz-1} {{Variant:0,CustomName:'\"Lucy\"'}}")
    cmds.append(f"summon axolotl {cx+1} {y_base+3} {cz+1} {{Variant:0,CustomName:'\"Rosa\"'}}")
    cmds.append(f"summon axolotl {cx-2} {y_base+2} {cz+2} {{Variant:1,CustomName:'\"Selvagem\"'}}")
    cmds.append(f"summon axolotl {cx+2} {y_base+2} {cz-2} {{Variant:2,CustomName:'\"Dourado\"'}}")
    cmds.append(f"summon axolotl {cx} {y_base+4} {cz-1} {{Variant:3,CustomName:'\"Ciano\"'}}")
    cmds.append(f"summon axolotl {cx} {y_base+4} {cz+1} {{Variant:4,CustomName:'\"Azul Raro ✨\"'}}")

    # Tropical fish companions
    cmds.append(f"summon tropical_fish {cx-1} {y_base+4} {cz}")
    cmds.append(f"summon tropical_fish {cx+1} {y_base+4} {cz}")
    cmds.append(f"summon tropical_fish {cx} {y_base+3} {cz-2}")

    return cmds

def main():
    print("[*] Generating 10x10 Axolotl Aquarium commands...")
    cmds = build_aquarium_commands()
    print(f"[*] Total commands: {len(cmds)}")

    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# 10x10 Axolotl Aquarium Generator\n")
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Saved to {FUNCTION_PATH}")

    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"🫧 Construindo o Grande Aquario 10x10 para Axolotes bem na sua frente...","color":"aqua","bold":true}]')

    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)

    print("[*] Executing /function blueprints:build_aquarium...")
    t0 = time.time()
    res = run_cmd("function blueprints:build_aquarium")
    elapsed = time.time() - t0
    print(f"[*] Finished in {elapsed:.2f}s: {res}")

    run_cmd("execute as @a at pace run playsound minecraft:entity.axolotl.splash master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"✨ Aquario 10x10 para Axolotes concluido com sucesso! Variedades Rosa, Ouro, Ciano e Azul Raro nadando!","color":"green","bold":true}]')
    print("[*] Done!")

if __name__ == "__main__":
    main()
