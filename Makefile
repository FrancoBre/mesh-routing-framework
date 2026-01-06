JAR=$(firstword $(wildcard target/*-SNAPSHOT.jar))
MAIN=org.ungs.cli.Main

.PHONY: all run build clean

all: build

build:
	mvn clean package

run: build
	java -cp $(JAR) $(MAIN)

run-debug: build
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -cp $(JAR) $(MAIN)

clean:
	mvn clean
