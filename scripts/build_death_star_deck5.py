#!/usr/bin/env python3
"""
build_death_star_deck5.py - Builds the Emperor's Throne Room (Deck 5, Y=183..189) on Death Star I
Features:
- Solid circular throne chamber floor (R=10) at Y=183
- Imperial Red Carpet from Turbolift (X=171, Z=-106) to Emperor's Dais (X=168, Z=-114)
- High-backed Imperial Throne (Obsidian + Crying Obsidian + Blackstone) on raised dais
- Iconic Spider-web Radial Observation Window overlooking the starry abyss
- Dual Reactor Chasm Drop Shafts (where Vader threw Palpatine!)
- Royal Guards (Red Carpet pedestals / Crimson Banners)
- Smooth connection with the Turbolift shaft doors at Y=183
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_deck5_throne.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_deck5_commands(cx=168, cy=155, cz=-110):
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    floor_y = 183
    ceiling_y = 189
    R = 10

    # 1. Clear chamber interior volume (cylinder R=10, Y=183..189)
    for y in range(floor_y, ceiling_y + 1):
        for dz in range(-R, R + 1):
            max_dx = int(math.sqrt(max(0, R*R - dz*dz)))
            if max_dx > 0:
                cmds.append(f"fill {cx-max_dx} {y} {cz+dz} {cx+max_dx} {y} {cz+dz} air replace")

    # 2. Chamber Floor (polished blackstone & deepslate trim)
    for dz in range(-R, R + 1):
        max_dx = int(math.sqrt(max(0, R*R - dz*dz)))
        if max_dx > 0:
            cmds.append(f"fill {cx-max_dx} {floor_y} {cz+dz} {cx+max_dx} {floor_y} {cz+dz} polished_blackstone replace")

    # Chamber Ceiling
    for dz in range(-R, R + 1):
        max_dx = int(math.sqrt(max(0, R*R - dz*dz)))
        if max_dx > 0:
            cmds.append(f"fill {cx-max_dx} {ceiling_y} {cz+dz} {cx+max_dx} {ceiling_y} {cz+dz} smooth_stone replace")

    # Chamber Perimeter Walls
    for ang in range(0, 360, 6):
        rad = math.radians(ang)
        wx = int(cx + (R + 1) * math.cos(rad))
        wz = int(cz + (R + 1) * math.sin(rad))
        cmds.append(f"fill {wx} {floor_y} {wz} {wx} {ceiling_y} {wz} polished_deepslate replace")

    # 3. Dual Reactor Chasm Drop Shafts (Left & Right of Dais)
    # Pit 1: [cx-5, cz-112]
    # Pit 2: [cx+5, cz-112]
    for px in [cx-5, cx+5]:
        cmds.append(f"fill {px-1} {floor_y} {cz-3} {px+1} {floor_y} {cz-1} air replace") # open to chasm below
        # Protective illuminated rim
        cmds.append(f"fill {px-2} {floor_y} {cz-4} {px+2} {floor_y} {cz} crying_obsidian replace")
        cmds.append(f"fill {px-1} {floor_y} {cz-3} {px+1} {floor_y} {cz-1} air replace")
        cmds.append(f"fill {px-2} {floor_y+1} {cz-4} {px+2} {floor_y+1} {cz} iron_bars replace")
        cmds.append(f"fill {px-1} {floor_y+1} {cz-3} {px+1} {floor_y+1} {cz-1} air replace")
        # Cyan warning beacons deep in pit
        cmds.append(f"setblock {px} {floor_y-1} {cz-2} sea_lantern replace")

    # 4. Elevated Emperor's Dais (Raised 1 block at Y=184, X=cx-3..cx+3, Z=cz-9..cz-5)
    dais_z = cz - 7
    cmds.append(f"fill {cx-3} {floor_y+1} {dais_z-2} {cx+3} {floor_y+1} {dais_z+2} polished_blackstone replace")
    # Dais steps
    cmds.append(f"fill {cx-2} {floor_y+1} {dais_z+3} {cx+2} {floor_y+1} {dais_z+3} polished_blackstone_stairs[facing=north] replace")

    # 5. Emperor's Throne (High-back rotating throne facing South)
    throne_z = dais_z - 1
    cmds.append(f"setblock {cx} {floor_y+2} {throne_z} obsidian replace")
    cmds.append(f"setblock {cx} {floor_y+2} {throne_z+1} polished_blackstone_stairs[facing=south] replace") # Seat cushion
    cmds.append(f"setblock {cx-1} {floor_y+2} {throne_z} polished_blackstone_wall replace") # Armrest L
    cmds.append(f"setblock {cx+1} {floor_y+2} {throne_z} polished_blackstone_wall replace") # Armrest R
    cmds.append(f"setblock {cx} {floor_y+3} {throne_z} crying_obsidian replace") # High backrest
    cmds.append(f"setblock {cx} {floor_y+4} {throne_z} polished_blackstone replace") # Crest

    # 6. Spider-web Observation Window (Behind the Throne facing North)
    # The giant radial viewport overlooking the battlefield (Star Wars Ep VI)
    win_z = cz - R
    cmds.append(f"fill {cx-5} {floor_y+1} {win_z} {cx+5} {floor_y+5} {win_z} tinted_glass replace")
    # Spider-web spokes (iron bars)
    cmds.append(f"fill {cx} {floor_y+1} {win_z} {cx} {floor_y+5} {win_z} iron_bars replace")
    cmds.append(f"fill {cx-5} {floor_y+3} {win_z} {cx+5} {floor_y+3} {win_z} iron_bars replace")
    cmds.append(f"setblock {cx-3} {floor_y+2} {win_z} iron_bars replace")
    cmds.append(f"setblock {cx+3} {floor_y+2} {win_z} iron_bars replace")
    cmds.append(f"setblock {cx-3} {floor_y+4} {win_z} iron_bars replace")
    cmds.append(f"setblock {cx+3} {floor_y+4} {win_z} iron_bars replace")
    cmds.append(f"setblock {cx} {floor_y+3} {win_z} sea_lantern replace") # Central glowing eye

    # 7. Imperial Red Carpet Runway
    # Running from Turbolift area (X=171, Z=-106) straight to the Dais steps (X=168, Z=-113)
    cmds.append(f"fill {cx-1} {floor_y+1} {cz-3} {cx+1} {floor_y+1} {cz+7} red_carpet replace")
    cmds.append(f"fill {cx-1} {floor_y+2} {dais_z} {cx+1} {floor_y+2} {dais_z+2} red_carpet replace")

    # 8. Royal Guard Pedestals & Lighting
    # Red Guards stand on crying obsidian pedestals with red banners
    cmds.append(f"setblock {cx-3} {floor_y+1} {dais_z} crying_obsidian replace")
    cmds.append(f"setblock {cx+3} {floor_y+1} {dais_z} crying_obsidian replace")
    cmds.append(f"setblock {cx-3} {floor_y+2} {dais_z} red_banner replace")
    cmds.append(f"setblock {cx+3} {floor_y+2} {dais_z} red_banner replace")

    # Perimeter lighting along floor
    for ang in [30, 90, 150, 210, 270, 330]:
        rad = math.radians(ang)
        lx = int(cx + (R - 1) * math.cos(rad))
        lz = int(cz + (R - 1) * math.sin(rad))
        cmds.append(f"setblock {lx} {floor_y} {lz} sea_lantern replace")
        cmds.append(f"setblock {lx} {floor_y+1} {lz} light_gray_carpet replace")

    # 9. Inscribed Throne Plaque
    throne_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"EMPEROR THRONE","color":"dark_red","bold":true}]\','
        '\'["",{"text":"DECK 5 - DS-1","color":"red","bold":true}]\','
        '\'["",{"text":"Witness the power","color":"gold","italic":true}]\','
        '\'["",{"text":"of this station!","color":"yellow","italic":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx} {floor_y+2} {throne_z+2} oak_wall_sign[facing=south]{throne_sign} replace")

    return cmds

def main():
    cx, cy, cz = 168, 155, -110
    print(f"[*] Generating Deck 5 Emperor Throne Room at [{cx}, 183, {cz}]...")
    cmds = generate_deck5_commands(cx, cy, cz)

    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_deck5_throne...")
    fn = run_cmd("function blueprints:build_deck5_throne")
    print("Function:", fn)

if __name__ == "__main__":
    main()
