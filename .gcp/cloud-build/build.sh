#!/bin/bash

# printing tool versions
java --version
mvn --version
gpg --version

# setting env variables
export MAVEN_OPTS="-Xms14g -Xmx14g"

# running the build
mvn -B -e -T 4 install
