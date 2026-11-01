#!/usr/bin/env python3
"""
illuminate_death_star.py - Inundates all decks of the Imperial Death Star with
invisible light blocks (light[level=15]), completely blocking natural mob spawning.
"""
import sys
import json
import math
import subprocess

JAVA_BIN = "/home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/java"
INJECTOR_CP = "/home/pace/dev/craft-repl/target/bin"
AGENT_JAR = "/home/pace/dev/craft-repl/target/live-cmd.jar"

def get_pid():
    out = subprocess.check_output(["pgrep", "-f", "fabric-server-launcher"]).decode().strip()
    return out.split()[0]

PID = get_pid()

def run_cmd(cmd):
    subprocess.run([JAVA_BIN, "-cp", INJECTOR_CP, "com.craft.agent.Injector", PID, AGENT_JAR, cmd],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

cx = 168
cy = 155
cz = -110

print("[*] 1. Purging all existing monsters and hostile drops...")
mob_types = [
    "zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman",
    "witch", "slime", "phantom", "drowned", "stray", "bogged",
    "wither_skeleton", "silverfish"
]
for m in mob_types:
    run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type={m}]")

# Clean monster junk drops
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=arrow]")
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=item,nbt={{Item:{{id:\"minecraft:rotten_flesh\"}}}}]")
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=item,nbt={{Item:{{id:\"minecraft:bone\"}}}}]")
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=item,nbt={{Item:{{id:\"minecraft:gunpowder\"}}}}]")
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=item,nbt={{Item:{{id:\"minecraft:string\"}}}}]")
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..45,type=item,nbt={{Item:{{id:\"minecraft:spider_eye\"}}}}]")

print("[*] 2. Applying invisible light field (level=15) across all decks...")
# Target Y levels corresponding to each deck walkable floor air layer
# Sphere radius = 33
y_levels = [
    131, 134, # Deck 1
    143, 146, # Deck 2
    156, 159, # Deck 3 (Equatorial Concourse / Hangar / Catwalk)
    170, 173, # Deck 4 (Superlaser Control)
    184, 187  # Deck 5 (Throne Room)
]

for y in y_levels:
    dy = y - cy
    # Horizontal circle radius at this Y
    max_r = int(math.sqrt(max(0, 33*33 - dy*dy))) - 1
    if max_r <= 0:
        continue
    # Divide into manageable fill chunks (Minecraft fill limit is ~32768 blocks)
    # A single horizontal slice 60x60 is only 3600 blocks!
    run_cmd(f"fill {cx-max_r} {y} {cz-max_r} {cx+max_r} {y} {cz+max_r} light[level=15] replace air")
    print(f"    - Deck Y={y} illuminated with light[level=15] (radius {max_r})")

print("[*] 3. Sending confirmation to players...")
run_cmd('title pace times 5 45 10')
run_cmd('title pace subtitle {"text":"Luz invisível instalada e monstros expurgados!","color":"aqua"}')
run_cmd('title pace title {"text":"🌑 DEATH STAR PROTEGIDA","color":"gold","bold":true}')
run_cmd('playsound minecraft:block.beacon.activate master pace 168 155 -110 2.0 1.2')
run_cmd('tellraw pace ["",{"text":"[DEATH STAR] ","color":"red","bold":true},{"text":"Iluminação invisível (nível 15) aplicada em todos os decks. Spawns naturais de monstros bloqueados permanentemente!","color":"gray"}]')
print("[*] Done!")
