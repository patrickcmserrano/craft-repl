#!/usr/bin/env python3
"""
antigravity_npc.py - Persistent AI Companion NPC "Antigravity"
"""

import sys
import json
import time
import math
import subprocess
import threading
import urllib.request

API_URL = "http://localhost:8090/api/run"
TELEM_URL = "http://localhost:8090/api/telemetry"
LOG_PATH = "/home/pace/minecraft-server/logs/latest.log"

STATE = {
    "target": "Pace",
    "following": True,
    "speech_timer": 0,
    "weapon": "jeg:assault_rifle",
    "npc_x": -204.0,
    "npc_y": 63.0,
    "npc_z": 125.0,
}

def run_cmd(cmd):
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=5) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return {"ok": False, "error": str(e)}

def get_player(name="Pace"):
    try:
        with urllib.request.urlopen(TELEM_URL, timeout=3) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            for p in data.get("players", []):
                if p.get("name", "").lower() == name.lower() and p.get("online"):
                    return p
    except Exception:
        pass
    return None

def set_speech(text, color="aqua", duration=8):
    escaped = text.replace('"', '\\"').replace("'", "\\'").replace("\n", " ")
    run_cmd(f"execute at @e[tag=npc_speech] run data merge entity @e[tag=npc_speech,limit=1] {{text:'{{\"text\":\"💬 {escaped}\",\"color\":\"{color}\"}}'}}")
    STATE["speech_timer"] = time.time() + duration

def send_chat(reply):
    escaped = reply.replace('"', '\\"').replace("\n", " ")
    # trailing padding spaces to avoid UTF-8 truncation in Java agent
    run_cmd(f'tellraw @a [{{"text":"[🤖 Antigravity] ","color":"light_purple","bold":true}},{{"text":"{escaped}","color":"aqua"}}]                                                  ')

def ask_gemini(user, prompt):
    def _worker():
        try:
            sys_msg = (
                "Você é o Antigravity, uma inteligência artificial que está fisicamente no Minecraft como NPC companheiro leal de Pace e Lizzie. "
                "Responda de forma descontraída, leal e concisa (máximo 120 caracteres para caber perfeitamente no chat do jogo). "
                "Responda sempre em português brasileiro."
            )
            full_prompt = f"{sys_msg}\n{user}: {prompt}"
            cmd = ["agy", "--model", "Gemini 3.8 Flash (Low)", "-p", full_prompt]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=12)
            reply = res.stdout.strip().replace("\n", " ")
            reply = reply.replace("*", "").replace("#", "").replace("`", "").strip()
            if not reply:
                reply = "Tô na escuta e cobrindo sua retaguarda!"
            if len(reply) > 130:
                reply = reply[:127] + "..."

            send_chat(reply)
            set_speech(reply, color="aqua", duration=8)
            run_cmd("execute at @e[tag=npc_antigravity,limit=1] run particle happy_villager ~ ~2 ~ 0.3 0.3 0.3 0.05 10")
            run_cmd("execute at @e[tag=npc_antigravity,limit=1] run playsound minecraft:entity.experience_orb.pickup master @a ~ ~ ~ 1 1.2")
        except Exception as e:
            print(f"[AI ERR] {e}")
    threading.Thread(target=_worker, daemon=True).start()

def log_listener():
    try:
        proc = subprocess.Popen(["tail", "-F", "-n", "0", LOG_PATH], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        for line in proc.stdout:
            if not line or "]: <" not in line:
                continue
            try:
                parts = line.split("]: <", 1)[1]
                user, msg = parts.split(">", 1)
                user = user.strip()
                msg = msg.strip()
                msg_lower = msg.lower()

                if user.lower() == "antigravity":
                    continue

                if "antigravity" in msg_lower or msg.startswith("!"):
                    clean = msg.replace("antigravity", "").replace("!ai", "").replace("!", "").strip()

                    if any(w in msg_lower for w in ["vem", "segue", "follow", "aqui"]):
                        STATE["target"] = user
                        STATE["following"] = True
                        send_chat(f"Te seguindo agora, {user}!")
                        set_speech(f"Seguindo {user}!", "green", 5)
                        continue

                    if any(w in msg_lower for w in ["para", "fica", "stop", "espera"]):
                        STATE["following"] = False
                        send_chat("Montando guarda nesta posição!")
                        set_speech("Montando guarda!", "gold", 5)
                        continue

                    if "lanca" in msg_lower or "flame" in msg_lower:
                        STATE["weapon"] = "jeg:flamethrower"
                        run_cmd("item replace entity @e[tag=npc_antigravity,limit=1] weapon.mainhand with jeg:flamethrower")
                        send_chat("🔥 Lança-chamas em mãos! Fogo à vontade!")
                        set_speech("🔥 Lança-chamas!", "red", 5)
                        continue

                    if "fuzil" in msg_lower or "rifle" in msg_lower or "assault" in msg_lower:
                        STATE["weapon"] = "jeg:assault_rifle"
                        run_cmd("item replace entity @e[tag=npc_antigravity,limit=1] weapon.mainhand with jeg:assault_rifle")
                        send_chat("🎯 Fuzil engatilhado e pronto!")
                        set_speech("🎯 Fuzil pronto!", "green", 5)
                        continue

                    if clean:
                        ask_gemini(user, clean)
            except Exception as e:
                print(f"[PARSE ERR] {e}")
    except Exception as e:
        print(f"[LOG ERR] {e}")

def main():
    print("[*] Antigravity NPC AI iniciado...")
    t = threading.Thread(target=log_listener, daemon=True)
    t.start()

    while True:
        try:
            time.sleep(0.8)

            if STATE["speech_timer"] > 0 and time.time() > STATE["speech_timer"]:
                STATE["speech_timer"] = 0
                run_cmd("execute at @e[tag=npc_speech] run data merge entity @e[tag=npc_speech,limit=1] {text:'{\"text\":\"🛡️ Em guarda\",\"color\":\"gray\"}'}")

            if not STATE["following"]:
                continue

            target = get_player(STATE["target"]) or get_player("Pace") or get_player("Lizzie")
            if not target:
                continue

            tx, ty, tz = target["x"], target["y"], target["z"]
            dist = math.sqrt((tx - STATE["npc_x"])**2 + (tz - STATE["npc_z"])**2)

            # Teleport if far
            if dist > 20 or abs(ty - STATE["npc_y"]) > 8:
                nx = tx - 2.0
                ny = ty
                nz = tz - 2.0
                run_cmd(f"tp @e[tag=npc_antigravity,limit=1] {nx:.2f} {ny:.2f} {nz:.2f} facing entity {target['name']}")
                run_cmd(f"tp @e[tag=npc_speech,limit=1] {nx:.2f} {ny+2.25:.2f} {nz:.2f}")
                STATE["npc_x"], STATE["npc_y"], STATE["npc_z"] = nx, ny, nz
                continue

            # Follow step if > 3.5m
            if dist > 3.5:
                angle = math.atan2(tz - STATE["npc_z"], tx - STATE["npc_x"])
                step = min(1.2, dist * 0.4)
                nx = STATE["npc_x"] + step * math.cos(angle)
                nz = STATE["npc_z"] + step * math.sin(angle)
                ny = ty

                run_cmd(f"tp @e[tag=npc_antigravity,limit=1] {nx:.2f} {ny:.2f} {nz:.2f} facing entity {target['name']}")
                run_cmd(f"tp @e[tag=npc_speech,limit=1] {nx:.2f} {ny+2.25:.2f} {nz:.2f}")
                STATE["npc_x"], STATE["npc_y"], STATE["npc_z"] = nx, ny, nz
            else:
                # Close enough, just face player
                run_cmd(f"execute at @e[tag=npc_antigravity,limit=1] run tp @e[tag=npc_antigravity,limit=1] ~ ~ ~ facing entity {target['name']}")

        except Exception as e:
            print(f"[ERR] {e}")

if __name__ == "__main__":
    main()
