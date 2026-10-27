JAVAC = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/javac --release 21
JAR = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/jar
JAVA = /home/pace/.minecraft/runtime/java-runtime-delta/linux/java-runtime-delta/bin/java

SRC_JAVA = src/java/com/craft/agent/CraftAgent.java src/java/com/craft/agent/Injector.java
BUILD_DIR = target/classes
AGENT_JAR = target/craft-repl-agent.jar
PORT ?= 7888

.PHONY: all build jar attach clean repl test

all: jar

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

attach: jar
	@PID_RESOLVED=$$( [ -n "$(PID)" ] && echo "$(PID)" || pgrep -f "KnotClient" | head -n 1 ); \
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
