#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.3.2
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; others: silence the output
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVA_HOME to default if not set
if [ -z "${JAVA_HOME-}" ]; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home)" || :
  fi
fi

# Determine the Java command to use to start the JVM.
if [ -n "${JAVA_HOME-}" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ]; then
    echo "The JAVA_HOME environment variable is not defined correctly, so Apache Maven Wrapper cannot run." >&2
    echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" does not exist." >&2
    exit 1
  fi
else
  JAVACMD="$(\unset -f command 2>/dev/null; \command -v java)" || :
  if [ ! -x "${JAVACMD-}" ]; then
    echo "The java binary is not found in PATH, and JAVA_HOME environment variable is not defined, so Apache Maven Wrapper cannot run." >&2
    exit 1
  fi
fi

# hash string like  "3.8.6" to a file
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf %d "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf %x\\n $h
}

verbose() { [ "${MVNW_VERBOSE-}" != true ] || printf %s\\n "$1"; }

repo_url() {
  if [ -n "${MVNW_REPOURL-}" ]; then
    printf %s "${MVNW_REPOURL}"
  else
    printf %s "https://repo.maven.apache.org/maven2"
  fi
}

wrapper_url() {
  if [ -n "${MVNW_REPOURL-}" ]; then
    printf %s "${MVNW_REPOURL}/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  else
    printf %s "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi
}

while [ -e .mvn ]; do
  MVN_HOME="$(pwd -P)"
  break
done

# read distributionUrl and wrapperUrl from properties
distributionUrl=
wrapperUrl=
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl="${value-}" ;;
  wrapperUrl) wrapperUrl="${value-}" ;;
  esac
done <"${MVN_HOME:-.}/.mvn/wrapper/maven-wrapper.properties"

[ -n "$distributionUrl" ] || {
  echo "Cannot read distributionUrl property in ${MVN_HOME:-.}/.mvn/wrapper/maven-wrapper.properties" >&2
  exit 1
}

# determine maven version
maven_version() {
  if [ -f "${MVN_HOME:-.}/.mvn/wrapper/maven-wrapper.properties" ]; then
    grep 'distributionUrl' "${MVN_HOME:-.}/.mvn/wrapper/maven-wrapper.properties" | grep -oE '[^/]+(?=-bin\.zip$)' | sed 's/apache-maven-//' | sed 's/-bin//'
  fi
}

# prepare distribution dir
if [ -z "${MVN_HOME-}" ]; then
  MVN_HOME="$(cd "$(dirname "$0")" && pwd -P)"
fi
MVNW_HOME="${MVN_HOME}/.mvn/wrapper"

MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"
DIST_DIR="${MAVEN_USER_HOME}/wrapper/dists/apache-maven-$(maven_version)"

# check if distribution exists
if [ -d "$DIST_DIR" ] && [ -f "$DIST_DIR/bin/mvn" ]; then
  verbose "Found distribution in $DIST_DIR"
else
  # download distribution
  mkdir -p "$DIST_DIR"
  DIST_ZIP="$DIST_DIR/apache-maven-$(maven_version)-bin.zip"
  if [ ! -f "$DIST_ZIP" ]; then
    verbose "Downloading distribution from $distributionUrl"
    if command -v curl > /dev/null 2>&1; then
      curl -fL "$distributionUrl" -o "$DIST_ZIP" || {
        echo "Failed to download Maven distribution" >&2
        exit 1
      }
    elif command -v wget > /dev/null 2>&1; then
      wget -q "$distributionUrl" -O "$DIST_ZIP" || {
        echo "Failed to download Maven distribution" >&2
        exit 1
      }
    else
      echo "Cannot find curl or wget to download Maven distribution" >&2
      exit 1
    fi
  fi
  # unzip
  verbose "Extracting distribution to $DIST_DIR"
  if command -v unzip > /dev/null 2>&1; then
    unzip -qo "$DIST_ZIP" -d "$DIST_DIR" || {
      echo "Failed to extract Maven distribution" >&2
      exit 1
    }
  else
    echo "Cannot find unzip to extract Maven distribution" >&2
    exit 1
  fi
fi

MVN_CMD="$DIST_DIR/bin/mvn"

# check wrapper jar
WRAPPER_JAR="$MVNW_HOME/maven-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  verbose "Downloading wrapper jar from $(wrapper_url)"
  mkdir -p "$MVNW_HOME"
  if command -v curl > /dev/null 2>&1; then
    curl -fL "$(wrapper_url)" -o "$WRAPPER_JAR" || {
      echo "Failed to download wrapper jar" >&2
      exit 1
    }
  elif command -v wget > /dev/null 2>&1; then
    wget -q "$(wrapper_url)" -O "$WRAPPER_JAR" || {
      echo "Failed to download wrapper jar" >&2
      exit 1
    }
  fi
fi

# run maven
exec "$JAVACMD" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=${MVN_HOME}" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
