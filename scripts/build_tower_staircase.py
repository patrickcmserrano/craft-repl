#!/usr/bin/env python3
"""
build_tower_staircase.py
Replaces the vertical wooden ladder in the Spire Tower with a magnificent,
wall-hugging 2-block wide Deepslate Spiral Staircase:
- Interior bounding box: X: -184..-172, Z: 149..161
- Climbs from Y=64 (ground) up to Y=85 (Level 1), Y=114 (Bridge), and Y=135 (Master Bedroom)
- Deepslate brick stairs with polished deepslate structural support and wall handrails
- Clears the straight wooden ladder and cuts generous stairwell openings through floor slabs
- Atmospheric gothic sconces and lanterns along each landing
"""

import sys
import json
import time
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_tower_stairs.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_staircase_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # 1. Remove the straight wooden ladder from base to top
    cmds.append("fill -178 64 150 -178 138 150 air replace ladder")

    # 2. Geometry of the 13x13 Tower Interior:
    # West wall: X = -184
    # East wall: X = -172
    # North wall: Z = 149
    # South wall: Z = 161
    # Stairs width = 2 blocks:
    # East flight: X = -172 and -173
    # South flight: Z = 160 and 161
    # West flight: X = -184 and -183
    # North flight: Z = 149 and 150

    # We will build a continuous square spiral hugging the walls from Y=64 to Y=135:
    # Flight 1: East Wall, heading South (Z: 151 to 159), Y: 65 to 73
    # Landing 1: Southeast Corner (X: -172..-173, Z: 160..161), Y = 74
    # Flight 2: South Wall, heading West (X: -174 to -182), Y: 75 to 83
    # Landing 2: Southwest Corner (X: -183..-184, Z: 160..161), Y = 84
    # Flight 3: West Wall, heading North (Z: 159 to 151), Y: 85 to 93 (Reaches Level 1!)
    # Landing 3: Northwest Corner (X: -183..-184, Z: 149..150), Y = 94
    # Flight 4: North Wall, heading East (X: -182 to -174), Y: 95 to 103 (Crosses above entrance)
    # Landing 4: Northeast Corner (X: -172..-173, Z: 149..150), Y = 104
    # Flight 5: East Wall, heading South (Z: 151 to 159), Y: 105 to 113
    # Landing 5: Southeast Corner (X: -172..-173, Z: 160..161), Y = 114 (Reaches Bridge Level!)
    # Flight 6: South Wall, heading West (X: -174 to -182), Y: 115 to 123
    # Landing 6: Southwest Corner (X: -183..-184, Z: 160..161), Y = 124
    # Flight 7: West Wall, heading North (Z: 159 to 151), Y: 125 to 133
    # Landing 7: Northwest Corner (X: -183..-184, Z: 149..150), Y = 134 (Reaches Master Bedroom!)

    # Clear headroom around the spiral path (Y=65 to 136)
    # Clear East flight
    cmds.append("fill -173 65 149 -172 135 161 air replace stone_brick_slab")
    # Clear South flight
    cmds.append("fill -184 65 160 -172 135 161 air replace stone_brick_slab")
    # Clear West flight
    cmds.append("fill -184 65 149 -183 135 161 air replace stone_brick_slab")
    # Clear North flight
    cmds.append("fill -184 65 149 -172 135 150 air replace stone_brick_slab")

    flights = [
        # (direction, start_coord, end_coord, start_y, is_x_axis, fixed_coord, facing)
        # Flight 1: East wall heading South (Z: 151..159)
        ("E_to_S", 151, 159, 65, False, [-172, -173], "south"),
        # Flight 2: South wall heading West (X: -174..-182)
        ("S_to_W", -174, -182, 75, True, [160, 161], "west"),
        # Flight 3: West wall heading North (Z: 159..151)
        ("W_to_N", 159, 151, 85, False, [-183, -184], "north"),
        # Flight 4: North wall heading East (X: -182..-174)
        ("N_to_E", -182, -174, 95, True, [149, 150], "east"),
        # Flight 5: East wall heading South (Z: 151..159)
        ("E_to_S2", 151, 159, 105, False, [-172, -173], "south"),
        # Flight 6: South wall heading West (X: -174..-182)
        ("S_to_W2", -174, -182, 115, True, [160, 161], "west"),
        # Flight 7: West wall heading North (Z: 159..151)
        ("W_to_N2", 159, 151, 125, False, [-183, -184], "north"),
    ]

    landings = [
        # (x1, x2, z1, z2, y)
        (-173, -172, 160, 161, 74),   # SE Landing 1
        (-184, -183, 160, 161, 84),   # SW Landing 2
        (-184, -183, 149, 150, 94),   # NW Landing 3
        (-173, -172, 149, 150, 104),  # NE Landing 4
        (-173, -172, 160, 161, 114),  # SE Landing 5 (Bridge level)
        (-184, -183, 160, 161, 124),  # SW Landing 6
        (-184, -183, 149, 150, 134),  # NW Landing 7 (Master bedroom entrance)
    ]

    # Generate Flights
    for name, c_start, c_end, y_start, is_x, fixed_coords, facing in flights:
        step = 1 if c_end >= c_start else -1
        coords = list(range(c_start, c_end + step, step))
        for idx, c in enumerate(coords):
            cur_y = y_start + idx
            for fc in fixed_coords:
                if is_x:
                    bx, bz = c, fc
                else:
                    bx, bz = fc, c
                # Stair block
                cmds.append(f"setblock {bx} {cur_y} {bz} deepslate_brick_stairs[facing={facing},half=bottom] replace")
                # Structural support underneath
                cmds.append(f"setblock {bx} {cur_y-1} {bz} polished_deepslate replace")
                # Ensure 3 blocks of headroom
                for hy in range(1, 4):
                    cmds.append(f"setblock {bx} {cur_y+hy} {bz} air replace")

    # Generate Landings
    for lx1, lx2, lz1, lz2, ly in landings:
        cmds.append(f"fill {lx1} {ly} {lz1} {lx2} {ly} {lz2} polished_deepslate replace")
        # Ensure headroom
        cmds.append(f"fill {lx1} {ly+1} {lz1} {lx2} {ly+3} {lz2} air replace")
        # Corner Sconce Lantern on the outer corner
        # Pick outer corner
        cx = lx2 if abs(lx2 - (-171)) < abs(lx1 - (-171)) else lx1
        cz = lz2 if abs(lz2 - 162) < abs(lz1 - 162) else lz1
        cmds.append(f"setblock {cx} {ly+2} {cz} lantern[hanging=false] replace")

    # 3. Handrails along the interior edge overlooking the central atrium:
    # Outer edge of East flight: X = -174
    # Outer edge of South flight: Z = 159
    # Outer edge of West flight: X = -182
    # Outer edge of North flight: Z = 151
    # We add polished deepslate wall handrails where appropriate for safety:
    for name, c_start, c_end, y_start, is_x, fixed_coords, facing in flights:
        step = 1 if c_end >= c_start else -1
        coords = list(range(c_start, c_end + step, step))
        for idx, c in enumerate(coords):
            cur_y = y_start + idx
            # The inner edge coordinate
            if is_x:
                inner_z = 159 if 160 in fixed_coords else 151
                cmds.append(f"setblock {c} {cur_y+1} {inner_z} polished_deepslate_wall replace")
            else:
                inner_x = -174 if -173 in fixed_coords else -182
                cmds.append(f"setblock {inner_x} {cur_y+1} {c} polished_deepslate_wall replace")

    # 4. Central Atrium Chandelier (hanging down the center at X = -178, Z = 155)
    # Center: X = -178, Z = 155
    ax, az = -178, 155
    for cy in range(70, 134):
        cmds.append(f"setblock {ax} {cy} {az} chain[axis=y] replace")
    # Glowing clusters along the central chain
    for ly in [75, 95, 115]:
        cmds.append(f"setblock {ax} {ly} {az} sea_lantern replace")
        cmds.append(f"setblock {ax+1} {ly} {az} soul_lantern[hanging=false] replace")
        cmds.append(f"setblock {ax-1} {ly} {az} soul_lantern[hanging=false] replace")
        cmds.append(f"setblock {ax} {ly} {az+1} soul_lantern[hanging=false] replace")
        cmds.append(f"setblock {ax} {ly} {az-1} soul_lantern[hanging=false] replace")

    # 5. Play chime sound
    cmds.append(f"playsound minecraft:block.deepslate.place block Pace -172 65 151 1 1")
    cmds.append(f"playsound minecraft:entity.player.levelup ambient Pace -172 65 151 1 1")

    return cmds

def main():
    cmds = generate_staircase_commands()
    with open(FUNCTION_PATH, "w") as f:
        f.write("\n".join(cmds) + "\n")

    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")
    res_reload = run_cmd("reload")
    print("[*] Reload result:", res_reload)
    time.sleep(0.3)
    res_func = run_cmd("function blueprints:build_tower_stairs")
    print("[+] Function execution result:", res_func)

if __name__ == "__main__":
    main()
