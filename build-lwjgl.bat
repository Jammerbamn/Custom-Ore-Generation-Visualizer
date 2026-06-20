@echo off
setlocal
set SRC=src\main\java\com\example\cogveins
set OUT=build\lwjgl-classes
if not exist "%OUT%" mkdir "%OUT%"
javac -source 1.8 -target 1.8 -cp "lib\*" -d "%OUT%" ^
  "%SRC%\Vec3.java" ^
  "%SRC%\PDist.java" ^
  "%SRC%\OreBlockDefinition.java" ^
  "%SRC%\BiomeGateEntry.java" ^
  "%SRC%\VeinDefinition.java" ^
  "%SRC%\OreVolume.java" ^
  "%SRC%\VeinGenerationReport.java" ^
  "%SRC%\OreVeinGenerator.java" ^
  "%SRC%\CogXmlLoader.java" ^
  "%SRC%\DefinitionXmlWriter.java" ^
  "%SRC%\LwjglOreVeinVisualizerApp.java"
if errorlevel 1 exit /b %errorlevel%
echo LWJGL build complete. Run with run-lwjgl.bat
