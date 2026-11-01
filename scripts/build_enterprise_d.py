#!/usr/bin/env python3
"""
build_enterprise_d.py - Generates the Galaxy-Class USS Enterprise NCC-1701-D
A grand monument in honor of Sir Patrick Stewart (Captain Jean-Luc Picard).

Position: cx = -65, cy = 138, cz = 100 (Dead center of Pace's line of sight)
Orientation: Facing North (-Z) or Southwest towards Castle/Pace.
Let's orient the ship with:
- Saucer forward towards Southwest/West or South (so Pace sees the majestic saucer, deflector, and bridge!)
Or let's orient the ship: Forward = -Z (North), Starboard = +X (East), Port = -X (West).
Wait! If Pace is at [-128, 127, 152] looking towards [-65, 138, 100], Pace is looking East-Northeast (+X, -Z).
If the Enterprise-D is angled or facing Southwest (towards Pace and the Castle), Pace will see the iconic elliptical saucer face-to-face, with the bridge dome, Ten Forward windows, and deflector dish shining directly at Pace!
Let's see: If forward is towards South-West (-X, +Z), or if forward is -Z:
If forward is South (+Z), the saucer is at the front (towards Pace!), and the nacelles trail back north.
Let's check:
Pace is at [-128, 152].
Ship is at [-65, 100].
Vector from ship to Pace: dx = -63, dz = +52 (Towards South-West).
If the ship's forward axis is oriented along South (+Z) or facing -X, +Z (angled 45 degrees towards the castle), it will look breathtaking!
A standard alignment (Forward = +Z, so Saucer is at +Z, Engineering hull at -Z) means:
- The Saucer is closest to Pace!
- The glowing Navigational Deflector points towards +Z (directly visible from below and front)!
- The bridge on top of the saucer is easily accessible and looks forward towards the Castle/Sanctuary!
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_enterprise_d.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_commands(cx, cy, cz):
    cmds = []
    
    # 0. Safety & gamerules
    cmds.append("gamerule commandModificationBlockLimit 2000000")
    
    # Forward is +Z (towards Castle/South), Aft is -Z. Starboard is +X, Port is -X.
    # Center of Saucer: [cx, cy, cz]
    
    # Clear sky volume around the ship
    # Saucer width: 48 (X: cx-24..cx+24), Saucer length: 36 (Z: cz-18..cz+18)
    # Secondary hull & nacelles extend to cz-45, X: cx-28..cx+28, Y: cy-12..cy+16
    cmds.append(f"fill {cx-30} {cy-14} {cz-48} {cx+30} {cy+16} {cz+22} air replace")
    
    # =========================================================================
    # 1. PRIMARY HULL: THE ELLIPTICAL SAUCER SECTION (Galaxy-Class)
    # =========================================================================
    # Ellipse radii: Rx = 23 (semi-major axis, width), Rz = 16 (semi-minor axis, length)
    
    # Deck 10: Lower saucer rim & Ten Forward (cy-1)
    # Tapered bottom hull (cy-2 to cy-4)
    for dz in range(-12, 13):
        rz = dz / 12.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 17)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy-2} {cz+dz} {cx+max_x} {cy-2} {cz+dz} smooth_quartz replace")
            
    for dz in range(-8, 9):
        rz = dz / 8.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 11)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy-3} {cz+dz} {cx+max_x} {cy-3} {cz+dz} smooth_quartz replace")
            
    # Saucer Main Floor (cy) - Wide Ellipse Rx=23, Rz=16
    for dz in range(-16, 17):
        rz = dz / 16.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 23)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy} {cz+dz} {cx+max_x} {cy} {cz+dz} smooth_quartz replace")

    # Saucer Ceiling / Upper Deck Floor (cy+3)
    for dz in range(-15, 16):
        rz = dz / 15.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 22)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy+3} {cz+dz} {cx+max_x} {cy+3} {cz+dz} smooth_quartz replace")

    # Outer Saucer Rim Walls & Windows (cy+1 and cy+2)
    for dz in range(-16, 17):
        rz = dz / 16.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 23)
        if max_x > 0:
            # Alternate quartz and light blue stained glass
            block = "light_blue_stained_glass" if (abs(dz) % 3 == 0) else "smooth_quartz"
            cmds.append(f"fill {cx-max_x} {cy+1} {cz+dz} {cx-max_x} {cy+2} {cz+dz} {block} replace")
            cmds.append(f"fill {cx+max_x} {cy+1} {cx-max_x+max_x*2} {cy+2} {cz+dz} {block} replace" if False else f"fill {cx+max_x} {cy+1} {cz+dz} {cx+max_x} {cy+2} {cz+dz} {block} replace")

    # Ten Forward Lounge (Front tip of Saucer at Z = cz + 13..cz + 16)
    # Panoramic observation windows
    cmds.append(f"fill {cx-6} {cy+1} {cz+15} {cx+6} {cy+2} {cz+15} light_blue_stained_glass replace")
    cmds.append(f"fill {cx-4} {cy+1} {cz+16} {cx+4} {cy+2} {cz+16} tinted_glass replace")
    # Ten forward bar counter and cozy interior
    cmds.append(f"fill {cx-3} {cy+1} {cz+13} {cx+3} {cy+1} {cz+13} dark_oak_stairs[facing=south] replace")
    cmds.append(f"setblock {cx-2} {cy+1} {cz+12} brewing_stand replace")
    cmds.append(f"setblock {cx+2} {cy+1} {cz+12} brewing_stand replace")
    cmds.append(f"setblock {cx} {cy+1} {cz+11} lantern[hanging=false] replace")

    # Upper Saucer Terracing / Superstructure (cy+4 and cy+5)
    for dz in range(-12, 13):
        rz = dz / 12.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 16)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy+4} {cz+dz} {cx+max_x} {cy+4} {cz+dz} smooth_quartz replace")

    for dz in range(-8, 9):
        rz = dz / 8.0
        max_x = int(math.sqrt(max(0, 1.0 - rz*rz)) * 10)
        if max_x >= 0:
            cmds.append(f"fill {cx-max_x} {cy+5} {cz+dz} {cx+max_x} {cy+5} {cz+dz} smooth_quartz replace")

    # =========================================================================
    # 2. DECK 1: MAIN BRIDGE (The Command Center of Captain Jean-Luc Picard)
    # =========================================================================
    # Bridge Dome at cy+6, Z: cz-2..cz+4, X: cx-5..cx+5
    cmds.append(f"fill {cx-5} {cy+6} {cz-2} {cx+5} {cy+6} {cz+4} smooth_quartz replace") # Bridge floor
    cmds.append(f"fill {cx-4} {cy+6} {cz-1} {cx+4} {cy+6} {cz+3} red_carpet replace")   # Bridge carpeting

    # Bridge Walls & Skylight
    cmds.append(f"fill {cx-5} {cy+7} {cz-2} {cx-5} {cy+8} {cz+4} polished_blackstone_brick_wall replace")
    cmds.append(f"fill {cx+5} {cy+7} {cz-2} {cx+5} {cy+8} {cz+4} polished_blackstone_brick_wall replace")
    cmds.append(f"fill {cx-4} {cy+7} {cz-2} {cx+4} {cy+8} {cz-2} polished_deepslate replace") # Aft bulkhead
    cmds.append(f"fill {cx-4} {cy+9} {cz-1} {cx+4} {cy+9} {cz+3} tinted_glass replace")       # Overhead skylight dome

    # Main Viewscreen (Facing Forward / South +Z)
    cmds.append(f"fill {cx-3} {cy+7} {cz+4} {cx+3} {cy+8} {cz+4} black_concrete replace")
    cmds.append(f"fill {cx-2} {cy+7} {cz+4} {cx+2} {cy+8} {cz+4} light_blue_stained_glass replace")
    cmds.append(f"setblock {cx} {cy+7} {cz+4} sea_lantern replace")

    # Conn and Ops (Forward Consoles in front of Viewscreen)
    cmds.append(f"setblock {cx-2} {cy+7} {cz+2} polished_blackstone_stairs[facing=south] replace") # Ops (Data)
    cmds.append(f"setblock {cx+2} {cy+7} {cz+2} polished_blackstone_stairs[facing=south] replace") # Conn (La Forge/Crusher)
    cmds.append(f"setblock {cx-2} {cy+7} {cz+3} daylight_detector replace")
    cmds.append(f"setblock {cx+2} {cy+7} {cz+3} daylight_detector replace")

    # Captain's Chair (Center - Jean-Luc Picard)
    cmds.append(f"setblock {cx} {cy+7} {cz} dark_oak_stairs[facing=south] replace")
    cmds.append(f"setblock {cx-1} {cy+7} {cz} oak_stairs[facing=south] replace") # Counselor Troi
    cmds.append(f"setblock {cx+1} {cy+7} {cz} oak_stairs[facing=south] replace") # Commander Riker

    # Curved Tactical Console (The Wooden Horseshoe behind Picard - Worf's Station)
    cmds.append(f"fill {cx-2} {cy+7} {cz-1} {cx+2} {cy+7} {cz-1} stripped_oak_wood replace")
    cmds.append(f"setblock {cx-3} {cy+7} {cz} stripped_oak_wood replace")
    cmds.append(f"setblock {cx+3} {cy+7} {cz} stripped_oak_wood replace")
    cmds.append(f"setblock {cx} {cy+8} {cz-1} polished_blackstone_pressure_plate replace")
    cmds.append(f"setblock {cx-1} {cy+8} {cz-1} heavy_weighted_pressure_plate replace")
    cmds.append(f"setblock {cx+1} {cy+8} {cz-1} light_weighted_pressure_plate replace")

    # Starfleet Memorial Plaque for Sir Patrick Stewart / Picard on Bridge Aft Bulkhead
    cmds.append(f'setblock {cx} {cy+7} {cz-2} chiseled_bookshelf[facing=south] replace')
    sign_nbt = (
        '{front_text:{messages:['
        '\'["",{"text":"USS ENTERPRISE","color":"gold","bold":true}]\','
        '\'["",{"text":"NCC-1701-D","color":"yellow","bold":true}]\','
        '\'["",{"text":"In Memory of","color":"white"}]\','
        '\'["",{"text":"Sir Patrick Stewart","color":"aqua","bold":true}]\''
        ']}}'
    )
    cmds.append(f'setblock {cx} {cy+8} {cz-2} oak_wall_sign[facing=south]{sign_nbt} replace')

    # Turbolift Shaft (Vertical ladder from Bridge cy+6 down to Saucer Main Deck cy+1)
    cmds.append(f"fill {cx+4} {cy+1} {cz-1} {cx+4} {cy+7} {cz-1} air replace")
    cmds.append(f"fill {cx+4} {cy+1} {cz-1} {cx+4} {cy+6} {cz-1} ladder[facing=east] replace")

    # Saucer Registry Decals on Outer Dorsal Hull
    # "U.S.S. ENTERPRISE NCC-1701-D" represented in red/black terracotta stripes
    cmds.append(f"fill {cx-12} {cy+4} {cz+6} {cx+12} {cy+4} {cz+6} red_concrete replace")
    cmds.append(f"fill {cx-8} {cy+4} {cz+7} {cx+8} {cy+4} {cz+7} black_concrete replace")
    cmds.append(f"fill {cx-10} {cy+4} {cz+8} {cx+10} {cy+4} {cz+8} red_concrete replace")

    # =========================================================================
    # 3. DORSAL INTERCONNECTING NECK & BATTLE BRIDGE
    # =========================================================================
    # Connects Saucer (Z: cz-10..cz-15) down to Engineering Hull (cy-6..cy)
    cmds.append(f"fill {cx-3} {cy-6} {cz-16} {cx+3} {cy} {cz-10} smooth_quartz replace")
    cmds.append(f"fill {cx-2} {cy-5} {cz-15} {cx+2} {cy-1} {cz-11} air replace") # Neck interior corridor
    cmds.append(f"fill {cx-2} {cy-6} {cz-15} {cx+2} {cy-6} {cz-11} polished_andesite replace")
    # Turbolift between hulls
    cmds.append(f"fill {cx} {cy-6} {cz-11} {cx} {cy} {cz-11} ladder[facing=north] replace")

    # =========================================================================
    # 4. SECONDARY HULL (ENGINEERING SECTION)
    # =========================================================================
    # Suspended from cy-10 to cy-5, Z: cz-38 to cz-14, Width: cx-6 to cx+6
    cmds.append(f"fill {cx-6} {cy-10} {cz-38} {cx+6} {cy-5} {cz-14} smooth_quartz replace")
    cmds.append(f"fill {cx-5} {cy-9} {cz-37} {cx+5} {cy-6} {cz-15} air replace") # Main Engineering Deck
    cmds.append(f"fill {cx-5} {cy-10} {cz-37} {cx+5} {cy-10} {cz-15} gray_concrete replace") # Engineering floor

    # Warp Core in Main Engineering (Pulsing Vertical Conduit)
    cmds.append(f"fill {cx} {cy-9} {cz-24} {cx} {cy-6} {cz-24} sea_lantern replace")
    cmds.append(f"fill {cx-1} {cy-9} {cz-24} {cx-1} {cy-6} {cz-24} cyan_stained_glass replace")
    cmds.append(f"fill {cx+1} {cy-9} {cz-24} {cx+1} {cy-6} {cz-24} cyan_stained_glass replace")
    cmds.append(f"fill {cx} {cy-9} {cz-25} {cx} {cy-6} {cz-25} cyan_stained_glass replace")
    cmds.append(f"fill {cx} {cy-9} {cz-23} {cx} {cy-6} {cz-23} cyan_stained_glass replace")
    # Engineering consoles (Geordi La Forge's station)
    cmds.append(f"setblock {cx-2} {cy-9} {cz-24} polished_blackstone_stairs[facing=east] replace")
    cmds.append(f"setblock {cx+2} {cy-9} {cz-24} polished_blackstone_stairs[facing=west] replace")

    # -------------------------------------------------------------------------
    # NAVIGATIONAL DEFLECTOR DISH (The Iconic Golden/Copper Glow on Engineering Fore)
    # At Z: cz-14 (Facing Forward +Z towards the Saucer underside and world)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-4} {cy-9} {cz-14} {cx+4} {cy-6} {cz-14} cut_copper replace")
    cmds.append(f"fill {cx-3} {cy-9} {cz-14} {cx+3} {cy-6} {cz-14} glowstone replace")
    cmds.append(f"fill {cx-2} {cy-8} {cz-14} {cx+2} {cy-7} {cz-14} sea_lantern replace")
    cmds.append(f"fill {cx-1} {cy-8} {cz-13} {cx+1} {cy-7} {cz-13} light_blue_stained_glass replace") # Deflector focal field

    # Main Shuttlebay (Aft of Engineering Hull at Z: cz-38)
    cmds.append(f"fill {cx-4} {cy-7} {cz-38} {cx+4} {cy-5} {cz-38} smooth_stone_slab[type=double] replace")
    cmds.append(f"fill {cx-3} {cy-7} {cz-38} {cx+3} {cy-6} {cz-38} yellow_stained_glass replace") # Bay blast door

    # =========================================================================
    # 5. WARP PYLONS & DUAL WARP NACELLES
    # =========================================================================
    # Galaxy-Class Pylons sweep outward (+/- X) and upward (+Y)
    # Left Pylon (Port, -X): from [cx-6, cy-8, cz-30] to [cx-18, cy-2, cz-32]
    # Right Pylon (Starboard, +X): from [cx+6, cy-8, cz-30] to [cx+18, cy-2, cz-32]
    for step in range(12):
        px = 6 + step
        py = -8 + int(step * 0.6)
        pz = -30 - int(step * 0.25)
        # Port
        cmds.append(f"fill {cx-px-1} {cy+py} {cz+pz-2} {cx-px} {cy+py+1} {cz+pz+2} smooth_quartz replace")
        # Starboard
        cmds.append(f"fill {cx+px} {cy+py} {cz+pz-2} {cx+px+1} {cy+py+1} {cz+pz+2} smooth_quartz replace")

    # WARP NACELLES: Port at X = cx - 19, Starboard at X = cx + 19
    # Nacelle length: 32 blocks (Z: cz-44 to cz-12), Y: cy-3 to cy
    for side_sign, nx in [(-1, cx - 19), (1, cx + 19)]:
        # Solid Nacelle Body
        cmds.append(f"fill {nx-2} {cy-3} {cz-44} {nx+2} {cy} {cz-12} smooth_quartz replace")
        
        # Bussard Ramscoop (Front of Nacelle facing +Z at cz-12): Glowing Red Antiproton Scoop!
        cmds.append(f"fill {nx-2} {cy-3} {cz-12} {nx+2} {cy} {cz-12} redstone_block replace")
        cmds.append(f"fill {nx-1} {cy-2} {cz-11} {nx+1} {cy-1} {cz-11} shroomlight replace")
        cmds.append(f"fill {nx-1} {cy-2} {cz-10} {nx+1} {cy-1} {cz-10} red_stained_glass replace")

        # Warp Field Grilles (Inboard and Outboard glowing Cyan/Blue):
        # Inboard facing center:
        inboard_x = nx + (2 if side_sign < 0 else -2)
        cmds.append(f"fill {inboard_x} {cy-2} {cz-40} {inboard_x} {cy-1} {cz-16} sea_lantern replace")
        cmds.append(f"fill {inboard_x} {cy-2} {cz-40} {inboard_x} {cy-1} {cz-16} cyan_stained_glass replace" if False else f"fill {inboard_x} {cy-2} {cz-40} {inboard_x} {cy-1} {cz-16} light_blue_stained_glass replace")
        
        # Outboard strip
        outboard_x = nx + (-2 if side_sign < 0 else 2)
        cmds.append(f"fill {outboard_x} {cy-2} {cz-38} {outboard_x} {cy-1} {cz-18} polished_deepslate replace")

        # Impulse Exhaust Vents on Aft (cz-44)
        cmds.append(f"fill {nx-1} {cy-2} {cz-44} {nx+1} {cy-1} {cz-44} red_concrete replace")

    # Navigation Marker Beacons:
    # Port Wingtip (Red Beacon):
    cmds.append(f"setblock {cx-21} {cy+1} {cz-28} redstone_lamp[lit=true] replace")
    cmds.append(f"setblock {cx-21} {cy+2} {cz-28} red_stained_glass replace")
    # Starboard Wingtip (Green Beacon):
    cmds.append(f"setblock {cx+21} {cy+1} {cz-28} sea_lantern replace")
    cmds.append(f"setblock {cx+21} {cy+2} {cz-28} lime_stained_glass replace")

    # =========================================================================
    # 6. BEACON OF ETERNAL HONOR (Memorial Beam to the Stars)
    # =========================================================================
    # A beacon atop the bridge dome projecting a sky-high beam of light
    cmds.append(f"fill {cx-1} {cy+9} {cz-1} {cx+1} {cy+9} {cz+1} netherite_block replace")
    cmds.append(f"setblock {cx} {cy+10} {cz} beacon replace")
    cmds.append(f"setblock {cx} {cy+11} {cz} light_blue_stained_glass replace")

    return cmds

def main():
    # Coords matching Pace's sightline:
    cx, cy, cz = -65, 138, 100
    
    print(f"[*] Compiling USS Enterprise NCC-1701-D commands at [{cx}, {cy}, {cz}]...")
    cmds = generate_commands(cx, cy, cz)
    
    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_enterprise_d...")
    fn = run_cmd("function blueprints:build_enterprise_d")
    print("Function:", fn)

    # Broadcast memorial tribute in chat
    run_cmd(
        'tellraw @a ['
        '{"text":"\\n✨ [HOMENAGEM] ","color":"gold","bold":true},'
        '{"text":"A ","color":"yellow"},'
        '{"text":"USS ENTERPRISE NCC-1701-D (Galaxy-Class)","color":"aqua","bold":true},'
        '{"text":" entrou em órbita nos céus!\\n","color":"yellow"},'
        '{"text":"\\n   \\"Things are only impossible until they are not.\\"","color":"gray","italic":true},'
        '{"text":"\\n   — Capitão Jean-Luc Picard (Sir Patrick Stewart, 1940-2026)\\n","color":"gold"}'
        ']'
    )
    
    run_cmd('playsound minecraft:ui.toast.challenge_complete master @a ~ ~ ~ 1.0 1.0')

if __name__ == "__main__":
    main()
