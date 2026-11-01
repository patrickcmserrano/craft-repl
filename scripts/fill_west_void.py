#!/usr/bin/env python3
"""
fill_west_void.py
Fills the vast western and north-western chasm/void surrounding the castle:
- Western flank (X: -320..-229, Z: 90..150)
- North-Western approach (X: -320..-130, Z: 10..89)
- Perfectly seals all empty air from Y=-60 (bedrock) up to Y=63 (surface grass)
- Completely protects the Volcanic Chasm (X: -216..-140, Z: 90..99)
- Completely protects the Gothic Citadel (X: -228..-129, Z: 100..199)
"""

import os
import sys
import json
import time
import urllib.request

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/fill_west_void.mcfunction"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def build_commands():
    cmds = []
    cmds.append("gamerule commandModificationBlockLimit 2000000")

    # We partition into safe zones to respect boundaries:

    # ZONE 1: North-West approach field (X: -320 to -130, Z: 10 to 89)
    # Slices along X: [-320..-226], [-225..-130]
    # Slices along Z: [10..49], [50..89]
    for x1, x2 in [(-320, -226), (-225, -130)]:
        for z1, z2 in [(10, 49), (50, 89)]:
            for replace_mat in ["air", "cave_air", "water"]:
                cmds.append(f"fill {x1} -60 {z1} {x2} 0 {z2} deepslate replace {replace_mat}")
                cmds.append(f"fill {x1} 1 {z1} {x2} 56 {z2} stone replace {replace_mat}")
                cmds.append(f"fill {x1} 57 {z1} {x2} 62 {z2} dirt replace {replace_mat}")
                cmds.append(f"fill {x1} 63 {z1} {x2} 63 {z2} grass_block replace {replace_mat}")

    # ZONE 2: West of Volcanic Chasm (X: -320 to -217, Z: 90 to 99)
    # Chasm is at X: -216..-140, Z: 90..99
    for replace_mat in ["air", "cave_air", "water"]:
        cmds.append(f"fill -320 -60 90 -217 0 99 deepslate replace {replace_mat}")
        cmds.append(f"fill -320 1 90 -217 56 99 stone replace {replace_mat}")
        cmds.append(f"fill -320 57 90 -217 62 99 dirt replace {replace_mat}")
        cmds.append(f"fill -320 63 90 -217 63 99 grass_block replace {replace_mat}")

    # ZONE 3: West flank of Castle (X: -320 to -229, Z: 100 to 155)
    # Castle western wall starts at X: -228
    # Slices along Z: [100..127], [128..155]
    for z1, z2 in [(100, 127), (128, 155)]:
        for replace_mat in ["air", "cave_air", "water"]:
            cmds.append(f"fill -320 -60 {z1} -229 0 {z2} deepslate replace {replace_mat}")
            cmds.append(f"fill -320 1 {z1} -229 56 {z2} stone replace {replace_mat}")
            cmds.append(f"fill -320 57 {z1} -229 62 {z2} dirt replace {replace_mat}")
            cmds.append(f"fill -320 63 {z1} -229 63 {z2} grass_block replace {replace_mat}")

    return cmds

def main():
    print("[*] Generating West & North-West Void Fill commands...")
    cmds = build_commands()
    print(f"[*] Total commands: {len(cmds)}")

    os.makedirs(os.path.dirname(FUNCTION_PATH), exist_ok=True)
    with open(FUNCTION_PATH, "w") as f:
        f.write("# West & North-West Void Fill Routine\n")
        for c in cmds:
            f.write(c + "\n")
    print(f"[*] Saved to {FUNCTION_PATH}")

    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Expandindo preenchimento solido para todo o quadrante oeste e noroeste...","color":"gold","bold":true}]')

    print("[*] Reloading datapacks...")
    run_cmd("reload")
    time.sleep(1.0)

    print("[*] Executing /function blueprints:fill_west_void...")
    t0 = time.time()
    res = run_cmd("function blueprints:fill_west_void")
    elapsed = time.time() - t0
    print(f"[*] Finished in {elapsed:.2f}s: {res}")

    run_cmd("execute as @a at @s run playsound minecraft:block.stone.place master @s ~ ~ ~ 1 1")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"Todo o quadrante oeste preenchido e nivelado ate a superficie com grama e rocha solida!","color":"green","bold":true}]')
    print("[*] Done!")

if __name__ == "__main__":
    main()
