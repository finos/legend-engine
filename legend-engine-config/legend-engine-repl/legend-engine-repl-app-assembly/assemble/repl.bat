@echo off
setlocal EnableDelayedExpansion

:: Initialize variables
set "debug="
set "profile=default"

:parse_options
if "%~1"=="-d" (
    shift
    if "%~2"=="" (
        echo Error: Missing port number after -d option.
        goto show_usage
    )
    SET "var="&for /f "delims=0123456789" %%i in ("%~2") do set var=%%i
    if defined var (
        echo Error: Invalid port number "%~2". It must be an integer.
        goto show_usage
    )
    set "debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:%~2"
    shift
    goto parse_options
)

if "%~1"=="-p" (
    shift
    if "%~2"=="" (
        echo Error: Missing profile value after -p option.
        goto show_usage
    )
    if /i "%~2"=="default" set "profile=default" & shift & goto parse_options
    if /i "%~2"=="relational" set "profile=relational" & shift & goto parse_options
    if /i "%~2"=="datacube" set "profile=datacube" & shift & goto parse_options
    echo Error: Invalid profile "%~2". Valid options are: default, relational, datacube.
    goto show_usage
)

if "%~1"=="-h" goto show_usage

if "%~1"=="" goto endparse

echo Error: Invalid option "%~1".
goto show_usage

:endparse

set "classpathFile=%~dp0%profile%-win-classpath.txt"
set "replMainClassFile=%~dp0%profile%-win-replMainClass.txt"

if not exist "%classpathFile%" (
    echo Computing %profile% classpath...
    call mvn -q -DforceStdout -P %profile% dependency:build-classpath -Dmdep.outputFile="%classpathFile%"
)

if not exist "%replMainClassFile%" (
    echo Computing %profile% REPL main class...
    call mvn -q -DforceStdout help:evaluate -P %profile% -Dexpression=replMainClass -q -DforceStdout --log-file %replMainClassFile%
)

java -cp @%classpathFile% %debug% @%replMainClassFile%
exit /b 0

:show_usage
echo Usage: %~nx0 [-d port_number] [-p {default^|relational^|datacube}] [-h]
echo   -d port_number     Enable debug mode with a specified port number.
echo   -p {default^|relational^|datacube}  Specify the profile to use.
echo   -h                 Show this help message and exit.
exit /b 1
