#!/bin/sh

CLASSNAME=io.continual.iam.apiserver.IamApiServer

# move into base directory
BASEDIR=`dirname $0`
cd $BASEDIR/..

# use JAVA_HOME if provided
if [ -z $JAVA_HOME ]; then
	JAVA=java
else
	JAVA=$JAVA_HOME/bin/java
fi

# run java. The classpath is the etc dir for config files, and the lib dir
# for all the jars.
$JAVA $JAVA_OPTS -cp etc:lib/* $CLASSNAME $*

