#!/usr/bin/env python3
import urllib.request, json, time

API_URL = "http://localhost:8090/api/run"
FUNCTION_PATH = "/home/pace/minecraft-server/world/datapacks/blueprints/data/blueprints/function/fill_aquarium.mcfunction"

def run_cmd(cmd):
    data = json.dumps({"cmd": cmd}).encode("utf-8")
    req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))

lines = [
    # 1. Fill water
    "fill -169 65 126 -162 70 133 water replace air",
    
    # 2. Add lush underwater decorations and lights
    "setblock -167 65 128 tube_coral_block replace",
    "setblock -166 65 128 brain_coral_block replace",
    "setblock -164 65 131 bubble_coral_block replace",
    "setblock -165 65 131 fire_coral_block replace",
    "setblock -167 65 131 horn_coral_block replace",
    
    # Sea pickles
    "setblock -168 65 127 sea_pickle[pickles=4,waterlogged=true] replace",
    "setblock -163 65 132 sea_pickle[pickles=4,waterlogged=true] replace",
    "setblock -168 65 132 sea_pickle[pickles=3,waterlogged=true] replace",
    "setblock -163 65 127 sea_pickle[pickles=3,waterlogged=true] replace",
    
    # 3. Axolotls (all 5 variants with persistence)
    "summon axolotl -166 66 128 {Variant:0,PersistenceRequired:1b,CustomName:'\"🌸 Pérola (Lucy)\"'}",
    "summon axolotl -164 66 128 {Variant:1,PersistenceRequired:1b,CustomName:'\"🍂 Cacau (Wild)\"'}",
    "summon axolotl -167 66 130 {Variant:2,PersistenceRequired:1b,CustomName:'\"⭐ Sol (Gold)\"'}",
    "summon axolotl -163 66 130 {Variant:3,PersistenceRequired:1b,CustomName:'\"💎 Glaciar (Cyan)\"'}",
    "summon axolotl -165 67 129 {Variant:4,PersistenceRequired:1b,CustomName:'\"✨ Índigo Raro (Blue)\"'}",
    
    # Tropical fish companions
    "summon tropical_fish -166 67 129 {PersistenceRequired:1b}",
    "summon tropical_fish -164 67 131 {PersistenceRequired:1b}",
    "summon tropical_fish -165 68 128 {PersistenceRequired:1b}",
    
    # 4. Give player underwater superpowers (Conduit Power + Night Vision)
    "effect give @a[distance=..15] minecraft:conduit_power 600 0 true",
    "effect give @a[distance=..15] minecraft:night_vision 600 0 true",
    "playsound minecraft:block.water.ambient ambient @a[distance=..15] -165 66 129 1 1",
    "playsound minecraft:entity.player.splash ambient @a[distance=..15] -165 66 129 1 1",
]

with open(FUNCTION_PATH, "w") as f:
    f.write("\n".join(lines) + "\n")

print(f"Wrote {len(lines)} commands to {FUNCTION_PATH}")
run_cmd("reload")
time.sleep(0.3)
run_cmd("function blueprints:fill_aquarium")
print("Executed fill_aquarium function successfully!")
