@echo off
setlocal
set JAVA_CMD=java
if exist "%JAVA_HOME%\bin\java.exe" set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
%JAVA_CMD% -cp "build\lwjgl-classes;lib\*" com.jammerbam.cogvisualizer.LwjglOreVeinVisualizerApp
