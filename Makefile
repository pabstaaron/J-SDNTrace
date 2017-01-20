# Define compiler for building JSDNTrace and JPcap
JFLAGS = -g -cp
JC = javac

# Define C compiler for building libpcap
CC = g++

# Define UNIX Tools
AWK = awk
FIND = find
MKDIR = mkdir -p
RM = rm -rf
SHELL = /bin/bash

# The directory where compiled java source will be placed
CLASS_DIR = bin/

.SUFFIXES: .java .class

# Define where the floodlight logging objects are.
SLF4J = /home/aaron/eclipseWorkspace/floodlight/lib/slf4j-api-1.6.4.jar

# Define where the floodlight types are
# TODO: How to I properly point to a JAR?
FLOODLIGHT_TYPES = /home/aaron/eclipseWorkspace/floodlight/lib/openflowj-3.0.0-SNAPSHOT.jar

JPCAP = jpcap/usr/java/packages/lib/ext/jpcap.jar

JARS = $(SLF4J):$(FLOODLIGHT_TYPES):$(JPCAP)

OUTPUT_JAR = TraceClient.jar

JAR = jar
JARFlags = -cvf

MANIFEST_TEMPLATE = src/manifests/default.mf
TMP_JAR_DIR       = $(call make-temp-dir)
TMP_MANIFEST      = $(TMP_JAR_DIR)/manifest.m
VERSION_NUMBER = 1.0

FLOODLIGHT_PACKET = $(shell $(FIND) ~/eclipseWorkspace/floodlight/src/main/java/net/floodlightcontroller/packet -type f -name '*.java')

CORE_FOLDER = src/traceapp/src/main/java/core

java_source = \
	$(FLOODLIGHT_PACKET) \
	$(CORE_FOLDER)/Hop.java \
	$(CORE_FOLDER)/TracePacket.java \
	$(CORE_FOLDER)/JpcapTracePacket.java \
	$(CORE_FOLDER)/TraceClient.java

classes: $(java_source:.java=.class)

# Compile all the necessary java source into class files
.java.class:
	$(JC) $(JFLAGS) $(JARS) $(java_source) -d $(CLASS_DIR)

.PHONY: $(OUTPUT_JAR)
$(OUTPUT_JAR):
	$(JAR) cfm $@ $(MANIFEST_TEMPLATE) $(CLASS_DIR)

# #$(JAR) cfm $@ $(MANIFEST_TEMPLATE) 

all: .java.class .PHONY

default: all

rebuild: clean all

clean: 
	rm bin/traceapp/core/Hop.class
	rm bin/traceapp/core/TracePacket.class
	rm bin/traceapp/core/JpcapTracePacket.class
	rm bin/traceapp/core/TraceClient.class
	rm -r bin/net
	rm TraceClient.jar

## Miscellaneous Functions Defined Below ##

# #(call make-temp-dir, root-opt)
define make-temp-dir
  mktemp -t $(if $1,$1,make).XXXXXXXXXX
endef

# $(call add-manifest, jar, jar-name, manifest-file-opt)
define add-manifest
  $(RM) $(dir $(TMP_MANIFEST))
  $(MKDIR) $(dir $(TMP_MANIFEST))
  m4 --define=NAME="$(notdir $2)"                       \
     --define=IMPL_VERSION=$(VERSION_NUMBER)            \
     --define=SPEC_VERSION=$(VERSION_NUMBER)            \
     $(if $3,$3,$(MANIFEST_TEMPLATE))                   \
     > $(TMP_MANIFEST)
  $(JAR) -ufm $1 $(TMP_MANIFEST)
  $(RM) $(dir $(TMP_MANIFEST))
endef

# $(call make-jar,jar-variable-prefix)
define make-jar
  .PHONY: $1 $$($1_name)
  $1: $($1_name)
  $$($1_name):
        cd $(OUTPUT_DIR); \
        $(JAR) $(JARFLAGS) $$(notdir $$@) $$($1_packages)
        $$(call add-manifest, $$@, $$($1_name), $$($1_manifest))
endef


