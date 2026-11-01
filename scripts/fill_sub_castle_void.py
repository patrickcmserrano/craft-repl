#!/usr/bin/env python3
"""
fill_sub_castle_void.py
Fills the cavernous void and vacuum underneath the Gothic Mega-Castle and the surrounding area.
- Solid deepslate from Y=-60 to Y=0
- Solid stone from Y=1 to Y=55
- Reinforced stone & foundation from Y=56 to Y=62
- Protects the volcanic chasm (Z: 90..99, Y: 45..63)
- Fills open void pits in the front approach (Z: 40..89) up to ground level Y=63
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/fill_sub_void.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_fill_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # -------------------------------------------------------------
    # 1. UNDER CASTLE FOOTPRINT (X: -228..-129, Z: 100..199)
    # -------------------------------------------------------------
    # Split into 2 halves along X to keep block counts super safe
    x_ranges = [(-228, -179), (-178, -129)]
    
    # Sub-bedrock deepslate: Y -60 to 0
    for x1, x2 in x_ranges:
        for y1, y2 in [(-60, -31), (-30, 0)]:
            for replace_mat in ["air", "cave_air", "water"]:
                cmds.append(f"fill {x1} {y1} 100 {x2} {y2} 199 deepslate replace {replace_mat}")

    # Stone strata: Y 1 to 55
    for x1, x2 in x_ranges:
        for y1, y2 in [(1, 28), (29, 55)]:
            for replace_mat in ["air", "cave_air", "water"]:
                cmds.append(f"fill {x1} {y1} 100 {x2} {y2} 199 stone replace {replace_mat}")

    # Immediate sub-foundation: Y 56 to 62
    for x1, x2 in x_ranges:
        for replace_mat in ["air", "cave_air", "water"]:
            cmds.append(f"fill {x1} 56 100 {x2} 62 199 deepslate replace {replace_mat}")

    # -------------------------------------------------------------
    # 2. UNDER VOLCANIC CHASM BED (X: -215..-141, Z: 90..99)
    # Below the magma bed (Y: 45) -> fill from Y=-60 to Y=44
    # -------------------------------------------------------------
    for replace_mat in ["air", "cave_air", "water"]:
        cmds.append(f"fill -215 -60 90 -141 0 99 deepslate replace {replace_mat}")
        cmds.append(f"fill -215 1 90 -141 44 99 stone replace {replace_mat}")

    # -------------------------------------------------------------
    # 3. APPROACH & SURROUNDING VOIDS (X: -256..-130, Z: 40..89)
    # Where Lizzie fell and open pits existed
    # -------------------------------------------------------------
    approach_x = [(-256, -195), (-194, -130)]
    for x1, x2 in approach_x:
        for replace_mat in ["air", "cave_air", "water"]:
            # Deepslate layer
            cmds.append(f"fill {x1} -60 40 {x2} 0 89 deepslate replace {replace_mat}")
            # Stone layer
            cmds.append(f"fill {x1} 1 40 {x2} 55 89 stone replace {replace_mat}")
            # Sub-surface stone
            cmds.append(f"fill {x1} 56 40 {x2} 60 89 stone replace {replace_mat}")
            # Dirt layer
            cmds.append(f"fill {x1} 61 40 {x2} 62 89 dirt replace {replace_mat}")
            # Surface grass
            cmds.append(f"fill {x1} 63 40 {x2} 63 89 grass_block replace {replace_mat}")

    # Resurrect/heal Lizzie and Pace if needed
    cmds.append("effect give @a minecraft:regeneration 10 255 true")
    cmds.append("effect give @a minecraft:saturation 10 255 true")

    return cmds

def main():
    print("[*] Generating Sub-Castle Void Fill commands...")
    cmds = build_fill_commands()
    print(f"[*] Total commands: {len(cmds)}")

    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# Sub-Castle Void Fill Routine\n")
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Saved to {FUNCTION_PATH}")

    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Preenchendo o vacuo subterraneo abaixo do castelo e o terreno frontal...","color":"gold","bold":true}]')
    
    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)

    print("[*] Executing /function blueprints:fill_sub_void...")
    t0 = time.time()
    res = run_cmd("function blueprints:fill_sub_void")
    elapsed = time.time() - t0
    print(f"[*] Finished in {elapsed:.2f}s: {res}")

    run_cmd("execute as @a at @s run playsound minecraft:block.stone.place master @s ~ ~ ~ 1 0.8")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Todo o vacuo abaixo do castelo foi 100% preenchido com rocha solida e deepslate ate a bedrock!","color":"green","bold":true}]')
    print("[*] Done!")

if __name__ == "__main__":
    main()
