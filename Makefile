JAVAC = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/javac --release 21
JAR = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/jar
JAVA = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/java

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
	$(JAVAC) -d target/bin \
		src/java/com/craft/patch/CommandRunnerAgent.java \
		src/java/com/craft/skin/ExtractSkin.java \
		src/java/com/craft/skin/SkinRestorer.java
	@echo "Manifest-Version: 1.0\nAgent-Class: com.craft.patch.CommandRunnerAgent\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true" > target/mf_cmd.mf
	$(JAR) cmf target/mf_cmd.mf target/live-cmd.jar -C target/bin com/craft/patch/CommandRunnerAgent.class
	@echo "Manifest-Version: 1.0\nAgent-Class: com.craft.skin.ExtractSkin\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true" > target/mf_extract.mf
	$(JAR) cmf target/mf_extract.mf target/extract-skin.jar -C target/bin com/craft/skin/ExtractSkin.class
	@echo "Manifest-Version: 1.0\nAgent-Class: com.craft.skin.SkinRestorer\nCan-Redefine-Classes: true\nCan-Retransform-Classes: true" > target/mf_restore.mf
	$(JAR) cmf target/mf_restore.mf target/restore-skin.jar -C target/bin com/craft/skin/SkinRestorer.class

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

clean:
	rm -rf target .cpcache
