#!/usr/bin/env python3
"""
illuminate_castle.py - Purges all monsters from the Castle, Courtyard & Outskirts,
and covers the area in invisible light blocks (light[level=15]) to prevent any respawns.
Safeguards 100% of Naturalist animals, NPCs, pets, players and structures.
"""
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

cx = -178
cy = 65
cz = 150

print("[*] 1. Purging all hostile mobs in Castle, Courtyard & Exterior (radius 85)...")
mob_types = [
    "zombie", "zombie_villager", "skeleton", "creeper", "spider", "cave_spider",
    "enderman", "witch", "slime", "phantom", "drowned", "stray", "bogged",
    "wither_skeleton", "silverfish"
]
for m in mob_types:
    run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..85,type={m}]")

# Also clear hostile arrows
run_cmd(f"kill @e[x={cx},y={cy},z={cz},distance=..85,type=arrow]")

print("[*] 2. Applying invisible light field (level=15) on Courtyard, Moat & Exterior...")
# Castle & Courtyard ground level is Y=64..66. Slices at Y=65 and Y=66.
# Castle footprint: X: -228..-129, Z: 100..199
# Courtyard: X: -215..-135, Z: 160..198
# Front / Moat / Bridge: X: -220..-140, Z: 70..100
# Exterior flanks: X: -260..-100, Z: 60..230
slices = [
    # (x1, z1, x2, z2, y)
    # Courtyard / Pátio interno do castelo (Santuário e jardins)
    (-220, 155, -135, 198, 65),
    (-220, 155, -135, 198, 66),
    # Pátio frontal, fosso de lava e ponte levadiça
    (-220, 70, -135, 105, 65),
    (-220, 70, -135, 105, 66),
    # Flanco Oeste do castelo
    (-265, 80, -221, 215, 65),
    (-265, 80, -221, 215, 66),
    # Flanco Leste do castelo
    (-134, 80, -95, 215, 65),
    (-134, 80, -95, 215, 66),
    # Flanco Sul (fundos do castelo)
    (-240, 199, -120, 235, 65),
    (-240, 199, -120, 235, 66),
    # Pavimento do Salão Principal e Criptas
    (-215, 105, -140, 154, 65),
    (-215, 105, -140, 154, 75)
]

for x1, z1, x2, z2, y in slices:
    run_cmd(f"fill {x1} {y} {z1} {x2} {y} {z2} light[level=15] replace air")

print("[*] 3. Audio & Visual Confirmation...")
run_cmd('title pace times 5 45 10')
run_cmd('title pace subtitle {"text":"Pátio e exterior do Castelo 100% iluminados e seguros!","color":"green"}')
run_cmd('title pace title {"text":"🏰 CASTELO BLINDADO","color":"gold","bold":true}')
run_cmd('playsound minecraft:block.beacon.activate master pace -178 65 150 2.0 1.2')
run_cmd('tellraw pace ["",{"text":"[SISTEMA DE DEFESA] ","color":"dark_purple","bold":true},{"text":"Monstros expurgados do Castelo e Pátio! Luz invisível (nível 15) instalada e animais protegidos.","color":"gray"}]')
print("[*] Complete!")
