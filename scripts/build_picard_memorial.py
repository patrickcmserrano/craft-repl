#!/usr/bin/env python3
"""
build_picard_memorial.py - Generates an Orbital Memorial Pavilion alongside USS Enterprise NCC-1701-D
Honoring the life, philosophy, and legacy of Sir Patrick Stewart (Captain Jean-Luc Picard).

Location:
Connected to the Enterprise-D saucer (cx = -65, cz = 100).
Rotunda Center: mx = -82, my = 138, mz = 112.
Directly visible from Pace's vantage point, with a crystalline walkway connecting to the saucer.
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_picard_memorial.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_commands(mx, my, mz):
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # Clear memorial volume (radius 10, height 12)
    cmds.append(f"fill {mx-10} {my-2} {mz-10} {mx+10} {my+12} {mz+10} air replace")

    # 1. Circular Pavilion Foundation (Radius 8)
    for dz in range(-8, 9):
        rx = int(math.sqrt(max(0, 8*8 - dz*dz)))
        # Sub-base (smooth stone)
        cmds.append(f"fill {mx-rx} {my-1} {mz+dz} {mx+rx} {my-1} {mz+dz} polished_deepslate replace")
        # Main Floor (smooth quartz)
        cmds.append(f"fill {mx-rx} {my} {mz+dz} {mx+rx} {my} {mz+dz} smooth_quartz replace")

    # Outer perimeter rim (chiseled quartz + polished blackstone)
    for dz in range(-8, 9):
        rx = int(math.sqrt(max(0, 8*8 - dz*dz)))
        if rx >= 1:
            cmds.append(f"setblock {mx-rx} {my} {mz+dz} polished_blackstone replace")
            cmds.append(f"setblock {mx+rx} {my} {mz+dz} polished_blackstone replace")
            # Perimeter low railing with lanterns
            if abs(dz) % 2 == 0 and rx > 2:
                cmds.append(f"setblock {mx-rx} {my+1} {mz+dz} polished_deepslate_wall replace")
                cmds.append(f"setblock {mx+rx} {my+1} {mz+dz} polished_deepslate_wall replace")

    # 2. Inlaid Starfleet Delta Insignia on the Floor (Gold & Crying Obsidian)
    # The Delta arrowhead pointing towards +Z (forward):
    cmds.append(f"setblock {mx} {my} {mz+4} gold_block replace")
    cmds.append(f"fill {mx-1} {my} {mz+3} {mx+1} {my} {mz+3} gold_block replace")
    cmds.append(f"fill {mx-2} {my} {mz+2} {mx+2} {my} {mz+2} gold_block replace")
    cmds.append(f"fill {mx-2} {my} {mz+1} {mx+2} {my} {mz+1} gold_block replace")
    cmds.append(f"fill {mx-3} {my} {mz} {mx+3} {my} {mz} gold_block replace")
    # Starfleet inner cutout / swoop
    cmds.append(f"fill {mx-1} {my} {mz} {mx+1} {my} {mz+1} crying_obsidian replace")
    cmds.append(f"setblock {mx-3} {my} {mz-1} gold_block replace")
    cmds.append(f"setblock {mx+3} {my} {mz-1} gold_block replace")
    cmds.append(f"setblock {mx-2} {my} {mz-2} gold_block replace")
    cmds.append(f"setblock {mx+2} {my} {mz-2} gold_block replace")

    # 3. Eight Classical Columns (Smooth Quartz Pillars)
    angles = [0, 45, 90, 135, 180, 225, 270, 315]
    for ang in angles:
        rad = math.radians(ang)
        col_x = mx + int(round(6.5 * math.cos(rad)))
        col_z = mz + int(round(6.5 * math.sin(rad)))
        # Pillar from my+1 to my+6
        cmds.append(f"fill {col_x} {my+1} {col_z} {col_x} {my+6} {col_z} quartz_pillar replace")
        cmds.append(f"setblock {col_x} {my+7} {col_z} chiseled_quartz_block replace")

    # 4. Domed Celestial Roof (Smooth Quartz & Tinted Glass skylight)
    for dz in range(-7, 8):
        rx = int(math.sqrt(max(0, 7*7 - dz*dz)))
        cmds.append(f"fill {mx-rx} {my+7} {mz+dz} {mx+rx} {my+7} {mz+dz} smooth_quartz replace")
    for dz in range(-4, 5):
        rx = int(math.sqrt(max(0, 4*4 - dz*dz)))
        cmds.append(f"fill {mx-rx} {my+8} {mz+dz} {mx+rx} {my+8} {mz+dz} tinted_glass replace")
    # Center Spire with End Rod
    cmds.append(f"setblock {mx} {my+9} {mz} end_rod[facing=up] replace")
    cmds.append(f"setblock {mx} {my+10} {mz} sea_lantern replace")

    # 5. THE MEMORIAL MONOLITH & ETERNAL FLAME (Center)
    # Monolith plinth at [mx, my+1, mz]
    cmds.append(f"fill {mx-1} {my+1} {mz-1} {mx+1} {my+1} {mz+1} polished_blackstone replace")
    cmds.append(f"setblock {mx} {my+1} {mz} soul_sand replace")
    cmds.append(f"setblock {mx} {my+2} {mz} soul_fire replace") # The Eternal Soul Flame of Picard

    # Memorial Obelisk Pillar behind the flame
    cmds.append(f"fill {mx-1} {my+2} {mz-2} {mx+1} {my+5} {mz-2} polished_deepslate replace")
    cmds.append(f"fill {mx} {my+2} {mz-2} {mx} {my+5} {mz-2} crying_obsidian replace")
    cmds.append(f"setblock {mx} {my+6} {mz-2} sea_lantern replace")

    # Inscribed Memorial Signs with Picard's quotes on the Obelisk (facing South +Z):
    sign_nbt_1 = (
        '{front_text:{messages:['
        '\'["",{"text":"SIR PATRICK","color":"gold","bold":true}]\','
        '\'["",{"text":"STEWART","color":"gold","bold":true}]\','
        '\'["",{"text":"1940 — 2026","color":"yellow"}]\','
        '\'["",{"text":"Capitão Jean-Luc Picard","color":"aqua"}]\''
        ']}}'
    )
    cmds.append(f"setblock {mx} {my+4} {mz-1} oak_wall_sign[facing=south]{sign_nbt_1} replace")

    sign_nbt_2 = (
        '{front_text:{messages:['
        '\'["",{"text":"Things are only","color":"white","italic":true}]\','
        '\'["",{"text":"impossible until","color":"white","italic":true}]\','
        '\'["",{"text":"they are not.","color":"white","italic":true}]\','
        '\'["",{"text":"— J.L. Picard","color":"yellow","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {mx-1} {my+3} {mz-1} oak_wall_sign[facing=south]{sign_nbt_2} replace")

    sign_nbt_3 = (
        '{front_text:{messages:['
        '\'["",{"text":"It is possible to","color":"white","italic":true}]\','
        '\'["",{"text":"make no mistakes","color":"white","italic":true}]\','
        '\'["",{"text":"and still lose.","color":"white","italic":true}]\','
        '\'["",{"text":"That is life.","color":"yellow","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {mx+1} {my+3} {mz-1} oak_wall_sign[facing=south]{sign_nbt_3} replace")

    # 6. CAPTAIN'S ARTIFACTS PEDESTALS
    # Left Pedestal: The Ressikan Flute ("The Inner Light")
    cmds.append(f"setblock {mx-3} {my+1} {mz+1} smooth_quartz_stairs[facing=south] replace")
    cmds.append(f"setblock {mx-3} {my+2} {mz+1} lightning_rod[facing=up] replace") # Golden Ressikan Flute relic
    flute_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"Ressikan Flute","color":"gold","bold":true}]\','
        '\'["",{"text":"The Inner Light","color":"gray","italic":true}]\','
        '\'["",{"text":"Memory of Kataan","color":"aqua"}]\','
        '\'["",{"text":"Picard Heirloom","color":"yellow"}]\''
        ']}}'
    )
    cmds.append(f"setblock {mx-3} {my+1} {mz+2} oak_wall_sign[facing=south]{flute_sign} replace")

    # Right Pedestal: Picard's Earl Grey Station ("Tea. Earl Grey. Hot.")
    cmds.append(f"setblock {mx+3} {my+1} {mz+1} smooth_quartz_stairs[facing=south] replace")
    cmds.append(f"setblock {mx+3} {my+2} {mz+1} flower_pot replace") # The Earl Grey teacup
    tea_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"Tea. Earl Grey.","color":"gold","bold":true}]\','
        '\'["",{"text":"Hot.","color":"yellow","bold":true}]\','
        '\'["",{"text":"Command Replicator","color":"gray"}]\','
        '\'["",{"text":"Picard Ritual","color":"aqua"}]\''
        ']}}'
    )
    cmds.append(f"setblock {mx+3} {my+1} {mz+2} oak_wall_sign[facing=south]{tea_sign} replace")

    # Memorial Flowers of Remembrance (Lily of the Valley & Spore Blossom particles)
    cmds.append(f"setblock {mx-4} {my+1} {mz-2} lily_of_the_valley replace")
    cmds.append(f"setblock {mx+4} {my+1} {mz-2} lily_of_the_valley replace")
    cmds.append(f"setblock {mx-3} {my+1} {mz-3} wither_rose replace")
    cmds.append(f"setblock {mx+3} {my+1} {mz-3} wither_rose replace")
    cmds.append(f"setblock {mx} {my+7} {mz} spore_blossom replace") # Drifting celestial particles

    # 7. CRYSTALLINE ENERGY WALKWAY LINKING MEMORIAL TO ENTERPRISE-D
    # Memorial is at [mx=-82, mz=112]. Saucer port rim is at [cx=-65, cz=106..112].
    for x in range(mx + 6, -65 - 6):
        # Floor: Cyan & light blue glass with sea lanterns
        cmds.append(f"setblock {x} {my} {mz} light_blue_stained_glass replace")
        cmds.append(f"setblock {x} {my} {mz-1} cyan_stained_glass replace")
        cmds.append(f"setblock {x} {my} {mz+1} cyan_stained_glass replace")
        # Underlights
        if x % 2 == 0:
            cmds.append(f"setblock {x} {my-1} {mz} sea_lantern replace")
        # Railings
        cmds.append(f"setblock {x} {my+1} {mz-1} end_rod[facing=up] replace")
        cmds.append(f"setblock {x} {my+1} {mz+1} end_rod[facing=up] replace")

    # Teleport Pad / Observation Look-out at the Memorial entrance
    cmds.append(f"setblock {mx} {my} {mz+6} lodestone replace")
    cmds.append(f"setblock {mx} {my+1} {mz+6} heavy_weighted_pressure_plate replace")

    return cmds

def main():
    mx, my, mz = -82, 138, 112
    print(f"[*] Compiling Picard Memorial Rotunda at [{mx}, {my}, {mz}]...")
    cmds = generate_commands(mx, my, mz)

    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_picard_memorial...")
    fn = run_cmd("function blueprints:build_picard_memorial")
    print("Function:", fn)

    # Save Waypoint
    try:
        waypoint_file = "/home/pace/.config/mc-cli/waypoints.json"
        with open(waypoint_file, "r") as f:
            wps = json.load(f)
        wps = [w for w in wps if w.get("name") != "Memorial Picard"]
        wps.append({"name": "Memorial Picard", "x": mx, "y": my + 1, "z": mz, "icon": "🕯️"})
        with open(waypoint_file, "w") as f:
            json.dump(wps, f, indent=2)
        print("[*] Saved 'Memorial Picard' waypoint")
    except Exception as e:
        print("[WARN] Could not save waypoint:", e)

    # Broadcast in-game
    run_cmd(
        'tellraw @a ['
        '{"text":"🕊️ [MEMORIAL] ","color":"aqua","bold":true},'
        '{"text":"O Memorial Celestial a Sir Patrick Stewart foi consagrado ao lado da Enterprise-D!","color":"gold"}'
        ']'
    )
    
    run_cmd("playsound minecraft:block.beacon.activate master @a ~ ~ ~ 1.0 0.8")

if __name__ == "__main__":
    main()
