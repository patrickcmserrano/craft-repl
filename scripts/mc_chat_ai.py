#!/usr/bin/env python3
"""
mc_chat_ai.py - Antigravity In-Game Minecraft AI Chat Proxy
Listens to latest.log for '!ai <prompt>' or '! <prompt>' and uses agy CLI
to execute in-game commands and converse in real-time.
"""

import os
import sys
import time
import re
import json
import subprocess
import urllib.request
import urllib.error

LOG_PATH = "/home/pace/minecraft-server/logs/latest.log"
API_URL = "http://localhost:8090/api/run"
TELEMETRY_URL = "http://localhost:8090/api/telemetry"

CHAT_REGEX = re.compile(r'\[\d{2}:\d{2}:\d{2}\]\s+\[Server thread/INFO\]:\s+<([a-zA-Z0-9_]+)>\s+(.+)', re.IGNORECASE)

def run_cmd(cmd):
    """Executes a command via mc-dashboard /api/run."""
    try:
        data = json.dumps({"cmd": cmd}).encode("utf-8")
        req = urllib.request.Request(API_URL, data=data, headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=8) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"[ERROR] run_cmd failed: {e}", file=sys.stderr)
        return {"ok": False, "error": str(e)}

def send_tellraw(msg, color="aqua", bold=False):
    """Broadcasts a formatted message from Antigravity to all players."""
    escaped_msg = msg.replace('\\', '\\\\').replace('"', '\\"').replace("\n", " ")
    b_flag = ',"bold":true' if bold else ''
    cmd = f'tellraw @a [{{"text":"[Antigravity] ","color":"light_purple","bold":true}},{{"text":"{escaped_msg}","color":"{color}"{b_flag}}}]          '
    run_cmd(cmd)

def play_sound(sound, player="@a"):
    run_cmd(f"execute as {player} at @s run playsound {sound} master @s ~ ~ ~ 1 1")

def get_telemetry():
    """Gets current coordinates for all players."""
    try:
        with urllib.request.urlopen(TELEMETRY_URL, timeout=4) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception:
        return {}

def handle_quick_commands(player, prompt):
    p = prompt.strip().lower()
    if p in ["dia", "day"]:
        run_cmd("time set day")
        send_tellraw(f"☀️ Dia amanhecendo para você, {player}!", color="yellow")
        play_sound("minecraft:block.note_block.chime", player)
        return True
    elif p in ["noite", "night"]:
        run_cmd("time set night")
        send_tellraw(f"🌙 Noite estrelada definida!", color="blue")
        play_sound("minecraft:block.note_block.chime", player)
        return True
    elif p in ["sol", "limpar", "clear"]:
        run_cmd("weather clear")
        send_tellraw(f"☀️ Tempo limpo e ensolarado!", color="yellow")
        play_sound("minecraft:block.note_block.chime", player)
        return True
    elif p in ["chuva", "rain"]:
        run_cmd("weather rain")
        send_tellraw(f"🌧️ Chuva iniciada!", color="blue")
        play_sound("minecraft:block.note_block.chime", player)
        return True
    elif any(k in p for k in ["curar", "cura", "heal"]):
        run_cmd(f"effect give {player} minecraft:instant_health 1 255 true")
        run_cmd(f"effect give {player} minecraft:saturation 5 255 true")
        send_tellraw(f"💚 {player} curado com sucesso!", color="green")
        play_sound("minecraft:entity.player.levelup", player)
        return True
    elif any(k in p for k in ["municao", "munição", "bala", "balas", "ammo"]):
        if any(k in p for k in ["lanca", "chama", "flame", "fogo"]):
            run_cmd(f"give {player} minecraft:fire_charge 64")
            run_cmd(f"give {player} minecraft:fire_charge 64")
            send_tellraw(f"🔥 128x Cargas de Fogo (Combustível do Lança-chamas) entregues para {player}!", color="gold")
        elif any(k in p for k in ["escopeta", "shotgun", "doze", "calibre 12"]):
            run_cmd(f"give {player} jeg:shotgun_shell 64")
            run_cmd(f"give {player} jeg:shotgun_shell 64")
            run_cmd(f"give {player} jeg:shotgun_drum_magazine 2")
            send_tellraw(f"💥 128x Cartuchos de Escopeta + 2x Tambores entregues para {player}!", color="gold")
        elif any(k in p for k in ["pistola", "pistol", "smg"]):
            run_cmd(f"give {player} jeg:pistol_ammo 64")
            run_cmd(f"give {player} jeg:pistol_ammo 64")
            send_tellraw(f"🔫 128x Balas de Pistola/SMG entregues para {player}!", color="gold")
        else:
            # Padrão ou Fuzis
            run_cmd(f"give {player} jeg:rifle_ammo 64")
            run_cmd(f"give {player} jeg:rifle_ammo 64")
            run_cmd(f"give {player} minecraft:fire_charge 64")
            run_cmd(f"give {player} jeg:rifle_drum_magazine 2")
            send_tellraw(f"🎯 128x Munição de Fuzil + 64x Cargas de Fogo + 2x Tambores entregues para {player}!", color="gold")
        play_sound("minecraft:entity.item.pickup", player)
        return True
    elif any(k in p for k in ["castelo gotico", "gothic castle", "mega castelo", "castelo 100x100"]):
        send_tellraw(f"🏰 Iniciando construção da Cidadela Gótica (100x100x90) para {player}...", color="gold", bold=True)
        subprocess.Popen(["/usr/bin/python3", "/home/pace/dev/craft-repl/scripts/build_gothic_megacastle.py", player])
        return True
    elif any(k in p for k in ["desfaça", "desfazer", "explode", "demolir", "remover", "limpar castelo"]):
        cmds = [
            "gamerule commandModificationBlockLimit 1000000",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace stone_bricks",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace deepslate_bricks",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace stone_brick_wall",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace deepslate_tile_wall",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace stone_brick_slab",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace deepslate_tiles",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace crying_obsidian",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace gold_block",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace beacon",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace iron_bars",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace oak_planks",
            f"execute at {player} run fill ~-30 ~-2 ~-30 ~30 ~35 ~30 air replace water",
            f"execute at {player} run fill ~-30 ~-1 ~-30 ~30 ~-1 ~30 grass_block replace dirt",
            f"execute at {player} run particle explosion_emitter ~ ~2 ~ 10 5 10 0.5 80",
            f"execute at {player} run playsound minecraft:entity.generic.explode master @a ~ ~ ~ 1 0.8"
        ]
        for c in cmds:
            run_cmd(c)
        send_tellraw(f"💥 Castelo demolido e limpo ao redor de {player}!", color="gold")
        return True
    return False

def sanitize_command(cmd):
    """Prevents lag-inducing commands like fill destroy or gigantic areas."""
    c = cmd.strip()
    # 1. Never allow 'destroy' in fill commands - replace with 'replace'
    if re.search(r'\bfill\b.*\bdestroy\b', c, re.IGNORECASE):
        c = re.sub(r'\bdestroy\b', 'replace', c, flags=re.IGNORECASE)
    return c

def process_ai_prompt(player, prompt):
    print(f"[PROMPT] from <{player}>: {prompt}")

    # 1. Quick command check
    if handle_quick_commands(player, prompt):
        return

    # 2. Acknowledge in-game
    send_tellraw(f"⏳ Processando: \"{prompt}\"...", color="gray")
    play_sound("minecraft:block.note_block.chime")

    # 3. Telemetry context
    telem = get_telemetry()
    players = telem.get("players", [])
    p_info = next((p for p in players if p.get("name", "").lower() == player.lower()), None)
    
    pos_desc = "posição desconhecida"
    if p_info:
        px = round(p_info.get("x", 0), 1)
        py = round(p_info.get("y", 0), 1)
        pz = round(p_info.get("z", 0), 1)
        yaw = round(p_info.get("yaw", 0), 1)
        pos_desc = f"x={px}, y={py}, z={pz}, yaw={yaw}"

    system_instruction = f"""Você é o assistente Antigravity conectado ao servidor Minecraft de Pace e Lizzie.
O jogador '{player}' está na posição {pos_desc}.
O pedido do jogador é: "{prompt}".

Responda ESTRITAMENTE com um objeto JSON válido, sem markdown envolvente e sem explicações fora do JSON:
{{
  "reply": "Mensagem curta e amigável em português para aparecer no chat in-game (máx 120 caracteres)",
  "commands": [
    "lista de comandos minecraft a executar, ex: execute at {player} run ... ou setblock / summon / particle"
  ]
}}
Regras obrigatórias:
- TABELA EXATA DE MUNIÇÕES DO MOD JUST ENOUGH GUNS (JEG):
  * Fuzis (Assault Rifle, Service Rifle, Bolt Action, Combat Rifle): use "jeg:rifle_ammo" (e carregador "jeg:rifle_drum_magazine")
  * Lança-chamas (Flamethrower): use "minecraft:fire_charge" (Cargas de Fogo do Minecraft)
  * Escopetas (Pump, Double Barrel, Repeating): use "jeg:shotgun_shell" (e carregador "jeg:shotgun_drum_magazine")
  * Pistolas / SMGs: use "jeg:pistol_ammo"
  * Lança-Foguetes: use "jeg:rocket"
  * Se o jogador pedir qualquer munição, use "/give {player} <item_exato> <quantidade>".
- Se for para criar ou modificar algo perto do jogador, use "execute at {player} run ..." ou coordenadas locais "^ ^ ^" ou "~".
- Se o pedido for EXPLODIR ou DETONAR uma área:
  * NUNCA use 'destroy' em /fill, use sempre 'air replace'!
  * O /fill deve escavar o CHÃO PARA BAIXO (ex: de Y=15 até Y=75) para abrir uma cratera profunda visível!
  * SEMPRE toque o som de explosão para todos os jogadores: "execute as @a at @s run playsound minecraft:entity.generic.explode master @s ~ ~ ~ 2 0.8"
  * Invoque partículas de explosão perto do jogador: "execute at {player} run particle explosion_emitter ~ ~2 ~ 3 2 3 0.2 30"
  * Dê proteção aos jogadores se for uma explosão gigante: "effect give @a minecraft:resistance 20 255 true"
- Se for apenas uma dúvida ou conversa, "commands" deve ser [].
- Para estruturas ou blocos, use comandos válidos do Minecraft 1.21.
- Responda apenas o JSON puro."""

    try:
        # Call agy CLI using fast Flash model
        cmd = ["agy", "-p", system_instruction, "--model", "Gemini 3.8 Flash (Low)", "--dangerously-skip-permissions"]
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=45)
        output = res.stdout.strip()
        print(f"[AGY OUTPUT]: {output}")

        # Parse JSON
        m = re.search(r'\{.*\}', output, re.DOTALL)
        if m:
            data = json.loads(m.group(0))
            reply = data.get("reply", "Feito!")
            commands = data.get("commands", [])

            # Execute commands with safety guardrails
            for c in commands:
                safe_c = sanitize_command(c)
                print(f"[EXEC]: {safe_c}", flush=True)
                run_cmd(safe_c)

            # Send reply
            send_tellraw(reply, color="aqua", bold=False)
            play_sound("minecraft:entity.experience_orb.pickup")
        else:
            send_tellraw(f"🤖 {output[:100]}", color="white")
            play_sound("minecraft:entity.experience_orb.pickup")
    except subprocess.TimeoutExpired:
        send_tellraw("⚠️ O processamento demorou muito. Tente novamente!", color="red")
    except Exception as e:
        print(f"[ERROR in process_ai_prompt]: {e}", file=sys.stderr)
        send_tellraw(f"⚠️ Erro ao processar prompt: {str(e)[:60]}", color="red")

def main():
    print(f"[*] Antigravity In-Game AI Proxy iniciado. Monitorando {LOG_PATH}...")
    send_tellraw("🟢 Proxy de IA In-game ativado! Usem '!ai <pedido>' ou '! <pedido>' no chat!", color="green", bold=True)
    play_sound("minecraft:entity.player.levelup")

    while not os.path.exists(LOG_PATH):
        time.sleep(1)

    current_inode = os.stat(LOG_PATH).st_ino
    f = open(LOG_PATH, "r", encoding="utf-8", errors="replace")
    f.seek(0, os.SEEK_END)

    while True:
        line = f.readline()
        if not line:
            time.sleep(0.3)
            # Detect log rotation or server restart
            try:
                stat = os.stat(LOG_PATH)
                if stat.st_ino != current_inode or stat.st_size < f.tell():
                    f.close()
                    f = open(LOG_PATH, "r", encoding="utf-8", errors="replace")
                    current_inode = stat.st_ino
                    f.seek(0, os.SEEK_END)
            except Exception:
                pass
            continue

        # Auto-apply skin on player join
        if "joined the game" in line:
            m_join = re.search(r':\s+([a-zA-Z0-9_]+)\s+joined the game', line)
            if m_join:
                joined_player = m_join.group(1)
                if joined_player.lower() == "lizzie":
                    print(f"[*] Lizzie conectou! Aplicando skin M0r7eM automaticamente...", flush=True)
                    time.sleep(1.0)
                    subprocess.run(["/home/pace/.local/bin/mc", "skin-restore", "Lizzie", "lizzie"])
                elif joined_player.lower() == "pace":
                    print(f"[*] pace conectou! Sincronizando skins no cliente...", flush=True)
                    time.sleep(1.5)
                    subprocess.run(["/home/pace/.local/bin/mc", "skin-restore", "pace", "pace"])
                    subprocess.run(["/home/pace/.local/bin/mc", "skin-restore", "Lizzie", "lizzie"])

        match = CHAT_REGEX.search(line)
        if match:
            player = match.group(1)
            raw_msg = match.group(2).strip()

            is_ai = False
            prompt = ""
            if raw_msg.startswith("!"):
                is_ai = True
                prompt = re.sub(r'^!(?:ai\s*|\s*)', '', raw_msg, flags=re.IGNORECASE).strip()
            elif re.search(r'\bantigravity\b', raw_msg, re.IGNORECASE):
                is_ai = True
                prompt = re.sub(r'@?antigravity[:,]?', '', raw_msg, flags=re.IGNORECASE).strip()

            if is_ai and prompt and player.lower() != "antigravity":
                print(f"[PROMPT MATCHED] from <{player}>: {prompt}", flush=True)
                process_ai_prompt(player, prompt)

if __name__ == "__main__":
    main()
