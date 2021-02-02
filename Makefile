
JDK = /opt/jdk16
JUNIT = 5.7.0

JAVA = $(JDK)/bin/java

JAVAC = $(JDK)/bin/javac
JAR = $(JDK)/bin/jar


REPO = https://repo1.maven.org/maven2

define MAVEN
	curl -sS $(REPO)/$(1)/$(2)/$(3)/$(2)-$(3).jar -o $(4)/$(2).jar
endef


default: jar

jar: dist/mtc-junit5.jar


dist/mtc-junit5.jar: lib/junit-jupiter-api.jar
	rm -fr bin
	mkdir -p bin
	$(JAVAC) -d bin \
		-cp $(shell find lib -name '*.jar' -printf %p:) \
		$(shell find src -name '*.java')
	mkdir -p $(@D)
	$(JAR) -c -f $@ -C bin .

test: dist/mtc-junit5.jar lib/junit-platform-launcher.jar
	rm -fr bin
	mkdir -p bin
	$(JAVAC) -d bin \
		-cp $<$(shell find lib -name '*.jar' -printf :%p) \
		$(shell find test -name '*.java')
	$(JAVA) \
		-cp $(shell find dist lib -name '*.jar' -printf %p:) \
		org.junit.platform.console.ConsoleLauncher \
		--details=summary --disable-banner \
		--class-path=bin --scan-classpath \
		> test.out 2>test.err


lib/junit-jupiter-api.jar:
	mkdir -p $(@D)
	$(call MAVEN,org/junit/jupiter,junit-jupiter-api,5.7.0,$(@D))
	$(call MAVEN,org/apiguardian,apiguardian-api,1.1.0,$(@D))

lib/junit-platform-launcher.jar: lib/junit-jupiter-api.jar
	mkdir -p $(@D)
	$(call MAVEN,org/junit/jupiter,junit-jupiter-engine,5.7.0,$(@D))
	$(call MAVEN,org/junit/platform,junit-platform-commons,1.7.0,$(@D))
	$(call MAVEN,org/junit/platform,junit-platform-console,1.7.0,$(@D))
	$(call MAVEN,org/junit/platform,junit-platform-engine,1.7.0,$(@D))
	$(call MAVEN,org/junit/platform,junit-platform-launcher,1.7.0,$(@D))
	$(call MAVEN,org/opentest4j,opentest4j,1.2.0,$(@D))


clean:
	rm -fr bin dist lib test.out test.err


