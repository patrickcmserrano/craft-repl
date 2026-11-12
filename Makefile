JAVAC = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/javac --release 21
JAR = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/jar
JAVA = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/java
ASM_JAR = /home/pace/.minecraft/libraries/org/ow2/asm/asm/9.11/asm-9.11.jar

SRC_JAVA = src/java/com/craft/agent/CraftAgent.java src/java/com/craft/agent/Injector.java
BUILD_DIR = target/classes
AGENT_JAR = target/craft-repl-agent.jar
PORT ?= 7888

.PHONY: all build jar agents attach clean repl test

all: jar agents

build:
	@mkdir -p $(BUILD_DIR)
	$(JAVAC) -d $(BUILD_DIR) $(SRC_JAVA)

jar: build
	@mkdir -p target
	@echo "Manifest-Version: 1.0" > target/MANIFEST.MF
	@echo "Agent-Class: com.craft.agent.CraftAgent" >> target/MANIFEST.MF
	@echo "Can-Redefine-Classes: true" >> target/MANIFEST.MF
	@echo "Can-Retransform-Classes: true" >> target/MANIFEST.MF
	$(JAR) cmf target/MANIFEST.MF $(AGENT_JAR) -C $(BUILD_DIR) com/craft/agent/CraftAgent.class

agents: build
	@mkdir -p target/bin target
	$(JAVAC) -cp $(ASM_JAR) -d target/bin \
		src/java/com/craft/patch/CommandRunnerAgent.java \
		src/java/com/craft/patch/LivePosAgent.java \
		src/java/com/craft/skin/ExtractSkin.java \
		src/java/com/craft/skin/SkinRestorer.java \
		src/java/com/craft/skin/SkinHttpHook.java \
		src/java/com/craft/skin/ClientSkinCacheFixer.java
	@printf "Manifest-Version: 1.0\nAgent-Class: com.craft.patch.CommandRunnerAgent\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\n" > target/mf_cmd.mf
	$(JAR) cmf target/mf_cmd.mf target/live-cmd.jar -C target/bin com/craft/patch/CommandRunnerAgent.class
	@printf "Manifest-Version: 1.0\nAgent-Class: com.craft.patch.LivePosAgent\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\n" > target/mf_pos.mf
	$(JAR) cmf target/mf_pos.mf target/live-pos.jar -C target/bin com/craft/patch/LivePosAgent.class
	@printf "Manifest-Version: 1.0\nAgent-Class: com.craft.skin.ExtractSkin\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\n" > target/mf_extract.mf
	$(JAR) cmf target/mf_extract.mf target/extract-skin.jar -C target/bin com/craft/skin/ExtractSkin.class
	@printf "Manifest-Version: 1.0\nAgent-Class: com.craft.skin.SkinRestorer\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\n" > target/mf_restore.mf
	$(JAR) cmf target/mf_restore.mf target/restore-skin.jar -C target/bin com/craft/skin/SkinRestorer.class
	@printf "Manifest-Version: 1.0\nAgent-Class: com.craft.skin.ClientSkinCacheFixer\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true\n" > target/mf_client_skin.mf
	@rm -rf target/skin-client && mkdir -p target/skin-client/com/craft/skin
	@cp target/bin/com/craft/skin/ClientSkinCacheFixer*.class target/skin-client/com/craft/skin/
	@cp target/bin/com/craft/skin/SkinHttpHook.class target/skin-client/com/craft/skin/
	@cd target/skin-client && $(JAR) xf $(ASM_JAR) org/objectweb/asm
	$(JAR) cmf target/mf_client_skin.mf target/fix-client-skin.jar -C target/skin-client .

attach: jar
	@PID_RESOLVED=$$( [ -n "$(PID)" ] && echo "$(PID)" || pgrep -f "KnotServer" | head -n 1 ); \
	if [ -z "$$PID_RESOLVED" ]; then PID_RESOLVED=$$(pgrep -f "KnotClient" | head -n 1); fi; \
	if [ -z "$$PID_RESOLVED" ]; then \
		echo "[-] Erro: Minecraft não detectado em execução!"; \
		exit 1; \
	fi; \
	echo "[+] Minecraft detectado com PID: $$PID_RESOLVED"; \
	$(JAVA) -cp $(BUILD_DIR) com.craft.agent.Injector $$PID_RESOLVED $(AGENT_JAR) $(PORT)

repl:
	clojure -M -e "(require '[nrepl.cmdline]) (nrepl.cmdline/-main \"--connect\" \"--host\" \"127.0.0.1\" \"--port\" \"$(PORT)\")"

test:
	clojure -M:test -m craft.test-runner
	$(MAKE) test-bot

test-bot:
	cd bot && npm test

test-live:
	clojure -M:test -m craft.smoke-live

# Valida o hook de skin fora do cliente, em duas frentes:
#   1. SkinHookProbe      — serve() devolve o JSON local e a injecao ASM
#                           (COMPUTE_FRAMES) passa o verificador REAL da JVM;
#   2. SkinAgentLoadProbe — o jar do agente resolve agentmain + todas as
#                           classes internas (pega NoClassDefFoundError de
#                           $N faltando ou de ASM ausente) ANTES do attach.
SKIN_CLIENT_JAR ?= /home/pace/.minecraft/sklauncher-fx.jar
skin-probe: agents
	$(JAVAC) -cp $(ASM_JAR) -d target/bin \
		src/java/com/craft/skin/SkinHttpHook.java \
		src/java/com/craft/skin/SkinHookProbe.java \
		src/java/com/craft/skin/SkinAgentLoadProbe.java
	@echo "--- probe 1: serve() + injecao ASM ---"
	$(JAVA) -cp "target/bin:$(ASM_JAR):$(SKIN_CLIENT_JAR)" com.craft.skin.SkinHookProbe $(SKIN_CLIENT_JAR)
	@echo "--- probe 2: carga do jar do agente (loader limpo) ---"
	$(JAVA) -cp target/bin com.craft.skin.SkinAgentLoadProbe target/fix-client-skin.jar

clean:
	rm -rf target .cpcache
