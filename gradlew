#!/bin/sh
# Gradle wrapper startup script (text). The gradle-wrapper.jar is generated
# by Android Studio on first project sync, or by running `gradle wrapper`.
DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIR"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
