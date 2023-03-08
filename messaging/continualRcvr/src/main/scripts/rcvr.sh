#!/bin/sh

CLASSNAME=io.continual.services.rcvr.RcvrServer

# move into base directory
BASEDIR=`dirname $0`
cd $BASEDIR/..

# use JAVA_HOME if provided
if [ -z $JAVA_HOME ]; then
	JAVA=java
else
	JAVA=$JAVA_HOME/bin/java
fi

# run java
$JAVA $JAVA_OPTS -cp etc:lib/* $CLASSNAME $RCVR_CMDLINE_ARGS $*
