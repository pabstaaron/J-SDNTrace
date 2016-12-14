# Define compiler for building JSDNTrace and JPcap
JFLAGS = -g -cp
JC = javac

# Define C compiler for building libpcap
CC = g++

# The directory where compiled java source will be placed
CLASS_DIR = bin/

.SUFFIXES: .java .class

# Define where the floodlight logging objects are.
SLF4J = /home/aaron/floodlight/lib/slf4j-api-1.6.4.jar

# Define where the floodlight types are
# TODO: How to I properly point to a JAR?
FLOODLIGHT_TYPES = /home/aaron/floodlight/lib/openflowj-0.9.0-SNAPSHOT.jar

JPCAP = jpcap/usr/java/packages/lib/ext/jpcap.jar

JARS = $(SLF4J):$(FLOODLIGHT_TYPES):$(JPCAP)

OUTPUT_JAR = TraceClient.jar

JAR = jar
JARFlags = -cvf

FLOODLIGHT_PACKET = $(shell find ~/floodlight/src/main/java/net/floodlightcontroller/packet -type f -name '*.java')

java_source = \
	$(FLOODLIGHT_PACKET) \
	src/traceapp/core/Hop.java \
	src/traceapp/core/TracePacket.java \
	src/traceapp/core/JpcapTracePacket.java \
	src/traceapp/core/TraceClient.java

classes: $(java_source:.java=.class)

# Compile all the necessary java source into class files
.java.class:
	$(JC) $(JFLAGS) $(JARS) $(java_source) -d $(CLASS_DIR)

.PHONY: $(OUTPUT_JAR)
$(OUTPUT_JAR):
	$(JAR) -cvf $@ $(CLASS_DIR) 

all: .java.class .PHONY

default: all

clean: 
	rm bin/traceapp/core/Hop.class
	rm bin/traceapp/core/TracePacket.class
	rm bin/traceapp/core/JpcapTracePacket.class
	rm bin/traceapp/core/TraceClient.class
	rm -r bin/net
	rm TraceClient.jar





