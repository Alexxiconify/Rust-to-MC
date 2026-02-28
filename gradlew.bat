@rem Gradle start up script for Windows
@rem Uses the system Gradle installation (9.2.0 daemon already running via VS Code plugin)
@echo off
setlocal

set GRADLE_EXE=
for %%i in (gradle.bat gradle) do (
    if exist "%%~$PATH:i" set GRADLE_EXE=%%~$PATH:i
)

if not defined GRADLE_EXE (
    echo ERROR: gradle not found in PATH.
    echo Please run this build task from the VS Code Gradle extension instead.
    exit /b 1
)

%GRADLE_EXE% %*
endlocal
