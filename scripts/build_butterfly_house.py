#!/usr/bin/env python3
"""
build_butterfly_house.py
Generates a magnificent 10x10 Botanical Butterfly Sanctuary (Borboletário)
right at Pace's position:
- Bounding Box: X: -195..-186, Z: 107..116, Y: 64..72
- Greenhouse crystal glass walls & mossy stone brick Gothic pillars
- Lush living botanical garden floor: moss, flowering azaleas, podzol, glowing shroomlights under moss carpets
- Central reflection pond with lily pads & dripleaves
- Hanging spore blossoms, glow berries, and vines
- Double-door airlock entrance so butterflies never escape
- All 7 Naturalist butterfly species (28 butterflies total)
"""

import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_butterfly_house.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_butterfly_house_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # 10x10 Bounds:
    x1, x2 = -195, -186
    z1, z2 = 107, 116
    y_floor = 64
    y_walls = 70
    y_roof = 71

    # 1. Clear interior space
    cmds.append(f"fill {x1} {y_floor+1} {z1} {x2} {y_roof+1} {z2} air replace")

    # 2. Floor Base (Y = 64)
    # Outer foundation frame: polished deepslate & mossy stone bricks
    cmds.append(f"fill {x1} {y_floor} {z1} {x2} {y_floor} {z2} mossy_stone_bricks replace")
    # Lush moss, podzol, and rooted dirt mosaic
    cmds.append(f"fill {x1+1} {y_floor} {z1+1} {x2-1} {y_floor} {z2-1} moss_block replace")
    for px, pz in [(x1+2, z1+3), (x1+7, z1+2), (x2-2, z2-3), (x1+4, z2-2)]:
        cmds.append(f"setblock {px} {y_floor} {pz} podzol replace")
        cmds.append(f"setblock {px+1} {y_floor} {pz} rooted_dirt replace")

    # Hidden glowing shroomlights under moss carpets for warm under-glow
    for lx, lz in [(x1+2, z1+2), (x2-2, z1+2), (x1+2, z2-2), (x2-2, z2-2), (x1+4, z1+5)]:
        cmds.append(f"setblock {lx} {y_floor} {lz} shroomlight replace")
        cmds.append(f"setblock {lx} {y_floor+1} {lz} moss_carpet replace")

    # Flagstone Garden Path through the greenhouse
    for pz in range(z1+1, z2):
        cmds.append(f"setblock {x1+3} {y_floor} {pz} mossy_cobblestone replace")
    for px in range(x1+3, x2-1):
        cmds.append(f"setblock {px} {y_floor} {z1+5} mossy_cobblestone replace")

    # Central Miniature Lily Pond (X: -191..-190, Z: 111..112)
    cmds.append(f"fill {x1+4} {y_floor} {z1+4} {x1+5} {y_floor} {z1+5} water replace")
    cmds.append(f"setblock {x1+4} {y_floor+1} {z1+4} lily_pad replace")
    cmds.append(f"setblock {x1+5} {y_floor+1} {z1+5} small_dripleaf[facing=north] replace")

    # 3. Outer Glass Greenhouse Walls & Structural Columns (Y = 65 to 70)
    # 4 Corner Pillars: mossy stone bricks
    for cy in range(y_floor + 1, y_walls + 1):
        cmds.append(f"setblock {x1} {cy} {z1} mossy_stone_bricks replace")
        cmds.append(f"setblock {x2} {cy} {z1} mossy_stone_bricks replace")
        cmds.append(f"setblock {x1} {cy} {z2} mossy_stone_bricks replace")
        cmds.append(f"setblock {x2} {cy} {z2} mossy_stone_bricks replace")

    # Crystal Clear Glass Walls
    cmds.append(f"fill {x1+1} {y_floor+1} {z1} {x2-1} {y_walls} {z1} glass replace")
    cmds.append(f"fill {x1+1} {y_floor+1} {z2} {x2-1} {y_walls} {z2} glass replace")
    cmds.append(f"fill {x1} {y_floor+1} {z1+1} {x1} {y_walls} {z2-1} glass replace")
    cmds.append(f"fill {x2} {y_floor+1} {z1+1} {x2} {y_walls} {z2-1} glass replace")

    # 4. Botanical Airlock Entrance on South side (Z = 116, X = -192..-191)
    # Double Glass Door with dark oak frames so butterflies cannot slip out
    cmds.append(f"setblock {x1+3} {y_floor+1} {z2} dark_oak_door[facing=south,half=lower,hinge=left] replace")
    cmds.append(f"setblock {x1+3} {y_floor+2} {z2} dark_oak_door[facing=south,half=upper,hinge=left] replace")
    cmds.append(f"setblock {x1+3} {y_floor+1} {z2-1} stone_pressure_plate replace")
    cmds.append(f"setblock {x1+3} {y_floor+1} {z2+1} stone_pressure_plate replace")

    # 5. Greenhouse Glass Vault & Roof (Y = 71 to 72)
    # Tinted glass dome roof allows natural sunlight to filter down
    cmds.append(f"fill {x1} {y_roof} {z1} {x2} {y_roof} {z2} dark_oak_slab[type=bottom] replace")
    cmds.append(f"fill {x1+1} {y_roof} {z1+1} {x2-1} {y_roof} {z2-1} tinted_glass replace")
    # Corner Victorian Lanterns
    cmds.append(f"setblock {x1} {y_roof+1} {z1} lantern[hanging=false] replace")
    cmds.append(f"setblock {x2} {y_roof+1} {z1} lantern[hanging=false] replace")
    cmds.append(f"setblock {x1} {y_roof+1} {z2} lantern[hanging=false] replace")
    cmds.append(f"setblock {x2} {y_roof+1} {z2} lantern[hanging=false] replace")

    # 6. Hanging Botanical Flora from the ceiling (Y = 70)
    # Spore Blossoms emitting drifting magical green particles
    cmds.append(f"setblock {x1+4} {y_walls} {z1+4} spore_blossom replace")
    cmds.append(f"setblock {x1+6} {y_walls} {z1+7} spore_blossom replace")
    # Glowing cave vines
    cmds.append(f"setblock {x1+2} {y_walls} {z1+3} cave_vines[berries=true] replace")
    cmds.append(f"setblock {x2-2} {y_walls} {z2-3} cave_vines[berries=true] replace")
    cmds.append(f"setblock {x1+7} {y_walls} {z1+3} hanging_roots replace")
    cmds.append(f"setblock {x1+3} {y_walls} {z2-2} hanging_roots replace")

    # 7. Miniature Flowering Azalea & Cherry Tree (X: -189, Z: 110)
    cmds.append(f"setblock {x1+6} {y_floor+1} {z1+3} oak_log replace")
    cmds.append(f"setblock {x1+6} {y_floor+2} {z1+3} oak_log replace")
    cmds.append(f"setblock {x1+6} {y_floor+3} {z1+3} flowering_azalea_leaves replace")
    cmds.append(f"setblock {x1+5} {y_floor+3} {z1+3} flowering_azalea_leaves replace")
    cmds.append(f"setblock {x1+7} {y_floor+3} {z1+3} flowering_azalea_leaves replace")
    cmds.append(f"setblock {x1+6} {y_floor+3} {z1+2} flowering_azalea_leaves replace")
    cmds.append(f"setblock {x1+6} {y_floor+3} {z1+4} flowering_azalea_leaves replace")

    # 8. Vibrant Flower Garden (All favorite butterfly flowers)
    flowers = [
        (x1+2, z1+2, "peony[half=lower]"),
        (x1+2, z1+2, "peony[half=upper]", 1),
        (x1+2, z1+4, "rose_bush[half=lower]"),
        (x1+2, z1+4, "rose_bush[half=upper]", 1),
        (x1+2, z1+6, "lilac[half=lower]"),
        (x1+2, z1+6, "lilac[half=upper]", 1),
        (x1+6, z1+7, "sunflower[half=lower]"),
        (x1+6, z1+7, "sunflower[half=upper]", 1),
        (x1+5, z1+2, "allium"),
        (x1+7, z1+5, "blue_orchid"),
        (x1+7, z1+6, "pink_tulip"),
        (x1+4, z1+7, "cornflower"),
        (x1+6, z1+8, "oxeye_daisy"),
        (x1+5, z1+8, "flowering_azalea")
    ]
    for item in flowers:
        if len(item) == 4:
            fx, fz, ftype, dy = item
            cmds.append(f"setblock {fx} {y_floor+1+dy} {fz} {ftype} replace")
        else:
            fx, fz, ftype = item
            cmds.append(f"setblock {fx} {y_floor+1} {fz} {ftype} replace")

    # 9. Pink petals on the ground
    for px, pz in [(x1+5, z1+3), (x1+6, z1+4), (x1+7, z1+2)]:
        cmds.append(f"setblock {px} {y_floor+1} {pz} pink_petals[flower_amount=3] replace")

    # 10. SPAWN ALL 7 NATURALIST BUTTERFLY SPECIES
    # 4 of each species = 28 butterflies total fluttering inside
    butterfly_species = [
        ("naturalist:blue_morpho", "Morfo Azul"),
        ("naturalist:monarch", "Monarca"),
        ("naturalist:clouded_yellow", "Amarela-das-Nuvens"),
        ("naturalist:swallowtail", "Cauda-de-Andorinha Verde"),
        ("naturalist:jade_green_swallowtail", "Cauda-de-Andorinha Jade"),
        ("naturalist:purple_emperor", "Imperador Púrpura"),
        ("naturalist:red_admiral", "Almirante Vermelho")
    ]

    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    for variant_id, name in butterfly_species:
        for i in range(4):
            dx = (i % 2) * 2 - 1
            dz = (i // 2) * 2 - 1
            by = y_floor + 2 + (i % 3)
            cmds.append(
                f"summon naturalist:butterfly {cx+dx} {by} {cz+dz} "
                f"{{Variant:\"{variant_id}\",PersistenceRequired:1b,CustomName:'\"{name}\"'}}"
            )

    # 11. Welcome sound effect
    cmds.append(f"playsound minecraft:block.flowering_azalea.place block @a[distance=..20] {cx} {y_floor+2} {cz} 1 1")
    cmds.append(f"playsound minecraft:entity.experience_orb.pickup ambient @a[distance=..20] {cx} {y_floor+2} {cz} 1 1.2")

    return cmds

def main():
    cmds = build_butterfly_house_commands()
    with open(FUNCTION_PATH, "w") as f:
        f.write("\n".join(cmds) + "\n")

    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")
    res_reload = run_cmd("reload")
    print("[*] Reload result:", res_reload)
    time.sleep(0.3)
    res_func = run_cmd("function blueprints:build_butterfly_house")
    print("[+] Function execution result:", res_func)

if __name__ == "__main__":
    main()
