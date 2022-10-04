#!/bin/bash

# We have 64GB of available memory
# (4 * 14 GB) + 6 GB = 62 GB
# This leaves 2GB spare for system stability

# printing tool versions
java --version
mvn --version
gpg --version

# setting env variables
export MAVEN_OPTS="-Xms6g -Xmx6g"

# running the build
mvn -B -e -T 4 install -DargLine="-Xms14g -Xmx14g"
