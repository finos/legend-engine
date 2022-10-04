#!/bin/bash

# printing tool versions
java --version
mvn --version
gpg --version

# setting env variables
export MAVEN_OPTS="-Xms6g -Xmx6g"

# running the build
mvn -B -e -T 4 install -DargLine="-Xms10g -Xmx10g"
