#!/usr/bin/env python3
"""
build_enterprise_d_interior.py - Generates complete, detailed interior decks for USS Enterprise NCC-1701-D
Features:
- Deck 1 (Y=144..147): Main Bridge (Hollowed & Detailed), Captain Picard's Ready Room & Observation Conference Lounge
- Deck 2/3 (Y=138..142): Saucer Concourse:
  * Transporter Room 3 (6-pad glowing platform & Chief O'Brien console)
  * Sickbay (Dr. Crusher's biobeds, medical monitors & office)
  * Holodeck 3 (Iconic yellow grid on black walls & control arch)
  * Ten Forward (Expanded panoramic lounge, Guinan's bar & tables)
  * Crystalline Skyway connection to the Picard Memorial
- Deck 10 (Y=128..133): Main Engineering (Pulsing Warp Core, Geordi's Pool table, MSD) & Shuttlebay with Type-6 Shuttle
- Turbolift Shafts linking all decks with vertical water/ladder transport
"""

import sys
import json
import math
import urllib.request

API_URL = "http://192.168.1.20:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/build_enterprise_d_interior.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def generate_interior(cx=-65, cy=138, cz=100):
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # =========================================================================
    # 1. DECK 1: MAIN BRIDGE (Fully Hollowed & Screen-Accurate)
    # =========================================================================
    # Bridge Dome Floor: Y=144, Ceiling: Y=148
    # Clear interior air volume: X: cx-5..cx+5, Y: 145..147, Z: cz-3..cz+4
    cmds.append(f"fill {cx-5} 145 {cz-3} {cx+5} 147 {cz+4} air replace")
    
    # Bridge Floor (Y=144): Smooth quartz with red carpet runner
    cmds.append(f"fill {cx-5} 144 {cz-3} {cx+5} 144 {cz+4} smooth_quartz replace")
    cmds.append(f"fill {cx-3} 144 {cz-2} {cx+3} 144 {cz+3} red_carpet replace")
    
    # Outer Perimeter Bulkheads (polished deepslate & smooth quartz)
    cmds.append(f"fill {cx-6} 145 {cz-3} {cx-6} 147 {cz+4} polished_deepslate replace")
    cmds.append(f"fill {cx+6} 145 {cz-3} {cx+6} 147 {cz+4} polished_deepslate replace")
    cmds.append(f"fill {cx-5} 145 {cz-4} {cx+5} 147 {cz-4} polished_deepslate replace")
    
    # Skylight Dome (Y=148): Tinted glass looking into the starry heavens
    cmds.append(f"fill {cx-3} 148 {cz-2} {cx+3} 148 {cz+3} tinted_glass replace")
    cmds.append(f"setblock {cx} 148 {cz} sea_lantern replace") # Dome illumination
    
    # Main Viewscreen (Forward +Z at Z=cz+5): Black concrete frame with cyan viewscreen
    cmds.append(f"fill {cx-3} 145 {cz+5} {cx+3} 147 {cz+5} black_concrete replace")
    cmds.append(f"fill {cx-2} 145 {cz+5} {cx+2} 146 {cz+5} light_blue_stained_glass replace")
    cmds.append(f"setblock {cx} 145 {cz+5} sea_lantern replace")
    
    # Conn & Ops Consoles (Front stations at Z=cz+3)
    cmds.append(f"setblock {cx-2} 145 {cz+2} polished_blackstone_stairs[facing=south] replace") # Ops (Data)
    cmds.append(f"setblock {cx+2} 145 {cz+2} polished_blackstone_stairs[facing=south] replace") # Conn
    cmds.append(f"setblock {cx-2} 145 {cz+3} daylight_detector replace")
    cmds.append(f"setblock {cx+2} 145 {cz+3} daylight_detector replace")
    
    # Captain's Chair & Command Trio (Center Z=cz)
    cmds.append(f"setblock {cx} 144 {cz} polished_blackstone replace") # Elevated plinth
    cmds.append(f"setblock {cx} 145 {cz} dark_oak_stairs[facing=south] replace") # Captain Picard
    cmds.append(f"setblock {cx-1} 145 {cz} oak_stairs[facing=south] replace")    # Counselor Troi
    cmds.append(f"setblock {cx+1} 145 {cz} oak_stairs[facing=south] replace")    # Commander Riker
    
    # Tactical Horseshoe Console (Aft behind Picard at Z=cz-2)
    cmds.append(f"fill {cx-2} 145 {cz-2} {cx+2} 145 {cz-2} stripped_oak_wood replace")
    cmds.append(f"setblock {cx-3} 145 {cz-1} stripped_oak_wood replace")
    cmds.append(f"setblock {cx+3} 145 {cz-1} stripped_oak_wood replace")
    cmds.append(f"setblock {cx} 146 {cz-2} polished_blackstone_pressure_plate replace")
    cmds.append(f"setblock {cx-1} 146 {cz-2} heavy_weighted_pressure_plate replace")
    cmds.append(f"setblock {cx+1} 146 {cz-2} light_weighted_pressure_plate replace")
    
    # Bridge Dedication Plaque on Aft Wall
    bridge_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"USS ENTERPRISE","color":"gold","bold":true}]\','
        '\'["",{"text":"NCC-1701-D","color":"yellow","bold":true}]\','
        '\'["",{"text":"Galaxy Class","color":"white"}]\','
        '\'["",{"text":"Capt. J.L. Picard","color":"aqua","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx} 146 {cz-3} oak_wall_sign[facing=south]{bridge_sign} replace")

    # -------------------------------------------------------------------------
    # CAPTAIN'S READY ROOM (Port side of Bridge, X: cx-10..cx-7, Z: cz-1..cz+3)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-11} 144 {cz-1} {cx-7} 144 {cz+3} polished_blackstone replace") # Floor
    cmds.append(f"fill {cx-11} 145 {cz-1} {cx-7} 147 {cz+3} air replace")                 # Air
    cmds.append(f"fill {cx-11} 148 {cz-1} {cx-7} 148 {cz+3} smooth_quartz replace")       # Ceiling
    # Outer walls & window
    cmds.append(f"fill {cx-11} 145 {cz-1} {cx-11} 147 {cz+3} polished_deepslate replace")
    cmds.append(f"fill {cx-11} 146 {cz} {cx-11} 146 {cz+2} tinted_glass replace")         # Panoramic star window
    # Doorway to Bridge
    cmds.append(f"setblock {cx-6} 145 {cz+1} iron_door[half=lower,facing=east] replace")
    cmds.append(f"setblock {cx-6} 146 {cz+1} iron_door[half=upper,facing=east] replace")
    # Picard's Desk & Fish Tank (Pim-Pim the lionfish)
    cmds.append(f"setblock {cx-9} 145 {cz+1} dark_oak_stairs[facing=west] replace") # Picard's office chair
    cmds.append(f"fill {cx-10} 145 {cz} {cx-10} 145 {cz+2} dark_oak_slab[type=top] replace") # Desk
    cmds.append(f"setblock {cx-10} 146 {cz+2} glass replace")
    cmds.append(f"setblock {cx-10} 146 {cz} chiseled_bookshelf[facing=east] replace") # Shakespeare collection
    ready_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"CAPTAIN","color":"gold","bold":true}]\','
        '\'["",{"text":"READY ROOM","color":"yellow","bold":true}]\','
        '\'["",{"text":"Jean-Luc Picard","color":"white"}]\','
        '\'["",{"text":"[PRIVATE]","color":"red"}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx-7} 146 {cz+1} oak_wall_sign[facing=east]{ready_sign} replace")

    # -------------------------------------------------------------------------
    # OBSERVATION LOUNGE / CONFERENCE ROOM (Aft of Bridge, Z: cz-8..cz-5)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-5} 144 {cz-8} {cx+5} 144 {cz-5} polished_blackstone replace") # Floor
    cmds.append(f"fill {cx-5} 145 {cz-8} {cx+5} 147 {cz-5} air replace")                 # Air
    cmds.append(f"fill {cx-5} 148 {cz-8} {cx+5} 148 {cz-5} smooth_quartz replace")       # Ceiling
    # Aft Observation Windows overlooking the saucer
    cmds.append(f"fill {cx-4} 146 {cz-8} {cx+4} 146 {cz-8} light_blue_stained_glass replace")
    # Conference Table (Large dark oak table with 8 chairs)
    cmds.append(f"fill {cx-2} 145 {cz-7} {cx+2} 145 {cz-6} dark_oak_slab[type=top] replace")
    cmds.append(f"setblock {cx-3} 145 {cz-7} dark_oak_stairs[facing=east] replace")
    cmds.append(f"setblock {cx-3} 145 {cz-6} dark_oak_stairs[facing=east] replace")
    cmds.append(f"setblock {cx+3} 145 {cz-7} dark_oak_stairs[facing=west] replace")
    cmds.append(f"setblock {cx+3} 145 {cz-6} dark_oak_stairs[facing=west] replace")
    # Doorway to Bridge
    cmds.append(f"setblock {cx} 145 {cz-4} iron_door[half=lower,facing=north] replace")
    cmds.append(f"setblock {cx} 146 {cz-4} iron_door[half=upper,facing=north] replace")
    # Soft table lighting
    cmds.append(f"setblock {cx} 147 {cz-6} sea_lantern replace")

    # =========================================================================
    # 2. DECK 2 & 3: SAUCER PRIMARY CONCOURSE (Y=138..142)
    # =========================================================================
    # Clear central saucer concourse: X: cx-18..cx+18, Y: 139..141, Z: cz-10..cz+12
    for dz in range(-10, 13):
        rz = dz / 12.0
        w = int(math.sqrt(max(0, 1.0 - rz*rz)) * 18)
        if w > 0:
            cmds.append(f"fill {cx-w} 139 {cz+dz} {cx+w} 141 {cz+dz} air replace")
            cmds.append(f"fill {cx-w} 138 {cz+dz} {cx+w} 138 {cz+dz} smooth_quartz replace")
            cmds.append(f"fill {cx-w} 142 {cz+dz} {cx+w} 142 {cz+dz} smooth_quartz replace")

    # Main Central Concourse Lighting Runners
    cmds.append(f"fill {cx} 138 {cz-8} {cx} 138 {cz+10} sea_lantern replace")
    cmds.append(f"fill {cx} 139 {cz-8} {cx} 139 {cz+10} light_gray_carpet replace")

    # -------------------------------------------------------------------------
    # TRANSPORTER ROOM 3 (Port Forward: X: cx-16..cx-10, Z: cz-3..cz+3)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-16} 138 {cz-3} {cx-10} 138 {cz+3} polished_blackstone replace")
    cmds.append(f"fill {cx-16} 139 {cz-3} {cx-10} 141 {cz+3} air replace")
    # Transporter Walls
    cmds.append(f"fill {cx-17} 139 {cz-3} {cx-17} 141 {cz+3} polished_deepslate replace")
    cmds.append(f"fill {cx-16} 139 {cz-4} {cx-10} 141 {cz-4} polished_deepslate replace")
    cmds.append(f"fill {cx-16} 139 {cz+4} {cx-10} 141 {cz+4} polished_deepslate replace")
    # 6-Pad Transporter Platform (Raised 1 block)
    cmds.append(f"fill {cx-15} 139 {cz-2} {cx-12} 139 {cz+2} smooth_stone_slab replace")
    pads = [(-15, -1), (-15, 1), (-13, -2), (-13, 0), (-13, 2), (-14, 0)]
    for px_rel, pz_rel in [(-15, -1), (-15, 1), (-13, -1), (-13, 1), (-14, -2), (-14, 2)]:
        cmds.append(f"setblock {cx+px_rel} 139 {cz+pz_rel} sea_lantern replace")
        cmds.append(f"setblock {cx+px_rel} 140 {cz+pz_rel} light_blue_stained_glass replace")
    # Transporter Overhead Emitter
    cmds.append(f"fill {cx-15} 142 {cz-2} {cx-12} 142 {cz+2} sea_lantern replace")
    # Chief O'Brien's Console
    cmds.append(f"setblock {cx-10} 139 {cz} daylight_detector replace")
    cmds.append(f"setblock {cx-10} 140 {cz} lever[facing=west] replace")
    trans_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"TRANSPORTER","color":"gold","bold":true}]\','
        '\'["",{"text":"ROOM 3","color":"yellow","bold":true}]\','
        '\'["",{"text":"Chief O\\\'Brien","color":"aqua"}]\','
        '\'["",{"text":"[ENERGIZE]","color":"green","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx-10} 140 {cz-1} oak_wall_sign[facing=east]{trans_sign} replace")

    # -------------------------------------------------------------------------
    # SICKBAY (Starboard: X: cx+10..cx+16, Z: cz-3..cz+3)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx+10} 138 {cz-3} {cx+16} 138 {cz+3} smooth_quartz replace")
    cmds.append(f"fill {cx+10} 139 {cz-3} {cx+16} 141 {cz+3} air replace")
    # Sickbay Walls (Clean sterile white quartz)
    cmds.append(f"fill {cx+17} 139 {cz-3} {cx+17} 141 {cz+3} smooth_quartz replace")
    cmds.append(f"fill {cx+10} 139 {cz-4} {cx+16} 141 {cz-4} smooth_quartz replace")
    cmds.append(f"fill {cx+10} 139 {cz+4} {cx+16} 141 {cz+4} smooth_quartz replace")
    # 3 Biobeds with overhead diagnostic monitors
    for bz in [-2, 0, 2]:
        cmds.append(f"setblock {cx+14} 139 {cz+bz} white_bed[facing=west] replace")
        cmds.append(f"setblock {cx+15} 139 {cz+bz} smooth_quartz replace")
        cmds.append(f"setblock {cx+15} 140 {cz+bz} sea_lantern replace")
        cmds.append(f"setblock {cx+15} 141 {cz+bz} end_rod[facing=down] replace")
    # Dr. Crusher's Consultation Desk & Medicine Replicator
    cmds.append(f"setblock {cx+11} 139 {cz-2} brewing_stand replace")
    cmds.append(f"setblock {cx+11} 139 {cz+2} chiseled_bookshelf[facing=west] replace")
    sick_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"MAIN SICKBAY","color":"aqua","bold":true}]\','
        '\'["",{"text":"DECK 2","color":"blue","bold":true}]\','
        '\'["",{"text":"Dr. B. Crusher","color":"white"}]\','
        '\'["",{"text":"[MEDICAL CARE]","color":"green"}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx+10} 140 {cz-1} oak_wall_sign[facing=west]{sick_sign} replace")

    # -------------------------------------------------------------------------
    # HOLODECK 3 (Port Aft: X: cx-15..cx-9, Z: cz+4..cz+9)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-15} 138 {cz+4} {cx-9} 138 {cz+9} black_concrete replace") # Grid Floor
    cmds.append(f"fill {cx-15} 139 {cz+4} {cx-9} 141 {cz+9} air replace")
    cmds.append(f"fill {cx-15} 142 {cz+4} {cx-9} 142 {cz+9} black_concrete replace") # Grid Ceiling
    # Yellow Grid Lines on Black Walls
    for hx in range(cx-15, cx-8):
        for hz in range(cz+4, cz+10):
            # Floor yellow grid
            if (hx - cx) % 2 == 0 or (hz - cz) % 2 == 0:
                cmds.append(f"setblock {hx} 138 {hz} yellow_concrete replace")
    # Holodeck Exit Archway
    cmds.append(f"setblock {cx-9} 139 {cz+6} yellow_concrete replace")
    cmds.append(f"setblock {cx-9} 140 {cz+6} yellow_concrete replace")
    cmds.append(f"setblock {cx-9} 141 {cz+6} yellow_concrete replace")
    cmds.append(f"setblock {cx-9} 140 {cz+5} daylight_detector replace") # Holodeck arch panel
    holo_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"HOLODECK 3","color":"yellow","bold":true}]\','
        '\'["",{"text":"SIMULATION GRID","color":"gold"}]\','
        '\'["",{"text":"Program: Dixon Hill","color":"gray","italic":true}]\','
        '\'["",{"text":"[SAFETY: ON]","color":"green","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx-9} 141 {cz+5} oak_wall_sign[facing=east]{holo_sign} replace")

    # -------------------------------------------------------------------------
    # TEN FORWARD LOUNGE (Front Tip: Z: cz+11..cz+16)
    # -------------------------------------------------------------------------
    cmds.append(f"fill {cx-7} 138 {cz+11} {cx+7} 138 {cz+16} dark_oak_planks replace") # Cozy wood floor
    cmds.append(f"fill {cx-7} 139 {cz+11} {cx+7} 141 {cz+16} air replace")
    # Panoramic forward windows
    cmds.append(f"fill {cx-6} 139 {cz+16} {cx+6} 140 {cz+16} light_blue_stained_glass replace")
    cmds.append(f"fill {cx-5} 141 {cz+16} {cx+5} 141 {cz+16} tinted_glass replace")
    # Lounge Tables and Chairs along the window
    for tx in [-5, 0, 5]:
        cmds.append(f"setblock {cx+tx} 139 {cz+15} dark_oak_fence replace")
        cmds.append(f"setblock {cx+tx} 140 {cz+15} gray_carpet replace")
        cmds.append(f"setblock {cx+tx-1} 139 {cz+15} dark_oak_stairs[facing=east] replace")
        cmds.append(f"setblock {cx+tx+1} 139 {cz+15} dark_oak_stairs[facing=west] replace")
    # Guinan's Curved Bar
    cmds.append(f"fill {cx-3} 139 {cz+12} {cx+3} 139 {cz+12} dark_oak_stairs[facing=south] replace")
    cmds.append(f"setblock {cx-2} 139 {cz+11} brewing_stand replace")
    cmds.append(f"setblock {cx+2} 139 {cz+11} brewing_stand replace")
    cmds.append(f"setblock {cx} 140 {cz+12} lantern[hanging=false] replace")
    ten_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"TEN FORWARD","color":"gold","bold":true}]\','
        '\'["",{"text":"LOUNGE & RECREATION","color":"yellow"}]\','
        '\'["",{"text":"Host: Guinan","color":"light_purple"}]\','
        '\'["",{"text":"[SYNTHEHOL ON TAP]","color":"aqua"}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx} 141 {cz+13} oak_wall_sign[facing=south]{ten_sign} replace")

    # -------------------------------------------------------------------------
    # CRYSTALLINE SKYWAY CONNECTION TO PICARD MEMORIAL
    # -------------------------------------------------------------------------
    # Doorway at Port concourse X: cx-18, Z: cz+10
    cmds.append(f"fill {cx-19} 138 {cz+9} {cx-19} 141 {cz+11} smooth_quartz replace")
    cmds.append(f"fill {cx-19} 139 {cz+10} {cx-19} 140 {cz+10} air replace") # Door opening
    # Signs pointing to Memorial
    sky_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"CRYSTAL SKYWAY","color":"aqua","bold":true}]\','
        '\'["",{"text":"PASSAGE TO","color":"white"}]\','
        '\'["",{"text":"PICARD MEMORIAL","color":"gold","bold":true}]\','
        '\'["",{"text":"[ROTUNDA]","color":"yellow"}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx-18} 140 {cz+11} oak_wall_sign[facing=east]{sky_sign} replace")

    # =========================================================================
    # 3. TURBOLIFT SYSTEM (High-Speed Access between Bridge, Saucer & Engineering)
    # =========================================================================
    # Turbolift 1: Bridge (Y=144) <-> Saucer Concourse (Y=138) at [cx+4, cz-1]
    cmds.append(f"fill {cx+4} 138 {cz-1} {cx+4} 144 {cz-1} iron_block replace")
    cmds.append(f"fill {cx+3} 139 {cz-1} {cx+3} 144 {cz-1} ladder[facing=west] replace")
    
    # Turbolift 2: Saucer (Y=138) <-> Engineering Section (Y=128) at [cx, cz-10]
    cmds.append(f"fill {cx} 128 {cz-10} {cx} 138 {cz-10} iron_block replace")
    cmds.append(f"fill {cx} 129 {cz-11} {cx} 138 {cz-11} ladder[facing=north] replace")

    # =========================================================================
    # 4. DECK 10: MAIN ENGINEERING & SHUTTLEBAY (Y=128..133)
    # =========================================================================
    # Engineering Hallway & Pool Deck: X: cx-5..cx+5, Z: cz-37..cz-15
    cmds.append(f"fill {cx-5} 128 {cz-37} {cx+5} 128 {cz-15} gray_concrete replace") # Engineering Floor
    cmds.append(f"fill {cx-5} 129 {cz-37} {cx+5} 132 {cz-15} air replace")           # Air
    cmds.append(f"fill {cx-5} 133 {cz-37} {cx+5} 133 {cz-15} smooth_quartz replace")   # Ceiling
    # Engineering Bulkheads
    cmds.append(f"fill {cx-6} 129 {cz-37} {cx-6} 132 {cz-15} polished_deepslate replace")
    cmds.append(f"fill {cx+6} 129 {cz-37} {cx+6} 132 {cz-15} polished_deepslate replace")

    # THE PULSING WARP CORE (Galaxy-Class vertical intermix chamber at Z=cz-24)
    cmds.append(f"fill {cx} 128 {cz-24} {cx} 133 {cz-24} sea_lantern replace")
    for dx, dz in [(-1,0), (1,0), (0,-1), (0,1)]:
        cmds.append(f"fill {cx+dx} 129 {cz-24+dz} {cx+dx} 132 {cz-24+dz} cyan_stained_glass replace")
    # Dilithium Chamber Articulator
    cmds.append(f"setblock {cx} 129 {cz-24} beacon replace")
    cmds.append(f"setblock {cx} 130 {cz-24} sea_lantern replace")

    # Geordi La Forge's Master Systems Display (MSD) Pool Table Console
    cmds.append(f"fill {cx-2} 129 {cz-20} {cx+2} 129 {cz-18} polished_blackstone_stairs[facing=south] replace")
    cmds.append(f"setblock {cx-1} 129 {cz-19} daylight_detector replace")
    cmds.append(f"setblock {cx+1} 129 {cz-19} daylight_detector replace")
    cmds.append(f"setblock {cx} 129 {cz-19} redstone_lamp[lit=true] replace")
    eng_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"MAIN ENGINEERING","color":"gold","bold":true}]\','
        '\'["",{"text":"WARP CORE MATRIX","color":"yellow"}]\','
        '\'["",{"text":"Chief Geordi La Forge","color":"white"}]\','
        '\'["",{"text":"[WARP: 9.6 READY]","color":"green","bold":true}]\''
        ']}}'
    )
    cmds.append(f"setblock {cx} 130 {cz-18} oak_wall_sign[facing=south]{eng_sign} replace")

    # -------------------------------------------------------------------------
    # MAIN SHUTTLEBAY WITH PARKED TYPE-6 SHUTTLECRAFT (Aft: Z: cz-37..cz-31)
    # -------------------------------------------------------------------------
    # Shuttlebay runway floor markings (Yellow concrete hazard lines)
    cmds.append(f"fill {cx-4} 128 {cz-37} {cx-4} 128 {cz-31} yellow_concrete replace")
    cmds.append(f"fill {cx+4} 128 {cz-37} {cx+4} 128 {cz-31} yellow_concrete replace")
    
    # Parked Type-6 Shuttlecraft ("Shuttlecraft Goddard")
    # Centered at [cx, 129, cz-34]
    sx, sy, sz = cx, 129, cz - 34
    cmds.append(f"fill {sx-2} {sy} {sz-2} {sx+2} {sy+1} {sz+2} smooth_quartz replace") # Shuttle Cabin
    cmds.append(f"fill {sx-1} {sy+1} {sz-2} {sx+1} {sy+1} {sz-2} light_blue_stained_glass replace") # Cockpit window
    # Twin Warp Nacelles on the Shuttle
    cmds.append(f"fill {sx-3} {sy} {sz-3} {sx-3} {sy} {sz+2} smooth_stone replace")
    cmds.append(f"fill {sx+3} {sy} {sz-3} {sx+3} {sy} {sz+2} smooth_stone replace")
    cmds.append(f"setblock {sx-3} {sy} {sz-1} cyan_stained_glass replace")
    cmds.append(f"setblock {sx+3} {sy} {sz-1} cyan_stained_glass replace")
    # Shuttle Interior (Seats)
    cmds.append(f"setblock {sx} {sy} {sz-1} oak_stairs[facing=north] replace") # Pilot seat
    cmds.append(f"setblock {sx} {sy} {sz} oak_stairs[facing=north] replace")   # Passenger seat
    shuttle_sign = (
        '{front_text:{messages:['
        '\'["",{"text":"SHUTTLECRAFT","color":"gold","bold":true}]\','
        '\'["",{"text":"GODDARD","color":"yellow","bold":true}]\','
        '\'["",{"text":"Type-6 Shuttle","color":"white"}]\','
        '\'["",{"text":"NCC-1701-D/07","color":"aqua"}]\''
        ']}}'
    )
    cmds.append(f"setblock {sx} {sy+1} {sz+2} oak_wall_sign[facing=south]{shuttle_sign} replace")

    return cmds

def main():
    cx, cy, cz = -65, 138, 100
    print(f"[*] Compiling Enterprise-D Interior commands at [{cx}, {cy}, {cz}]...")
    cmds = generate_interior(cx, cy, cz)

    with open(FUNCTION_PATH, "w") as f:
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Wrote {len(cmds)} commands to {FUNCTION_PATH}")

    # Trigger via API
    print("[*] Reloading datapacks...")
    rel = run_cmd("reload")
    print("Reload:", rel)

    print("[*] Executing /function blueprints:build_enterprise_d_interior...")
    fn = run_cmd("function blueprints:build_enterprise_d_interior")
    print("Function:", fn)

if __name__ == "__main__":
    main()
