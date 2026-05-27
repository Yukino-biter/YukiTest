@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN ("%~dp0\.mvn\wrapper\maven-wrapper.properties") DO @(
    IF "%%~A"=="wrapperUrl" SET "__MVNW_CMD__=%%~B"
    IF "%%~A"=="distributionUrl" SET "__MVNW_DISTRIBUTION__=%%~B"
)
@IF "%__MVNW_CMD__%"=="" (
    SET "__MVNW_ERROR__=Cannot read maven-wrapper.properties"
    goto error
)

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@IF NOT EXIST "%~dp0\.mvn\wrapper\maven-wrapper.jar" (
    IF "%MVNW_REPOURL%"=="" SET MVNW_REPOURL=https://repo.maven.apache.org/maven2
    SET "MVNW_DOWNLOAD_LOCATION=%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    IF "%MVNW_VERBOSE%" == "true" (
        echo Couldn't find %~dp0\.mvn\wrapper\maven-wrapper.jar, downloading it ...
        echo Downloading from: %MVNW_DOWNLOAD_LOCATION%
    )
    powershell -Command ^
        "$progressPreference = 'silentlyContinue'; Invoke-WebRequest -Uri '%MVNW_DOWNLOAD_LOCATION%' -OutFile '%~dp0\.mvn\wrapper\maven-wrapper.jar'" 2>nul
    IF "%MVNW_VERBOSE%" == "true" (
        echo Downloaded %~dp0\.mvn\wrapper\maven-wrapper.jar
    )
)
@REM End of extension

@IF NOT EXIST "%~dp0\.mvn\wrapper\maven-wrapper.jar" (
    SET "__MVNW_ERROR__=Cannot download maven-wrapper.jar"
    goto error
)

@SET WRAPPER_JAR="%~dp0\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET JAVA_EXE=java.exe
@IF NOT "%JAVA_HOME%"=="" SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

@"%JAVA_EXE%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
  %WRAPPER_LAUNCHER% %*

:error
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@ECHO %__MVNW_ERROR__% 1>&2
@EXIT /B 1
