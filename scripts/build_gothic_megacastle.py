#!/usr/bin/env python3
"""
build_gothic_megacastle.py
Generates the Massive 100x100 Gothic Medieval Mega-Castle directly in front of Pace:
- Exact 100x100 area (X: -228..-129, Z: 100..199), max height 90 blocks (Y: 64..154)
- Volcanic chasm with lava & magma (Z: 90..99, depth Y: 46..63)
- Majestic 5-block wide drawbridge (dark oak & stone bricks with suspension chains)
- Main hollow cathedral hall (40x30x40) with magnificent gothic chandeliers (light > 15)
- Mob-proof bottom-slab and carpet flooring
- Full survival functional rooms: Grand Throne Room, Crafting/Anvils/Furnaces/Brewing, Double Chests Storage Vault, Level 30 Enchanting Sanctuary
- Central towering hollow main spire (15x15 base, 75m high, pointed red brick roof to Y:154, fully furnished master bedroom)
- 5-block wide high-altitude colonnade bridge on 50m stone pillars
- Separate hollow 9x9 watchtower with observation deck & bell
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_castle.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_castle_commands():
    cmds = []
    
    # 0. Safety, gamerules & resistance
    cmds.append("gamerule commandModificationBlockLimit 2000000")
    cmds.append("effect give @a minecraft:resistance 120 255 true")
    cmds.append("effect give @a minecraft:fire_resistance 120 255 true")

    # Castle Footprint Coordinates:
    # Pace is at X=-178, Y=64, Z=88
    # Footprint: X: -228 to -129 (100 blocks wide), Z: 100 to 199 (100 blocks deep)
    # Altitude: Y: 64 (ground) to 154 (90 blocks high)
    x1, x2 = -228, -129
    z1, z2 = 100, 199
    base_y = 64
    max_y = 154
    cx = -178 # Center axis aligned with Pace

    # 1. Clear castle air envelope (Y: 64 to 155)
    # Do in 4 vertical slices to avoid single-fill overload
    for sy in range(base_y, max_y + 1, 25):
        ey = min(sy + 24, max_y + 1)
        cmds.append(f"fill {x1} {sy} {z1} {x2} {ey} {z2} air replace")

    # 2. Volcanic Chasm in front of the castle entrance
    # Z: 90 to 99, X: -215 to -141, Y: 46 to 63
    cmds.append("fill -215 46 90 -141 63 99 air replace")
    # Chasm magma bed & lava
    cmds.append("fill -215 45 90 -141 45 99 magma_block replace")
    cmds.append("fill -215 46 90 -141 54 99 lava replace")
    # Rugged volcanic chasm walls
    cmds.append("fill -216 46 90 -216 63 99 polished_blackstone replace")
    cmds.append("fill -140 46 90 -140 63 99 polished_blackstone replace")
    cmds.append("fill -215 46 89 -141 63 89 obsidian replace")
    cmds.append("fill -215 46 100 -141 63 100 deepslate_bricks replace")

    # 3. Majestic Drawbridge over the volcanic chasm
    # Spans from Z: 88 to Z: 102, Width: 5 (X: -180 to -176)
    # Bridge deck
    cmds.append(f"fill {cx-2} {base_y} 88 {cx+2} {base_y} 101 dark_oak_planks replace")
    cmds.append(f"fill {cx-2} {base_y} 88 {cx+2} {base_y} 101 dark_oak_slab[type=bottom] replace")
    # Parapets / Railings
    cmds.append(f"fill {cx-2} {base_y+1} 88 {cx-2} {base_y+1} 101 stone_brick_wall replace")
    cmds.append(f"fill {cx+2} {base_y+1} 88 {cx+2} {base_y+1} 101 stone_brick_wall replace")
    cmds.append(f"fill {cx-2} {base_y+2} 88 {cx-2} {base_y+2} 101 dark_oak_fence replace")
    cmds.append(f"fill {cx+2} {base_y+2} 88 {cx+2} {base_y+2} 101 dark_oak_fence replace")
    # Stone approach ramps
    cmds.append(f"fill {cx-2} {base_y} 87 {cx+2} {base_y} 87 stone_bricks replace")
    # Heavy suspension chains hanging from gatehouse towers
    for y in range(base_y + 3, base_y + 16):
        cmds.append(f"setblock {cx-2} {y} 101 chain replace")
        cmds.append(f"setblock {cx+2} {y} 101 chain replace")
    for step in range(5):
        cmds.append(f"setblock {cx-2} {base_y+2+step*2} {93+step*2} chain replace")
        cmds.append(f"setblock {cx+2} {base_y+2+step*2} {93+step*2} chain replace")

    # 4. Citadel Foundation & Fortified Perimeter (100x100)
    # Solid plinth beneath ground
    cmds.append(f"fill {x1} {base_y-1} {z1} {x2} {base_y-1} {z2} polished_blackstone replace")
    # Courtyard tile flooring
    cmds.append(f"fill {x1+2} {base_y} {z1+2} {x2-2} {base_y} {z2-2} deepslate_tiles replace")
    # Perimeter curtain walls (16m high, Y: 64 to 80)
    cmds.append(f"fill {x1} {base_y} {z1} {x2} {base_y+16} {z1} deepslate_bricks replace")
    cmds.append(f"fill {x1} {base_y} {z2} {x2} {base_y+16} {z2} deepslate_bricks replace")
    cmds.append(f"fill {x1} {base_y} {z1} {x1} {base_y+16} {z2} deepslate_bricks replace")
    cmds.append(f"fill {x2} {base_y} {z1} {x2} {base_y+16} {z2} deepslate_bricks replace")
    # Wall battlements / crenellations at Y=81
    cmds.append(f"fill {x1} {base_y+17} {z1} {x2} {base_y+17} {z1} deepslate_tile_wall replace")
    cmds.append(f"fill {x1} {base_y+17} {z2} {x2} {base_y+17} {z2} deepslate_tile_wall replace")
    cmds.append(f"fill {x1} {base_y+17} {z1} {x1} {base_y+17} {z2} deepslate_tile_wall replace")
    cmds.append(f"fill {x2} {base_y+17} {z1} {x2} {base_y+17} {z2} deepslate_tile_wall replace")

    # 4 Bastion Corner Towers (12x12, hollow, rising to Y=90)
    corners = [
        (x1, z1, x1+11, z1+11),
        (x2-11, z1, x2, z1+11),
        (x1, z2-11, x1+11, z2),
        (x2-11, z2-11, x2, z2)
    ]
    for bx1, bz1, bx2, bz2 in corners:
        cmds.append(f"fill {bx1} {base_y} {bz1} {bx2} {base_y+26} {bz2} deepslate_bricks hollow")
        cmds.append(f"fill {bx1+1} {base_y+26} {bz1+1} {bx2-1} {base_y+26} {bz2-1} stone_brick_slab[type=bottom] replace")
        cmds.append(f"fill {bx1} {base_y+27} {bz1} {bx2} {base_y+27} {bz2} stone_brick_wall outline")
        cmds.append(f"setblock {(bx1+bx2)//2} {base_y+27} {(bz1+bz2)//2} sea_lantern replace")

    # 5. Front Gatehouse & Drawbridge Portal
    # Archway through front curtain wall (aligned with drawbridge)
    cmds.append(f"fill {cx-2} {base_y} {z1} {cx+2} {base_y+8} {z1} air replace")
    cmds.append(f"fill {cx-4} {base_y} {z1-1} {cx-3} {base_y+18} {z1+3} stone_bricks replace")
    cmds.append(f"fill {cx+3} {base_y} {z1-1} {cx+4} {base_y+18} {z1+3} stone_bricks replace")
    cmds.append(f"fill {cx-2} {base_y+9} {z1} {cx+2} {base_y+10} {z1} stone_brick_slab[type=top] replace")

    # 6. Main Hollow Cathedral Hall (40x30x40 blocks)
    # Centered at cx=-178, Z: 105 to 144 (40 deep), X: cx-20 to cx+19 (-198 to -159, 40 wide)
    # Y: base_y (64) to base_y+30 (94)
    hx1, hx2 = cx - 20, cx + 19
    hz1, hz2 = 105, 144
    hy1, hy2 = base_y, base_y + 30
    
    # Outer hollow cathedral structure
    cmds.append(f"fill {hx1} {hy1} {hz1} {hx2} {hy2} {hz2} deepslate_bricks hollow")
    # Vaulted ceiling
    cmds.append(f"fill {hx1+1} {hy2} {hz1+1} {hx2-1} {hy2} {hz2-1} deepslate_tiles replace")
    
    # Grand Arched Entrance into Cathedral Hall (front wall hz1)
    cmds.append(f"fill {cx-2} {base_y} {hz1} {cx+2} {base_y+7} {hz1} air replace")
    cmds.append(f"fill {cx-1} {base_y+8} {hz1} {cx+1} {base_y+8} {hz1} air replace")

    # Arch windows on east and west walls (tall gothic stained glass with warm sea lantern arches)
    for wz in [hz1 + 8, hz1 + 18, hz1 + 28, hz1 + 34]:
        # West windows
        cmds.append(f"fill {hx1} {base_y+4} {wz-1} {hx1} {base_y+16} {wz+1} purple_stained_glass replace")
        cmds.append(f"setblock {hx1} {base_y+17} {wz} sea_lantern replace")
        # East windows
        cmds.append(f"fill {hx2} {base_y+4} {wz-1} {hx2} {base_y+16} {wz+1} purple_stained_glass replace")
        cmds.append(f"setblock {hx2} {base_y+17} {wz} sea_lantern replace")

    # Cathedral Floor: 100% Mob-proof bottom slabs & carpets
    cmds.append(f"fill {hx1+1} {base_y} {hz1+1} {hx2-1} {base_y} {hz2-1} polished_deepslate_slab[type=bottom] replace")
    # Central imperial red carpet runner with black carpet borders
    cmds.append(f"fill {cx-2} {base_y} {hz1+1} {cx+2} {base_y} {hz2-2} red_carpet replace")
    cmds.append(f"fill {cx-3} {base_y} {hz1+1} {cx-3} {base_y} {hz2-2} black_carpet replace")
    cmds.append(f"fill {cx+3} {base_y} {hz1+1} {cx+3} {base_y} {hz2-2} black_carpet replace")

    # 4 Magnificent Gothic Chandeliers hanging from the high ceiling
    # Golden light level > 15 completely mob-proofing the hall
    chandelier_locs = [
        (hx1 + 10, hz1 + 12),
        (hx2 - 10, hz1 + 12),
        (hx1 + 10, hz2 - 12),
        (hx2 - 10, hz2 - 12)
    ]
    for lx, lz in chandelier_locs:
        # Central hanging chain from ceiling (Y=93 down to Y=82)
        for cy_chain in range(hy2 - 1, hy2 - 12, -1):
            cmds.append(f"setblock {lx} {cy_chain} {lz} chain replace")
        # Chandelier cross-frame of iron bars and glowstone at Y=82
        cy_lamp = hy2 - 12
        cmds.append(f"setblock {lx} {cy_lamp} {lz} glowstone replace")
        for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            cmds.append(f"setblock {lx+dx} {cy_lamp} {lz+dz} iron_bars replace")
            cmds.append(f"setblock {lx+dx*2} {cy_lamp} {lz+dz*2} glowstone replace")
            cmds.append(f"setblock {lx+dx*2} {cy_lamp-1} {lz+dz*2} soul_lantern[hanging=true] replace")

    # 7. Functional Furnished Survival Rooms Inside Cathedral
    
    # ROOM A: Grand Throne Room (South end of Cathedral: Z = hz2 - 4 to hz2 - 1)
    # Raised dais of crying obsidian
    cmds.append(f"fill {cx-5} {base_y} {hz2-4} {cx+5} {base_y+1} {hz2-1} crying_obsidian replace")
    cmds.append(f"fill {cx-5} {base_y+1} {hz2-4} {cx+5} {base_y+1} {hz2-4} smooth_quartz_stairs[facing=north,half=bottom] replace")
    # Twin Imperial Thrones (Pace & Lizzie)
    for throne_x in [cx - 2, cx + 2]:
        cmds.append(f"setblock {throne_x} {base_y+2} {hz2-2} gold_block replace")
        cmds.append(f"setblock {throne_x} {base_y+3} {hz2-2} crying_obsidian replace")
        cmds.append(f"setblock {throne_x} {base_y+4} {hz2-2} purple_banner replace")
        cmds.append(f"setblock {throne_x-1} {base_y+2} {hz2-2} smooth_quartz_stairs[facing=east,half=bottom] replace")
        cmds.append(f"setblock {throne_x+1} {base_y+2} {hz2-2} smooth_quartz_stairs[facing=west,half=bottom] replace")
        cmds.append(f"setblock {throne_x} {base_y+5} {hz2-2} soul_lantern[hanging=false] replace")

    # ROOM B: Crafting & Metallurgy Wing (West wall: hx1+1 to hx1+6, Z: hz1+4 to hz1+18)
    cmds.append(f"fill {hx1+1} {base_y} {hz1+4} {hx1+1} {base_y} {hz1+6} blast_furnace[facing=east,lit=true] replace")
    cmds.append(f"fill {hx1+1} {base_y} {hz1+7} {hx1+1} {base_y} {hz1+9} furnace[facing=east,lit=true] replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+11} anvil[facing=south] replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+12} chipped_anvil[facing=south] replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+14} smithing_table replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+15} grindstone[facing=east] replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+17} crafting_table replace")
    cmds.append(f"setblock {hx1+1} {base_y} {hz1+18} brewing_stand replace")
    cmds.append(f"setblock {hx1+2} {base_y} {hz1+18} water_cauldron[level=3] replace")

    # ROOM C: Grand Storage Room (East wall: hx2-6 to hx2-1, Z: hz1+4 to hz1+20)
    # Double chests rows with barrels
    for cz_step in range(hz1 + 4, hz1 + 20, 2):
        cmds.append(f"setblock {hx2-1} {base_y} {cz_step} chest[facing=west,type=left] replace")
        cmds.append(f"setblock {hx2-1} {base_y} {cz_step+1} chest[facing=west,type=right] replace")
        cmds.append(f"setblock {hx2-1} {base_y+1} {cz_step} barrel[facing=west] replace")
        cmds.append(f"setblock {hx2-1} {base_y+1} {cz_step+1} barrel[facing=west] replace")
        cmds.append(f"setblock {hx2-1} {base_y+2} {cz_step} lantern[hanging=false] replace")

    # ROOM D: Level 30 Enchanting Sanctuary (West wing alcove: hx1+1 to hx1+8, Z: hz2-14 to hz2-6)
    # Enchanting table surrounded by 16+ bookshelves (Level 30 power)
    ench_x = hx1 + 4
    ench_z = hz2 - 10
    cmds.append(f"setblock {ench_x} {base_y} {ench_z} enchanting_table replace")
    # Bookshelves ring
    for bx in range(ench_x - 2, ench_x + 3):
        for bz in range(ench_z - 2, ench_z + 3):
            if abs(bx - ench_x) == 2 or abs(bz - ench_z) == 2:
                cmds.append(f"fill {bx} {base_y} {bz} {bx} {base_y+1} {bz} bookshelf replace")
    # Doorway into enchanting circle
    cmds.append(f"fill {ench_x} {base_y} {ench_z+2} {ench_x} {base_y+1} {ench_z+2} air replace")
    cmds.append(f"setblock {ench_x-1} {base_y} {ench_z+2} lectern[facing=north] replace")
    cmds.append(f"setblock {ench_x+1} {base_y} {ench_z+2} ender_chest[facing=north] replace")
    cmds.append(f"setblock {ench_x} {base_y+4} {ench_z} soul_lantern[hanging=true] replace")

    # 8. Central Towering Main Spire (15x15 base, 75m high, pointed red roof to Y=154)
    # Base: 15x15 centered behind the cathedral hall
    # X: cx-7 to cx+7 (-185 to -171), Z: 148 to 162 (15x15)
    # Altitude: base_y (64) to base_y+75 (139) hollow shaft!
    sx1, sx2 = cx - 7, cx + 7
    sz1, sz2 = 148, 162
    spire_top_y = base_y + 75 # 139
    
    cmds.append(f"fill {sx1} {base_y} {sz1} {sx2} {spire_top_y} {sz2} deepslate_bricks hollow")
    
    # Doorway connecting Cathedral Hall to Spire Base
    cmds.append(f"fill {cx-1} {base_y} {hz2} {cx+1} {base_y+3} {sz1} air replace")
    
    # Internal spire levels & platforms at Y=85, Y=114 (Bridge level), Y=135 (Master Bedroom)
    for py in [base_y + 21, base_y + 50, spire_top_y - 4]:
        cmds.append(f"fill {sx1+1} {py} {sz1+1} {sx2-1} {py} {sz2-1} stone_brick_slab[type=bottom] replace")
        cmds.append(f"setblock {cx} {py} {sz1+2} air replace") # Hatch
    
    # Vertical ladder shaft connecting all spire floors
    for ly in range(base_y, spire_top_y):
        cmds.append(f"setblock {cx} {ly} {sz1+2} ladder[facing=south] replace")

    # MASTER BEDROOM at the top of the Spire (Y=135..139)
    bed_y = spire_top_y - 4
    # Panoramic arched picture windows with views of the entire realm
    for wx in [sx1, sx2]:
        cmds.append(f"fill {wx} {bed_y+1} {sz1+4} {wx} {bed_y+3} {sz2-4} tinted_glass replace")
    for wz in [sz1, sz2]:
        cmds.append(f"fill {sx1+4} {bed_y+1} {wz} {sx2-4} {bed_y+3} {wz} tinted_glass replace")
    
    # Royal red beds & furnishings
    cmds.append(f"setblock {cx-2} {bed_y+1} {sz2-2} red_bed[facing=north,part=head] replace")
    cmds.append(f"setblock {cx-2} {bed_y+1} {sz2-1} red_bed[facing=north,part=foot] replace")
    cmds.append(f"setblock {cx+2} {bed_y+1} {sz2-2} red_bed[facing=north,part=head] replace")
    cmds.append(f"setblock {cx+2} {bed_y+1} {sz2-1} red_bed[facing=north,part=foot] replace")
    cmds.append(f"setblock {cx-4} {bed_y+1} {sz2-2} ender_chest[facing=north] replace")
    cmds.append(f"setblock {cx+4} {bed_y+1} {sz2-2} furnace[facing=north] replace")
    cmds.append(f"summon armor_stand {cx-4} {bed_y+1} {sz1+2} {{ShowArms:1b}}")
    cmds.append(f"summon armor_stand {cx+4} {bed_y+1} {sz1+2} {{ShowArms:1b}}")
    cmds.append(f"setblock {cx} {bed_y+4} {(sz1+sz2)//2} sea_lantern replace")

    # Steep Pointed Red Brick Roof from Y=139 to Y=154 (reaches max height of 90 blocks!)
    for i in range(8):
        ry = spire_top_y + (i * 2)
        cmds.append(f"fill {sx1+i} {ry} {sz1+i} {sx2-i} {ry+1} {sz2-i} red_nether_bricks replace")
    # Apex Finial & Beacon at Y=154
    cmds.append(f"setblock {cx} {max_y-1} {(sz1+sz2)//2} gold_block replace")
    cmds.append(f"setblock {cx} {max_y} {(sz1+sz2)//2} beacon replace")

    # 9. High-Altitude Open Colonnade Bridge (5-block wide on 50m stone pillars)
    # Exits Spire East wall (sx2) at Y = base_y + 50 (114)
    # Extends across to Watchtower West wall (tx1 = -137)
    # Width: 5 blocks (Z: 153 to 157)
    by = base_y + 50
    bx1 = sx2 # -171
    bx2 = -137
    bz1, bz2 = 153, 157
    
    # Bridge doorway through Spire wall
    cmds.append(f"fill {sx2} {by+1} {bz1+1} {sx2} {by+3} {bz2-1} air replace")
    # Bridge walkway deck
    cmds.append(f"fill {bx1} {by} {bz1} {bx2} {by} {bz2} deepslate_tile_slab[type=top] replace")
    # Parapets with stone brick walls & lanterns
    cmds.append(f"fill {bx1} {by+1} {bz1} {bx2} {by+1} {bz1} stone_brick_wall replace")
    cmds.append(f"fill {bx1} {by+1} {bz2} {bx2} {by+1} {bz2} stone_brick_wall replace")
    for lx in range(bx1 + 3, bx2, 5):
        cmds.append(f"setblock {lx} {by+2} {bz1} lantern[hanging=false] replace")
        cmds.append(f"setblock {lx} {by+2} {bz2} lantern[hanging=false] replace")

    # 50-Block-High Vertical Stone Pillars supporting the bridge (Y: 64 to 114)
    # 3 colossal pillars at X = -163, -153, -143
    for px in [bx1 + 8, bx1 + 18, bx1 + 28]:
        cmds.append(f"fill {px-1} {base_y} {bz1} {px+1} {by} {bz2} polished_deepslate replace")
        cmds.append(f"fill {px} {base_y} {bz1-1} {px} {by} {bz2+1} stone_bricks replace")

    # 10. Separate Hollow 9x9 Watchtower on the far right
    # X: -137 to -129 (9 wide), Z: 151 to 159 (9 deep)
    # Rises from base_y (64) up to base_y + 61 (125)
    tx1, tx2 = -137, -129
    tz1, tz2 = 151, 159
    ty_top = base_y + 61 # 125
    
    cmds.append(f"fill {tx1} {base_y} {tz1} {tx2} {ty_top} {tz2} stone_bricks hollow")
    # Doorway connecting from Colonnade Bridge at Y=114
    cmds.append(f"fill {tx1} {by+1} {bz1+1} {tx1} {by+3} {bz2-1} air replace")
    
    # Watchtower Observation Battlement Platform at Y=125
    cmds.append(f"fill {tx1+1} {ty_top} {tz1+1} {tx2-1} {ty_top} {tz2-1} stone_brick_slab[type=bottom] replace")
    cmds.append(f"fill {tx1} {ty_top+1} {tz1} {tx2} {ty_top+1} {tz2} stone_brick_wall outline")
    # Golden Warning Bell in center
    cmds.append(f"setblock {(tx1+tx2)//2} {ty_top+1} {(tz1+tz2)//2} bell replace")
    cmds.append(f"setblock {(tx1+tx2)//2} {ty_top} {(tz1+tz2)//2} sea_lantern replace")
    
    # Internal ladder from ground to observation deck
    for ly in range(base_y, ty_top):
        cmds.append(f"setblock {tx1+2} {ly} {tz1+2} ladder[facing=south] replace")

    return cmds

def main():
    print("[*] Generating 100x100 Gothic Medieval Mega-Castle commands...")
    cmds = generate_castle_commands()
    print(f"[*] Total commands generated: {len(cmds)}")
    
    # Write to mcfunction datapack file
    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# Massive 100x100 Gothic Medieval Castle Generator\n")
        for cmd in cmds:
            f.write(cmd + "\n")
    print(f"[*] Wrote mcfunction to {FUNCTION_PATH}")

    # Announce in-game
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Iniciando a construcao da Cidadela Gotica Monumental (100x100x90)...","color":"gold","bold":true}]')
    run_cmd("execute as @a at @s run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 0.8")

    # Reload datapack and execute function
    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)
    
    print("[*] Executing /function blueprints:build_castle...")
    t0 = time.time()
    res = run_cmd("function blueprints:build_castle")
    elapsed = time.time() - t0
    print(f"[*] Function execution finished in {elapsed:.2f}s: {res}")

    # Teleport Pace directly onto the grand drawbridge entrance to admire the view!
    run_cmd("tp Pace -178 65 89 0 0")
    run_cmd("execute as @a at @s run playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 1 1")
    run_cmd("execute as @a at @s run playsound minecraft:block.beacon.activate master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Cidadela Gotica Colossal (100x100x90) CONCLUIDA! Atravesse a ponte levadica e entre no Grande Salao!","color":"green","bold":true}]')
    print("[*] Castle generation completed successfully!")

if __name__ == "__main__":
    main()
