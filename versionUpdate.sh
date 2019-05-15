#!/bin/sh

mvn versions:set -DgenerateBackupPoms=false -DoffNetwork=true
mvn versions:update-child-modules -DgenerateBackupPoms=false -DoffNetwork=true
