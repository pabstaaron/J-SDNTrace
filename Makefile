# Define compiler for building JSDNTrace and JPcap
JFLAGS = -g -cp
JC = javac

# Define C compiler for building libpcap
CC = g++

.SUFFIXES: .java .class

# Define where the floodlight logging objects are.
SLF4J = /home/aaron/floodlight/lib/slf4j-api-1.6.4.jar

# Define where the floodlight types are
# TODO: How to I properly point to a JAR?
FLOODLIGHT_TYPES = /home/aaron/floodlight/lib/openflowj-0.9.0-SNAPSHOT.jar

JPCAP = jpcap/usr/java/packages/lib/ext/jpcap.jar

JARS = $(SLF4J):$(FLOODLIGHT_TYPES):$(JPCAP)

FLOODLIGHT_PACKET = $(shell find ~/floodlight/src/main/java/net/floodlightcontroller/packet -type f -name '*.java')

CLASSES = \
	$(FLOODLIGHT_PACKET) \
	src/traceapp/core/Hop.java \
	src/traceapp/core/TracePacket.java \
	src/traceapp/core/JpcapTracePacket.java \
	src/traceapp/core/TraceClient.java

classes: $(CLASSES:.java=.class)

.java.class:
	$(JC) -g -cp  $(JARS) $(CLASSES)

default: classes

clean: 
	rm SDNTrace.jar





