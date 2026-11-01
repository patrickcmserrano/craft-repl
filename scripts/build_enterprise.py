#!/usr/bin/env python3
"""
build_enterprise.py - Generates the Starship USS Enterprise NCC-1701 hovering in Minecraft
- Circular Primary Saucer Section (32m diameter, true circular geometry, hollow)
- Bridge Deck with Viewscreen, Captain's Chair, Consoles & Transporter Room
- Dorsal Neck with Turbolift ladder
- Engineering Hull with glowing Deflector Dish & Shuttlebay
- Angled Warp Pylons
- Two 38-block Warp Nacelles with glowing Bussard Ramscoops & Cyan Warp Grills
- Teleports Pace and Lizzie to the Bridge with sound effects
"""

import sys
import json
import time
import math
import urllib.request

API_URL = "http://localhost:8090/api/run"
TELEM_URL = "http://localhost:8090/api/telemetry"
WAYPOINTS_FILE = "/home/pace/.config/mc-cli/waypoints.json"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def send_tellraw(msg, color="aqua", bold=False):
    escaped = msg.replace('"', '\\"').replace("\n", " ")
    b = ',"bold":true' if bold else ''
    run_cmd(f'tellraw @a [{{"text":"[Antigravity] ","color":"light_purple","bold":true}},{{"text":"{escaped}","color":"{color}"{b}}}]          ')

def get_player_coords(player="Pace"):
    try:
        with urllib.request.urlopen(TELEM_URL, timeout=4) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            for p in data.get("players", []):
                if p.get("name", "").lower() == player.lower():
                    return int(round(p.get("x"))), int(round(p.get("y"))), int(round(p.get("z")))
    except Exception:
        pass
    return -200, 65, 90

def add_waypoint(name, x, y, z, icon="🛸"):
    try:
        with open(WAYPOINTS_FILE, "r") as f:
            waypoints = json.load(f)
        # Avoid duplicates
        waypoints = [w for w in waypoints if w.get("name") != name]
        waypoints.append({"name": name, "x": x, "y": y, "z": z, "icon": icon})
        with open(WAYPOINTS_FILE, "w") as f:
            json.dump(waypoints, f, indent=2)
        print(f"[*] Waypoint '{name}' saved to {WAYPOINTS_FILE}")
    except Exception as e:
        print(f"[WARN] Could not update waypoint: {e}")

def generate_enterprise_cmds(cx, cy, cz):
    cmds = []
    
    # 0. Safety & rules
    cmds.append("gamerule commandModificationBlockLimit 2000000")
    cmds.append("effect give @a minecraft:resistance 60 255 true")
    cmds.append("effect give @a minecraft:slow_falling 60 1 true")

    # Clear space for the ship: 70 wide (X), 35 tall (Y), 100 long (Z)
    cmds.append(f"fill {cx-35} {cy-15} {cz-50} {cx+35} {cy+20} {cz+25} air replace")

    # 1. CIRCULAR SAUCER SECTION (Primary Hull) - Center at [cx, cy, cz]
    # Saucer Radius = 16 blocks. Forward is +Z, Aft is -Z.
    
    # Saucer Floor (cy) - Solid disc
    for dz in range(-16, 17):
        rx = int(math.sqrt(max(0, 16*16 - dz*dz)))
        if rx >= 0:
            cmds.append(f"fill {cx-rx} {cy} {cz+dz} {cx+rx} {cy} {cz+dz} smooth_quartz replace")

    # Saucer Ceiling (cy+3) - Solid disc
    for dz in range(-16, 17):
        rx = int(math.sqrt(max(0, 16*16 - dz*dz)))
        if rx >= 0:
            cmds.append(f"fill {cx-rx} {cy+3} {cz+dz} {cx+rx} {cy+3} {cz+dz} smooth_quartz replace")

    # Saucer Outer Walls (cy+1 and cy+2) - Rim rings with windows
    for dz in range(-16, 17):
        rx = int(math.sqrt(max(0, 16*16 - dz*dz)))
        if rx > 0:
            # Alternate windows and quartz
            block = "light_blue_stained_glass" if (abs(dz) % 4 == 0) else "smooth_quartz"
            cmds.append(f"fill {cx-rx} {cy+1} {cz+dz} {cx-rx} {cy+2} {cz+dz} {block} replace")
            cmds.append(f"fill {cx+rx} {cy+1} {cz+dz} {cx+rx} {cy+2} {cz+dz} {block} replace")

    # Bottom taper disc (cy-1) - Radius 12 disc
    for dz in range(-12, 13):
        rx = int(math.sqrt(max(0, 12*12 - dz*dz)))
        if rx >= 0:
            cmds.append(f"fill {cx-rx} {cy-1} {cz+dz} {cx+rx} {cy-1} {cz+dz} smooth_quartz replace")

    # Saucer Upper Deck / Bridge structure (cy+4 to cy+6)
    # Radius 7 disc at cy+4
    for dz in range(-7, 8):
        rx = int(math.sqrt(max(0, 7*7 - dz*dz)))
        if rx >= 0:
            cmds.append(f"fill {cx-rx} {cy+4} {cz+dz} {cx+rx} {cy+4} {cz+dz} smooth_quartz replace")

    # Bridge Outer Wall at cy+4, cy+5
    for dz in range(-5, 6):
        rx = int(math.sqrt(max(0, 5*5 - dz*dz)))
        if rx > 0:
            cmds.append(f"fill {cx-rx} {cy+4} {cz+dz} {cx-rx} {cy+5} {cz+dz} smooth_quartz replace")
            cmds.append(f"fill {cx+rx} {cy+4} {cz+dz} {cx+rx} {cy+5} {cz+dz} smooth_quartz replace")

    # Bridge Dome & Skylight (cy+5, cy+6)
    cmds.append(f"fill {cx-3} {cy+6} {cz-3} {cx+3} {cy+6} {cz+3} smooth_quartz replace")
    cmds.append(f"fill {cx-1} {cy+6} {cz-1} {cx+1} {cy+6} {cz+1} sea_lantern replace")
    cmds.append(f"setblock {cx} {cy+7} {cz} tinted_glass replace")

    # Red Starfleet registry markings on upper saucer
    cmds.append(f"fill {cx-12} {cy+4} {cz-3} {cx-12} {cy+4} {cz+3} red_concrete replace")
    cmds.append(f"fill {cx+12} {cy+4} {cz-3} {cx+12} {cy+4} {cz+3} red_concrete replace")
    # NCC-1701 decal
    cmds.append(f"fill {cx-4} {cy+4} {cz+9} {cx+4} {cy+4} {cz+9} red_concrete replace")

    # Impulse Engine at rear of saucer (cz - 16, cy+1 to cy+2)
    cmds.append(f"fill {cx-4} {cy+1} {cz-16} {cx+4} {cy+2} {cz-16} redstone_lamp replace")
    cmds.append(f"fill {cx-4} {cy+1} {cz-17} {cx+4} {cy+2} {cz-17} red_stained_glass replace")
    cmds.append(f"fill {cx-2} {cy} {cz-16} {cx+2} {cy} {cz-16} redstone_block replace")

    # 2. DORSAL NECK (Connecting Saucer to Engineering Hull)
    # From cy-1 down to cy-8, Z: cz-10 to cz-2
    cmds.append(f"fill {cx-1} {cy-8} {cz-10} {cx+1} {cy-1} {cz-2} smooth_quartz replace")
    # Turbolift shaft inside neck with ladders and lights
    cmds.append(f"fill {cx} {cy-8} {cz-6} {cx} {cy} {cz-6} air replace")
    cmds.append(f"fill {cx} {cy-8} {cz-5} {cx} {cy} {cz-5} ladder[facing=north] replace")
    cmds.append(f"setblock {cx} {cy-4} {cz-7} sea_lantern replace")

    # 3. SECONDARY ENGINEERING HULL (Stardrive Section)
    # Cylindrical body: X: cx-3 to cx+3, Y: cy-12 to cy-7, Z: cz-36 to cz-10
    cmds.append(f"fill {cx-3} {cy-12} {cz-36} {cx+3} {cy-7} {cz-10} iron_block hollow")
    cmds.append(f"fill {cx-2} {cy-11} {cz-35} {cx+2} {cy-8} {cz-11} air replace") # clear interior

    # Navigational Deflector Dish (Front of Engineering: cz - 10)
    cmds.append(f"fill {cx-2} {cy-11} {cz-10} {cx+2} {cy-8} {cz-10} copper_block replace")
    cmds.append(f"fill {cx-1} {cy-10} {cz-9} {cx+1} {cy-9} {cz-9} sea_lantern replace")
    cmds.append(f"fill {cx-1} {cy-10} {cz-8} {cx+1} {cy-9} {cz-8} light_blue_stained_glass replace")

    # Shuttlebay (Rear of Engineering: cz - 36)
    cmds.append(f"fill {cx-2} {cy-10} {cz-36} {cx+2} {cy-8} {cz-36} tinted_glass replace")
    cmds.append(f"fill {cx-1} {cy-11} {cz-37} {cx+1} {cy-11} {cz-37} polished_blackstone_slab[type=top] replace")

    # 4. WARP PYLONS (Wings connecting Engineering to Nacelles)
    for step in range(13):
        px = 4 + step
        py = cy - 9 + int(step * 0.95)
        pz = cz - 24
        # Port (Left) Pylon
        cmds.append(f"fill {cx-px} {py} {pz-1} {cx-px} {py+1} {pz+1} smooth_quartz replace")
        # Starboard (Right) Pylon
        cmds.append(f"fill {cx+px} {py} {pz-1} {cx+px} {py+1} {pz+1} smooth_quartz replace")

    # 5. WARP NACELLES (Two Colossal Warp Engines)
    # Port (cx - 17), Starboard (cx + 17)
    # Length: 38 blocks (cz-44 to cz-6), Height: cy+2 to cy+5
    for nx in [cx - 17, cx + 17]:
        # Nacelle main hollow body (4x4x38)
        cmds.append(f"fill {nx-1} {cy+2} {cz-44} {nx+2} {cy+5} {cz-6} smooth_quartz hollow")
        cmds.append(f"fill {nx} {cy+3} {cz-43} {nx+1} {cy+4} {cz-7} air replace")
        
        # Warp Plasma Grills (Glowing Blue along the inner side)
        if nx < cx: # Port nacelle (faces +X towards center)
            cmds.append(f"fill {nx+1} {cy+3} {cz-38} {nx+1} {cy+4} {cz-12} sea_lantern replace")
            cmds.append(f"fill {nx+2} {cy+3} {cz-38} {nx+2} {cy+4} {cz-12} light_blue_stained_glass replace")
        else:       # Starboard nacelle (faces -X towards center)
            cmds.append(f"fill {nx} {cy+3} {cz-38} {nx} {cy+4} {cz-12} sea_lantern replace")
            cmds.append(f"fill {nx-1} {cy+3} {cz-38} {nx-1} {cy+4} {cz-12} light_blue_stained_glass replace")

        # Bussard Ramscoop Collectors (Glowing Red at FRONT: cz-6 to cz-4)
        cmds.append(f"fill {nx} {cy+3} {cz-6} {nx+1} {cy+4} {cz-6} shroomlight replace")
        cmds.append(f"fill {nx} {cy+3} {cz-5} {nx+1} {cy+4} {cz-5} red_stained_glass replace")
        cmds.append(f"setblock {nx} {cy+3} {cz-4} red_stained_glass replace")

        # Nacelle rear exhaust / finial (cz-45)
        cmds.append(f"fill {nx} {cy+3} {cz-45} {nx+1} {cy+4} {cz-45} chiseled_stone_bricks replace")

    # 6. BRIDGE INTERIOR & FURNISHINGS (Deck 1 of Saucer at cy+1)
    # Floor: polished blackstone / deepslate
    cmds.append(f"fill {cx-5} {cy} {cz-5} {cx+5} {cy} {cz+5} polished_deepslate replace")
    
    # Red carpet runner leading from Transporter pad to Captain's Chair
    cmds.append(f"fill {cx} {cy+1} {cz-4} {cx} {cy+1} {cz+1} red_carpet replace")

    # Captain's Chair (Center of Bridge at cx, cz)
    cmds.append(f"setblock {cx} {cy+1} {cz} smooth_quartz_stairs[facing=south,half=bottom] replace")
    cmds.append(f"setblock {cx-1} {cy+1} {cz} smooth_quartz_slab[type=bottom] replace")
    cmds.append(f"setblock {cx+1} {cy+1} {cz} smooth_quartz_slab[type=bottom] replace")
    cmds.append(f"setblock {cx} {cy+2} {cz-1} red_banner replace")

    # Helm & Navigation Consoles (In front of Captain: cz+3)
    cmds.append(f"setblock {cx-1} {cy+1} {cz+3} blast_furnace[facing=south] replace")
    cmds.append(f"setblock {cx} {cy+1} {cz+3} crafting_table replace")
    cmds.append(f"setblock {cx+1} {cy+1} {cz+3} smithing_table replace")
    cmds.append(f"setblock {cx-1} {cy+2} {cz+3} polished_blackstone_pressure_plate replace")
    cmds.append(f"setblock {cx+1} {cy+2} {cz+3} polished_blackstone_pressure_plate replace")

    # Main Forward Viewscreen (Front wall of bridge: cz+5 to cz+6)
    cmds.append(f"fill {cx-3} {cy+1} {cz+5} {cx+3} {cy+3} {cz+5} air replace")
    cmds.append(f"fill {cx-3} {cy+1} {cz+6} {cx+3} {cy+3} {cz+6} tinted_glass replace") # Viewscreen glass looking out into the world!
    cmds.append(f"fill {cx-2} {cy} {cz+6} {cx+2} {cy} {cz+6} sea_lantern replace") # Glowing border

    # Science, Spock & Engineering stations on the side walls
    cmds.append(f"setblock {cx-4} {cy+1} {cz} enchanting_table replace")
    cmds.append(f"setblock {cx-4} {cy+1} {cz+1} barrel replace")
    cmds.append(f"setblock {cx-4} {cy+1} {cz-1} ender_chest replace")
    cmds.append(f"setblock {cx+4} {cy+1} {cz} anvil replace")
    cmds.append(f"setblock {cx+4} {cy+1} {cz+1} chest replace")
    cmds.append(f"setblock {cx+4} {cy+1} {cz-1} lectern[facing=west] replace")

    # Transporter Pad at rear of Bridge (cz-5)
    cmds.append(f"fill {cx-1} {cy} {cz-5} {cx+1} {cy} {cz-5} sea_lantern replace")
    cmds.append(f"fill {cx-1} {cy+1} {cz-5} {cx+1} {cy+1} {cz-5} light_blue_carpet replace")

    # Ceiling Lighting
    cmds.append(f"setblock {cx-2} {cy+3} {cz-2} soul_lantern[hanging=true] replace")
    cmds.append(f"setblock {cx+2} {cy+3} {cz-2} soul_lantern[hanging=true] replace")
    cmds.append(f"setblock {cx-2} {cy+3} {cz+2} soul_lantern[hanging=true] replace")
    cmds.append(f"setblock {cx+2} {cy+3} {cz+2} soul_lantern[hanging=true] replace")

    # 7. Teleportation of crew to the Bridge Transporter Room
    # Beam up Pace and Lizzie!
    for p in ["Pace", "Lizzie", "pace", "lizzie"]:
        cmds.append(f"tp {p} {cx} {cy+1} {cz-5} 0 0")
    
    cmds.append(f"playsound minecraft:block.beacon.activate master @a {cx} {cy+1} {cz-5} 2 1")
    cmds.append(f"playsound minecraft:ui.toast.challenge_complete master @a {cx} {cy+1} {cz-5} 2 1")

    return cmds

def main():
    player = "Pace"
    if len(sys.argv) > 1:
        player = sys.argv[1]
    
    px, py, pz = get_player_coords(player)
    cx = px
    cy = max(120, py + 50) # High altitude hovering
    cz = pz

    print(f"[*] Constructing USS Enterprise NCC-1701 for {player} at [{cx}, {cy}, {cz}]...")
    send_tellraw(f"🛸 Iniciando construção da USS Enterprise NCC-1701 a {cy}m de altitude...", color="gold", bold=True)
    
    cmds = generate_enterprise_cmds(cx, cy, cz)
    total = len(cmds)
    print(f"[*] Total commands to execute: {total}")

    for i, c in enumerate(cmds):
        run_cmd(c)
        if i % 25 == 0:
            pct = int((i / total) * 100)
            print(f"[{pct}%] {c[:60]}...")
            time.sleep(0.02)
    
    # Save to waypoints
    add_waypoint("🛸 USS Enterprise NCC-1701", cx, cy + 1, cz)

    send_tellraw(f"🖖 USS Enterprise NCC-1701 construída! Tripulação transportada para a Ponte de Comando!", color="green", bold=True)
    send_tellraw(f"🚀 Capitão no convés! Olhe pela tela de visualização ou sente na cadeira de comando.", color="aqua")
    print("[*] USS Enterprise built and crew beamed up successfully!")

if __name__ == "__main__":
    main()
