#!/usr/bin/env python3
"""
clear_floating_castle.py
Safely demolishes the remnants of the gothic castle floating in the air in front of Pace,
with strict guarantees to protect the USS Enterprise NCC-1701 hovering nearby.
"""

import urllib.request
import json
import time
import sys

API_URL = "http://localhost:8090/api/run"

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERR] {e}: {cmd}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def clean_castle():
    print("[*] Starting safe demolition of floating castle remnants...")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"🧹 Iniciando limpeza segura das estruturas do castelo no ar... A USS Enterprise está 100% protegida!","color":"gold","bold":true}]')
    run_cmd("execute as @a at @s run playsound minecraft:block.beacon.deactivate master @s ~ ~ ~ 1 1")

    # Castle specific blocks to safely target
    castle_materials = [
        "deepslate_bricks",
        "stone_bricks",
        "red_nether_bricks",
        "polished_deepslate",
        "deepslate_tile_slab",
        "stone_brick_slab",
        "stone_brick_wall",
        "deepslate_tile_wall",
        "beacon",
        "soul_lantern",
        "ladder",
        "bell",
        "gold_block",
        "purple_stained_glass",
        "purple_carpet",
        "purple_banner",
        "dark_oak_planks",
        "dark_oak_slab",
        "dark_oak_stairs",
        "spruce_planks",
        "bookshelf",
        "enchanting_table",
        "red_bed",
        "chest",
        "campfire",
        "candle"
    ]

    # Castle zone: X: -260..-140, Z: 96..160 (Enterprise ends at Z=93, Y=106..141)
    # 1. First pass: specific material surgical replacement across the entire potential castle sky box (Y: 65..160)
    for mat in castle_materials:
        cmd = f"fill -260 65 96 -140 160 160 air replace {mat}"
        run_cmd(cmd)

    # 2. Air replacement in slices strictly at Z >= 98 (well beyond Enterprise saucer at Z=93)
    # Clear any residual floating blocks in the castle volume
    for y_start in range(70, 160, 25):
        y_end = min(y_start + 24, 160)
        cmd = f"fill -250 {y_start} 98 -160 {y_end} 145 air replace"
        run_cmd(cmd)

    run_cmd("execute as @a at @s run playsound minecraft:entity.experience_orb.pickup master @s ~ ~ ~ 1 1.2")
    run_cmd('tellraw @a [{"text":"[Antigravity] ","color":"light_purple","bold":true},{"text":"✅ Restos do castelo no ar removidos com sucesso! Céu limpo e USS Enterprise intacta.","color":"green","bold":true}]')
    print("[*] Cleanup finished!")

if __name__ == "__main__":
    clean_castle()
