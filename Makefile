JAVAC = javac
JAR = jar
JAVA = java

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
	@if [ -z "$(PID)" ]; then \
		echo "Erro: Defina o PID do Minecraft. Exemplo: make attach PID=195362 [PORT=7888]"; \
		exit 1; \
	fi
	$(JAVA) -cp $(BUILD_DIR) com.craft.agent.Injector $(PID) $(AGENT_JAR) $(PORT)

repl:
	clj -M:test -m nrepl.cmdline --connect --host 127.0.0.1 --port $(PORT)

test:
	clj -M:test -m cognitect.test-runner

clean:
	rm -rf target .cpcache
