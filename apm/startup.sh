#!/bin/bash

# Set default APM_RESOLUTION if not provided
APM_RESOLUTION=${APM_RESOLUTION:-120}

# Append JavaMelody configuration to JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS -Djavamelody.resolution-seconds=$APM_RESOLUTION"

# Start Tomcat
exec catalina.sh run