#!/usr/bin/env python3
"""
build_death_star.py - Procedurally generates the Imperial Death Star I (DS-1 Orbital Battle Station)
Monumental Scale (Diameter = 66 blocks, Radius = 33 blocks).

Location:
Center: cx = 168, cy = 155, cz = -110
Directly framed in Pace's line of sight from [116.2, 99.0, -226.8].
Superlaser Dish faces North-Northwest (towards Pace).
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_death_star.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_death_star_commands(cx, cy, cz, R=33):
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # Clear target volume
    cmds.append(f"fill {cx-R-3} {cy-R-3} {cz-R-3} {cx+R+3} {cy+R+3} {cz+R+3} air replace")

    # Normal vector for Superlaser Dish (pointing from sphere center towards Pace)
    # Pace is at ~[116, 99, -226], Sphere is at [168, 155, -110]
    # Vector from center towards Pace & up:
    dish_elevation = math.radians(25) # 25 degrees above equator
    azimuth = math.atan2(-116, -52)   # direction to Pace in XZ plane
    
    nx = math.cos(dish_elevation) * math.cos(azimuth)
    ny = math.sin(dish_elevation)
    nz = math.cos(dish_elevation) * math.sin(azimuth)

    # Dish center on sphere surface
    dish_cx = cx + R * nx
    dish_cy = cy + R * ny
    dish_cz = cz + R * nz
    dish_radius = 9.5
    dish_max_depth = 4.0

    print(f"[*] Dish Center: [{dish_cx:.1f}, {dish_cy:.1f}, {dish_cz:.1f}], normal: [{nx:.3f}, {ny:.3f}, {nz:.3f}]")

    # Greeble block palette selector based on coordinate hash
    palette = [
        "stone", "stone_bricks", "smooth_stone", "stone",
        "stone_bricks", "polished_andesite", "stone", "chiseled_stone_bricks"
    ]

    # Generate the sphere shell layer by layer (dy from -R to +R)
    for dy in range(-R, R + 1):
        y = cy + dy
        r_outer_sq = max(0, R * R - dy * dy)
        r_outer = math.sqrt(r_outer_sq)

        # Check if in equatorial trench (dy in [-2, 2])
        is_trench = abs(dy) <= 2
        cur_R = (R - 2.5) if is_trench else R
        cur_r_outer_sq = max(0, cur_R * cur_R - dy * dy)
        cur_r_outer = math.sqrt(cur_r_outer_sq)

        cur_R_inner = cur_R - 1.8
        cur_r_inner_sq = max(0, cur_R_inner * cur_R_inner - dy * dy) if abs(dy) < cur_R_inner else 0
        cur_r_inner = math.sqrt(cur_r_inner_sq)

        max_dz = int(math.ceil(cur_r_outer))
        for dz in range(-max_dz, max_dz + 1):
            z = cz + dz
            # Calculate outer and inner x bounds
            dz_sq = dz * dz
            if dz_sq > cur_r_outer_sq:
                continue

            x_span_outer = int(math.floor(math.sqrt(cur_r_outer_sq - dz_sq)))
            x_span_inner = int(math.ceil(math.sqrt(cur_r_inner_sq - dz_sq))) if (cur_r_inner_sq > dz_sq) else 0

            # Left and Right hull segments
            # Negative side: [cx - x_span_outer .. cx - max(0, x_span_inner - 1)]
            # Positive side: [cx + max(0, x_span_inner - 1) .. cx + x_span_outer]
            segments = []
            if x_span_inner <= 0:
                segments.append((cx - x_span_outer, cx + x_span_outer))
            else:
                if cx - x_span_outer <= cx - x_span_inner:
                    segments.append((cx - x_span_outer, cx - x_span_inner))
                if cx + x_span_inner <= cx + x_span_outer:
                    segments.append((cx + x_span_inner, cx + x_span_outer))

            for x1, x2 in segments:
                # Select block type
                if is_trench:
                    block = "deepslate_tiles" if (dy == 0) else "polished_deepslate"
                else:
                    # Deterministic greeble selection
                    h = abs((x1 * 31 + y * 17 + z * 13) % len(palette))
                    block = palette[h]
                cmds.append(f"fill {x1} {y} {z} {x2} {y} {z} {block} replace")

    # =========================================================================
    # 2. EQUATORIAL TRENCH DETAILS (Turbolasers & Redstone Lighting)
    # =========================================================================
    # Add lighted conduit strips along the trench
    trench_radius = int(R - 2)
    for ang in range(0, 360, 15):
        rad = math.radians(ang)
        tx = int(cx + trench_radius * math.cos(rad))
        tz = int(cz + trench_radius * math.sin(rad))
        cmds.append(f"setblock {tx} {cy} {tz} redstone_lamp[lit=true] replace")
        cmds.append(f"setblock {tx} {cy+1} {tz} iron_bars replace")

    # =========================================================================
    # 3. CARVE AND LINING OF THE SUPERLASER CONCAVE DISH
    # =========================================================================
    # Carve the concave depression into the hull
    dish_box = int(dish_radius + 2)
    for d_dx in range(-dish_box, dish_box + 1):
        for d_dy in range(-dish_box, dish_box + 1):
            for d_dz in range(-dish_box, dish_box + 1):
                px = int(dish_cx + d_dx)
                py = int(dish_cy + d_dy)
                pz = int(dish_cz + d_dz)

                # Distance from dish center
                dist_dish_sq = (px - dish_cx)**2 + (py - dish_cy)**2 + (pz - dish_cz)**2
                if dist_dish_sq <= dish_radius * dish_radius:
                    dist_dish = math.sqrt(dist_dish_sq)
                    # Parabolic depth: deep in center, 0 at edge
                    depth = dish_max_depth * (1.0 - (dist_dish / dish_radius)**2)
                    
                    # Distance from sphere center
                    dist_sphere = math.sqrt((px - cx)**2 + (py - cy)**2 + (pz - cz)**2)
                    
                    # If this block is in the hollowed out carve zone
                    if dist_sphere > (R - depth - 0.5) and dist_sphere <= R + 1:
                        cmds.append(f"setblock {px} {py} {pz} air replace")
                    
                    # Dish lining surface
                    if abs(dist_sphere - (R - depth)) <= 0.8:
                        if dist_dish < 2.0:
                            cmds.append(f"setblock {px} {py} {pz} sea_lantern replace")
                        elif dist_dish < 5.0:
                            cmds.append(f"setblock {px} {py} {pz} polished_deepslate replace")
                        else:
                            cmds.append(f"setblock {px} {py} {pz} deepslate_tiles replace")

    # Superlaser Core Emitter at center of dish (Deepest point)
    emitter_x = int(dish_cx - dish_max_depth * nx)
    emitter_y = int(dish_cy - dish_max_depth * ny)
    emitter_z = int(dish_cz - dish_max_depth * nz)
    cmds.append(f"fill {emitter_x-1} {emitter_y-1} {emitter_z-1} {emitter_x+1} {emitter_y+1} {emitter_z+1} emerald_block replace")
    cmds.append(f"setblock {emitter_x} {emitter_y} {emitter_z} beacon replace")

    # 8 Tributary Convergence Beams converging onto Focal Point F
    # Focal point hovering 4 blocks in front of dish surface
    focal_x = int(dish_cx + 4.0 * nx)
    focal_y = int(dish_cy + 4.0 * ny)
    focal_z = int(dish_cz + 4.0 * nz)
    cmds.append(f"setblock {focal_x} {focal_y} {focal_z} sea_lantern replace")

    # Tributaries from 8 points around the rim
    for i in range(8):
        ang = i * (2 * math.pi / 8)
        # Vector perpendicular to normal (nx, ny, nz)
        # Choose a reference vector (0, 1, 0)
        v1_x = -nz
        v1_y = 0
        v1_z = nx
        v1_len = math.sqrt(v1_x*v1_x + v1_z*v1_z)
        if v1_len > 0.001:
            v1_x /= v1_len
            v1_z /= v1_len
        else:
            v1_x, v1_y, v1_z = 1, 0, 0

        # Second perpendicular vector v2 = n x v1
        v2_x = ny * v1_z - nz * v1_y
        v2_y = nz * v1_x - nx * v1_z
        v2_z = nx * v1_y - ny * v1_x

        rim_x = int(dish_cx + (dish_radius - 1.0) * (math.cos(ang) * v1_x + math.sin(ang) * v2_x))
        rim_y = int(dish_cy + (dish_radius - 1.0) * (math.cos(ang) * v1_y + math.sin(ang) * v2_y))
        rim_z = int(dish_cz + (dish_radius - 1.0) * (math.cos(ang) * v1_z + math.sin(ang) * v2_z))

        # Draw laser beam from rim to focal point
        steps = 6
        for s in range(steps + 1):
            t = s / steps
            bx = int(rim_x + t * (focal_x - rim_x))
            by = int(rim_y + t * (focal_y - rim_y))
            bz = int(rim_z + t * (focal_z - rim_z))
            cmds.append(f"setblock {bx} {by} {bz} lime_stained_glass replace")

    # Main Superlaser Beam firing forward from focal point across the sky (25 blocks)
    for step in range(1, 28):
        bx = int(focal_x + step * nx)
        by = int(focal_y + step * ny)
        bz = int(focal_z + step * nz)
        block = "sea_lantern" if (step % 4 == 0) else "lime_stained_glass"
        cmds.append(f"setblock {bx} {by} {bz} {block} replace")

    # =========================================================================
    # 4. EMPEROR'S THRONE ROOM (North Pole at Y = cy + 28..32)
    # =========================================================================
    tp_y = cy + 28
    # Throne room floor
    cmds.append(f"fill {cx-6} {tp_y} {cz-6} {cx+6} {tp_y} {cz+6} polished_blackstone replace")
    cmds.append(f"fill {cx-4} {tp_y} {cz-4} {cx+4} {tp_y} {cz+4} red_carpet replace")
    # Imperial Throne (center)
    cmds.append(f"setblock {cx} {tp_y+1} {cz-3} crying_obsidian replace")
    cmds.append(f"setblock {cx} {tp_y+2} {cz-3} polished_blackstone_stairs[facing=south] replace")
    cmds.append(f"setblock {cx-1} {tp_y+2} {cz-3} polished_blackstone_wall replace")
    cmds.append(f"setblock {cx+1} {tp_y+2} {cz-3} polished_blackstone_wall replace")
    # Spider-web observation window overlooking space (Facing South/Pace)
    cmds.append(f"fill {cx-4} {tp_y+1} {cz+5} {cx+4} {tp_y+3} {cz+5} tinted_glass replace")
    cmds.append(f"setblock {cx} {tp_y+2} {cz+5} iron_bars replace")
    # Emperor's Royal Guards Armor Stands
    cmds.append(f"setblock {cx-2} {tp_y+1} {cz-2} red_concrete replace")
    cmds.append(f"setblock {cx+2} {tp_y+1} {cz-2} red_concrete replace")

    # =========================================================================
    # 5. MAIN IMPERIAL HANGAR BAY (Equatorial Level in the Trench)
    # =========================================================================
    # Hangar bay facing West (-X)
    h_x = cx - int(cur_R) + 2
    cmds.append(f"fill {h_x} {cy-1} {cz-6} {h_x+10} {cy+2} {cz+6} air replace")
    cmds.append(f"fill {h_x} {cy-2} {cz-6} {h_x+10} {cy-2} {cz+6} polished_deepslate replace")
    # Magnetic containment forcefield (Cyan glass)
    cmds.append(f"fill {h_x} {cy-1} {cz-5} {h_x} {cy+2} {cz+5} light_blue_stained_glass replace")
    # Imperial TIE Fighter parked inside!
    tie_x = h_x + 5
    tie_z = cz
    # TIE Pod (Cockpit)
    cmds.append(f"setblock {tie_x} {cy} {tie_z} black_concrete replace")
    cmds.append(f"setblock {tie_x-1} {cy} {tie_z} tinted_glass replace") # Cockpit viewport
    # Pylons
    cmds.append(f"setblock {tie_x} {cy} {tie_z-1} polished_deepslate_wall replace")
    cmds.append(f"setblock {tie_x} {cy} {tie_z+1} polished_deepslate_wall replace")
    # Hexagonal Solar Wings (Left & Right)
    for wz in [tie_z - 2, tie_z + 2]:
        cmds.append(f"fill {tie_x-1} {cy-1} {wz} {tie_x+1} {cy+1} {wz} polished_blackstone replace")
        cmds.append(f"setblock {tie_x} {cy} {wz} iron_block replace")

    return cmds

def main():
    cx, cy, cz = 168, 155, -110
    R = 33
    print(f"[*] Compiling Death Star I commands at [{cx}, {cy}, {cz}] with radius {R}...")
    cmds = generate_death_star_commands(cx, cy, cz, R)

    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_death_star...")
    fn = run_cmd("function blueprints:build_death_star")
    print("Function:", fn)

    # Save Waypoint
    try:
        waypoint_file = "/home/pace/.config/mc-cli/waypoints.json"
        with open(waypoint_file, "r") as f:
            wps = json.load(f)
        wps = [w for w in wps if w.get("name") != "Death Star"]
        wps.append({"name": "Death Star", "x": cx, "y": cy + 29, "z": cz, "icon": "🌑"})
        with open(waypoint_file, "w") as f:
            json.dump(wps, f, indent=2)
        print("[*] Saved 'Death Star' waypoint")
    except Exception as e:
        print("[WARN] Could not save waypoint:", e)

    # Broadcast in-game
    run_cmd(
        'tellraw @a ['
        '{"text":"\\n🌑 [IMPERIAL ALERT] ","color":"red","bold":true},'
        '{"text":"A ","color":"gray"},'
        '{"text":"DEATH STAR I (Estrela da Morte)","color":"dark_red","bold":true},'
        '{"text":" emergiu do hiperespaço sobre o quadrante!\\n","color":"gray"},'
        '{"text":"   \\"That is no moon... It\'s a space station.\\"\\n","color":"yellow","italic":true}'
        ']'
    )
    
    run_cmd("playsound minecraft:entity.ender_dragon.growl master @a ~ ~ ~ 1.0 0.5")

if __name__ == "__main__":
    main()
