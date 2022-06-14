#!/bin/sh

CLASSNAME=io.continual.services.processor.engine.runtime.ProgramRunner

# switch to the application's base directory so that various relative 
# paths work properly (e.g. logging, config files)
BASE_DIR=`dirname "$0"`/..
cd ${BASE_DIR}

# use JAVA_HOME if provided
if [ -n "${JAVA_HOME}" ]; then
    JAVA=${JAVA_HOME}/bin/java
else
    JAVA=java
fi

# make sure the logs dir exists
mkdir -p ${BASE_DIR}/logs
umask 022

# Start JMX if given a port value
if [ -n "${PROCESSOR_JMX_PORT}" ]; then
	PROCESSOR_JMX="-Dcom.sun.management.jmxremote.port=${PROCESSOR_JMX_PORT} -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=true"
fi

# run java. The classpath is the etc dir for config files, and the lib dir
# for all the jars.
$JAVA ${PROCESSOR_JMX} -cp ${BASE_DIR}/etc:${BASE_DIR}/lib/* ${CLASSNAME} $*
