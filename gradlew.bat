@rem Gradle startup script for Windows

@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
    set JAVACMD=java.exe
)

if not exist "%JAVACMD%" (
    if not "%JAVA_HOME%"=="" (
        echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
    ) else (
        echo ERROR: JAVA_HOME is not set and no 'java' command could be found. >&2
    )
    exit /b 1
)

"%JAVACMD%" %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal
