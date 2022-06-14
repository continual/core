#!/bin/sh

CLASSNAME=io.continual.email.impl.EmailTestSender

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

# run java. The classpath is the etc dir for config files, and the lib dir
# for all the jars.
$JAVA -cp ${BASE_DIR}/etc:${BASE_DIR}/lib/* ${CLASSNAME} $*
