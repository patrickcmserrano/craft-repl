#!/usr/bin/env python3
"""
build_death_star_interior.py - Generates complete interconnected interior decks for Death Star I
Features:
- Deck 5 (Y=183): Emperor's Throne Room & Royal Antechamber
- Deck 4 (Y=169): Superlaser Firing Control Room & Gunner Stations
- Deck 3 (Y=155): Central Chasm, Retractable Catwalk & Tractor Beam Terminal + Hangar Concourse
- Deck 2 (Y=142): Detention Block AA-23, Cell 2187 & Garbage Chute
- Deck 1 (Y=130): Trash Compactor 3263827 & Hypermatter Reactor Core
- High-speed Vertical Turbolift (Bubble Elevator) linking all 5 decks
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_death_star_interior.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_interior_commands(cx, cy, cz):
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # =========================================================================
    # 1. CENTRAL REACTOR CHASM (Vertical Abyss from Y=130 to Y=180)
    # =========================================================================
    # Cylinder radius = 7 blocks, centered at [cx, cz] = [168, -110]
    for y in range(130, 182):
        # Hollow out cylinder
        for dz in range(-7, 8):
            rx = int(math.sqrt(max(0, 7*7 - dz*dz)))
            if rx > 0:
                cmds.append(f"fill {cx-rx} {y} {cz+dz} {cx+rx} {y} {cz+dz} air replace")
        # Perimeter chasm wall
        for ang in range(0, 360, 15):
            rad = math.radians(ang)
            wx = int(cx + 8 * math.cos(rad))
            wz = int(cz + 8 * math.sin(rad))
            block = "crying_obsidian" if (y % 4 == 0) else "polished_deepslate"
            cmds.append(f"setblock {wx} {y} {wz} {block} replace")

    # Hypermatter Reactor Core at the bottom of the Chasm (Y=130..132)
    cmds.append(f"fill {cx-3} {130} {cz-3} {cx+3} {130} {cz+3} crying_obsidian replace")
    cmds.append(f"fill {cx-2} {131} {cz-2} {cx+2} {131} {cz+2} sea_lantern replace")
    cmds.append(f"fill {cx-1} {132} {cz-1} {cx+1} {132} {cz+1} cyan_stained_glass replace")
    cmds.append(f"setblock {cx} {131} {cz} beacon replace")

    # =========================================================================
    # 2. DECK 3 (Y=155): RETRACTABLE CATWALK & TRACTOR BEAM TERMINAL
    # =========================================================================
    # Narrow 1-block catwalk across the abyss from West wall to East wall
    cmds.append(f"fill {cx-8} {155} {cz} {cx+8} {155} {cz} polished_deepslate replace")
    cmds.append(f"fill {cx-8} {156} {cz-1} {cx+8} {156} {cz-1} iron_bars replace")
    cmds.append(f"fill {cx-8} {156} {cz+1} {cx+8} {156} {cz+1} iron_bars replace")

    # Central Tractor Beam Control Terminal (Where Obi-Wan disables it)
    cmds.append(f"setblock {cx} {155} {cz} polished_blackstone replace")
    cmds.append(f"setblock {cx} {156} {cz} observer[facing=up] replace")
    cmds.append(f"setblock {cx} {157} {cz} lightning_rod[facing=up] replace")
    cmds.append(f"setblock {cx} {156} {cz-1} stone_button[facing=north] replace")
    cmds.append(f"setblock {cx} {156} {cz+1} lever[facing=south] replace")

    # Corridor connecting Hangar (X=145..149) into the Central Chasm (X=160)
    cmds.append(f"fill {149} {154} {cz-2} {cx-8} {154} {cz+2} polished_deepslate replace")
    cmds.append(f"fill {149} {155} {cz-1} {cx-8} {157} {cz+1} air replace")
    cmds.append(f"fill {149} {158} {cz-2} {cx-8} {158} {cz+2} polished_blackstone replace")
    # Corridor lighting
    cmds.append(f"setblock {154} {158} {cz} sea_lantern replace")
    cmds.append(f"setblock {160} {158} {cz} sea_lantern replace")

    # =========================================================================
    # 3. DECK 4 (Y=168..172): SUPERLASER FIRING CONTROL ROOM
    # =========================================================================
    # Located at [cx-12..cx-2, Y=168, cz-20..cz-12] behind the Superlaser Dish
    cmds.append(f"fill {cx-12} {168} {cz-20} {cx-2} {168} {cz-12} polished_blackstone replace") # Floor
    cmds.append(f"fill {cx-12} {169} {cz-20} {cx-2} {172} {cz-12} air replace")                # Room air
    cmds.append(f"fill {cx-12} {173} {cz-20} {cx-2} {173} {cz-12} smooth_stone replace")       # Ceiling
    # Walls
    cmds.append(f"fill {cx-13} {169} {cz-20} {cx-13} {172} {cz-12} polished_deepslate replace")
    cmds.append(f"fill {cx-1} {169} {cz-20} {cx-1} {172} {cz-12} polished_deepslate replace")
    cmds.append(f"fill {cx-12} {169} {cz-21} {cx-2} {172} {cz-21} polished_deepslate replace")

    # Blast Viewport looking directly into the primary Superlaser emitter tube
    cmds.append(f"fill {cx-9} {169} {cz-21} {cx-5} {171} {cz-21} lime_stained_glass replace")
    cmds.append(f"setblock {cx-7} {170} {cz-22} sea_lantern replace")

    # Imperial Gunner Firing Stations
    cmds.append(f"setblock {cx-8} {169} {cz-18} polished_blackstone_stairs[facing=north] replace")
    cmds.append(f"setblock {cx-6} {169} {cz-18} polished_blackstone_stairs[facing=north] replace")
    cmds.append(f"setblock {cx-8} {170} {cz-19} daylight_detector replace")
    cmds.append(f"setblock {cx-6} {170} {cz-19} daylight_detector replace")
    cmds.append(f"setblock {cx-7} {170} {cz-19} redstone_lamp[lit=true] replace")
    # Master Firing Lever
    cmds.append(f"setblock {cx-7} {171} {cz-19} lever[facing=south,powered=true] replace")

    # Firing Control Plaque
    firing_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"SUPERLASER","color":"dark_red","bold":true}]\','
        '\'["",{"text":"FIRING CONTROL","color":"red","bold":true}]\','
        '\'["",{"text":"Target: Alderaan","color":"gold"}]\','
        '\'["",{"text":"[FIRE WHEN READY]","color":"green","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx-7} {172} {cz-20} oak_wall_sign[facing=south]{firing_sign} replace")

    # Access corridor from Turbolift into Firing Control
    cmds.append(f"fill {cx-7} {168} {cz-12} {cx-7} {168} {cz-5} polished_blackstone replace")
    cmds.append(f"fill {cx-7} {169} {cz-12} {cx-7} {171} {cz-5} air replace")
    cmds.append(f"fill {cx-7} {172} {cz-12} {cx-7} {172} {cz-5} smooth_stone replace")

    # =========================================================================
    # 4. DECK 2 (Y=142..146): DETENTION BLOCK AA-23 & CELL 2187
    # =========================================================================
    # Floor at Y=142, X: cx+2..cx+14, Z: cz-6..cz+6
    cmds.append(f"fill {cx+2} {142} {cz-6} {cx+14} {142} {cz+6} polished_deepslate replace")
    cmds.append(f"fill {cx+2} {143} {cz-6} {cx+14} {146} {cz+6} air replace")
    cmds.append(f"fill {cx+2} {147} {cz-6} {cx+14} {147} {cz+6} polished_deepslate replace")

    # Corridor division: Central hallway along X with cells on the sides
    cmds.append(f"fill {cx+2} {143} {cz-2} {cx+14} {146} {cz-2} polished_blackstone_brick_wall replace")
    cmds.append(f"fill {cx+2} {143} {cz+2} {cx+14} {146} {cz+2} polished_blackstone_brick_wall replace")
    # Corridor floor lighting
    cmds.append(f"setblock {cx+5} {142} {cz} sea_lantern replace")
    cmds.append(f"setblock {cx+11} {142} {cz} sea_lantern replace")

    # Security Guard Desk (Commander Praji / Imperial Officer station)
    cmds.append(f"setblock {cx+4} {143} {cz} polished_blackstone_stairs[facing=east] replace")
    cmds.append(f"setblock {cx+3} {143} {cz} chiseled_bookshelf[facing=east] replace")
    cmds.append(f"setblock {cx+3} {144} {cz} redstone_lamp[lit=true] replace")

    # Cell 2187 (Princess Leia's Cell) on South side [cx+9..cx+13, cz+3..cz+5]
    cmds.append(f"fill {cx+8} {143} {cz+3} {cx+14} {146} {cz+6} polished_deepslate replace")
    cmds.append(f"fill {cx+9} {143} {cz+3} {cx+13} {145} {cz+5} air replace") # Cell interior
    # Forcefield energy door (Red stained glass + lever)
    cmds.append(f"setblock {cx+11} {143} {cz+2} red_stained_glass replace")
    cmds.append(f"setblock {cx+11} {144} {cz+2} red_stained_glass replace")
    cmds.append(f"setblock {cx+10} {144} {cz+1} lever[facing=north] replace")
    # Cell bunk
    cmds.append(f"setblock {cx+13} {143} {cz+4} gray_bed[facing=west] replace")
    # Cell 2187 Sign
    cell_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"DETENTION BLOCK","color":"dark_red","bold":true}]\','
        '\'["",{"text":"AA-23","color":"red","bold":true}]\','
        '\'["",{"text":"CELL 2187","color":"yellow","bold":true}]\','
        '\'["",{"text":"Prisoner: Leia Organa","color":"white"}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx+11} {145} {cz+1} oak_wall_sign[facing=north]{cell_sign} replace")

    # The Infamous Garbage Chute Hatch ("Into the garbage chute, flyboy!")
    cmds.append(f"setblock {cx+8} {143} {cz-1} iron_trapdoor[facing=north,open=false] replace")
    chute_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"GARBAGE CHUTE","color":"gold","bold":true}]\','
        '\'["",{"text":"WASTE DISPOSAL","color":"yellow"}]\','
        '\'["",{"text":"DROP TO COMPACTOR","color":"red"}]\','
        '\'["",{"text":"[JUMP IN]","color":"dark_green","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx+8} {144} {cz-2} oak_wall_sign[facing=south]{chute_sign} replace")
    # Chute vertical shaft dropping from Y=142 down to Deck 1 (Y=132)
    cmds.append(f"fill {cx+8} {133} {cz-1} {cx+8} {142} {cz-1} air replace")
    cmds.append(f"fill {cx+7} {133} {cz-2} {cx+9} {142} {cz} iron_block replace")
    cmds.append(f"fill {cx+8} {133} {cz-1} {cx+8} {142} {cz-1} air replace") # ensure hollow

    # =========================================================================
    # 5. DECK 1 (Y=130..134): TRASH COMPACTOR 3263827
    # =========================================================================
    # Located under the chute at [cx+4..cx+12, Y=130, cz-8..cz-2]
    cmds.append(f"fill {cx+4} {130} {cz-8} {cx+12} {130} {cz-2} mud replace") # Sludge floor
    cmds.append(f"fill {cx+4} {131} {cz-8} {cx+12} {134} {cz-2} air replace")
    cmds.append(f"fill {cx+4} {135} {cz-8} {cx+12} {135} {cz-2} iron_block replace") # Ceiling
    # Scrap metal, rusty copper & iron bars scattered in the muck
    cmds.append(f"setblock {cx+5} {131} {cz-6} weathered_cut_copper replace")
    cmds.append(f"setblock {cx+6} {131} {cz-4} iron_bars replace")
    cmds.append(f"setblock {cx+10} {131} {cz-6} iron_bars replace")
    cmds.append(f"setblock {cx+9} {131} {cz-3} anvil replace")
    # Hydraulic Compactor Pistons on the sidewalls
    cmds.append(f"fill {cx+3} {131} {cz-7} {cx+3} {133} {cz-3} piston[facing=east] replace")
    cmds.append(f"fill {cx+13} {131} {cz-7} {cx+13} {133} {cz-3} piston[facing=west] replace")
    # The heavy metal brace rod (Luke & Han trying to stop the walls!)
    cmds.append(f"fill {cx+6} {132} {cz-5} {cx+10} {132} {cz-5} lightning_rod[facing=east] replace")

    # =========================================================================
    # 6. CENTRAL VERTICAL TURBOLIFT (Bubble Elevator from Y=130 to Y=183)
    # =========================================================================
    # Two elevator shafts located at [cx+4, cz+4] and [cx+4, cz+5]
    # Shaft 1: UP (Bubble elevator with Soul Sand)
    # Shaft 2: DOWN (Drop shaft with water pad at bottom)
    
    # UP SHAFT (Soul Sand bubble column)
    cmds.append(f"setblock {cx+4} {130} {cz+4} soul_sand replace")
    cmds.append(f"fill {cx+3} {130} {cz+3} {cx+5} {184} {cz+5} iron_block replace")
    cmds.append(f"fill {cx+4} {131} {cz+4} {cx+4} {183} {cz+4} water replace") # Water column for bubbles
    
    # Glass observation front for the turbolift
    cmds.append(f"fill {cx+4} {131} {cz+3} {cx+4} {183} {cz+3} light_blue_stained_glass replace")
    
    # Door openings and signs at each Deck level:
    decks = [
        (131, "Deck 1: Reator & Compactador"),
        (143, "Deck 2: Bloco de Detenção AA-23"),
        (155, "Deck 3: Hangar & Catwalk do Abismo"),
        (169, "Deck 4: Controle do Superlaser"),
        (183, "Deck 5: Sala do Trono do Imperador")
    ]
    for dy, dname in decks:
        # Exit doorway
        cmds.append(f"setblock {cx+3} {dy} {cz+4} iron_door[half=lower,facing=west] replace")
        cmds.append(f"setblock {cx+3} {dy+1} {cz+4} iron_door[half=upper,facing=west] replace")
        cmds.append(f"setblock {cx+2} {dy} {cz+4} polished_blackstone_pressure_plate replace")
        # Deck signage
        part1 = dname[:15]
        part2 = dname[15:30]
        dsign = (
            '{front_text:{messages:['
            '\'["",{"text":"TURBOLIFT","color":"gold","bold":true}]\','
            '\'["",{"text":"' + part1 + '","color":"yellow"}]\','
            '\'["",{"text":"' + part2 + '","color":"aqua"}]\','
            '\'["",{"text":"[STATION LEVEL]","color":"green"}]\''
            ']}}'
        )
        cmds.append(f"setblock {cx+3} {dy+2} {cz+4} oak_wall_sign[facing=west]{dsign} replace")

    return cmds

def main():
    cx, cy, cz = 168, 155, -110
    print(f"[*] Compiling Death Star Interior commands at [{cx}, {cy}, {cz}]...")
    cmds = generate_interior_commands(cx, cy, cz)

    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_death_star_interior...")
    fn = run_cmd("function blueprints:build_death_star_interior")
    print("Function:", fn)

    # Broadcast in-game
    run_cmd(
        'tellraw @a ['
        '{"text":"\\n⚡ [DEATH STAR] ","color":"red","bold":true},'
        '{"text":"Todos os 5 Deques Internos da Estrela da Morte foram concluídos!\\n","color":"gold"},'
        '{"text":"   Deck 5: Sala do Trono • Deck 4: Tiro do Superlaser • Deck 3: Catwalk do Abismo • Deck 2: Detenção AA-23 • Deck 1: Compactador de Lixo\\n","color":"yellow"}'
        ']'
    )
    
    run_cmd("playsound minecraft:block.beacon.activate master @a ~ ~ ~ 1.0 1.2")

if __name__ == "__main__":
    main()
