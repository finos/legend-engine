#!/bin/bash

# Initialize variables
debug=""
profile="default"

# Function to show usage
show_usage() {
    echo "Usage: $0 [-d port_number] [-p {default|relational|datacube}] [-h]"
    echo "  -d port_number     Enable debug mode with a specified port number."
    echo "  -p {default|relational|datacube}  Specify the profile to use."
    echo "  -h                 Show this help message and exit."
    exit 0
}

# Parse options
while getopts "d:p:h" opt; do
    case $opt in
        d)
            if ! [[ "$OPTARG" =~ ^[0-9]+$ ]]; then
                echo "Invalid port number for -d. Port number must be an integer."
                show_usage
            fi
            debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$OPTARG"
            ;;
        p)
            if [[ "$OPTARG" != "default" && "$OPTARG" != "relational" && "$OPTARG" != "datacube" ]]; then
                echo "Invalid value for -p. Valid options are: default, relational, datacube"
                show_usage
            fi
            profile="$OPTARG"
            ;;
        h)
            show_usage
            ;;
        *)
            show_usage
            ;;
    esac
done

classpathFile=$(dirname "$0")/$profile-classpath.txt
replMainClassFile=$(dirname "$0")/$profile-replMainClass.txt

if [ ! -f "$classpathFile" ]; then
    echo "Computing $profile repl classpath"
    mvn -q -DforceStdout -P "$profile" dependency:build-classpath -Dmdep.outputFile="$classpathFile"
fi

if [ ! -f "$replMainClassFile" ]; then
  echo "Computing $profile repl main class"
  mvn -q -DforceStdout help:evaluate -P "$profile" -Dexpression=replMainClass -q -DforceStdout --log-file "$replMainClassFile"
fi

java -cp @$classpathFile $debug @$replMainClassFile