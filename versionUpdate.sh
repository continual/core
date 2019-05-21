#!/bin/sh

mvn versions:set -DgenerateBackupPoms=false
mvn versions:update-child-modules -DgenerateBackupPoms=false

