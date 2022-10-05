#!/bin/bash

# printing tool versions
java --version
mvn --version
gpg --version

grep MemTotal /proc/meminfo

# setting env variables
export MAVEN_OPTS="-Xms10g -Xmx10g"

# running the build
mvn -B -e -T 4 install -DargLine="-Xms12g -Xmx12g"
