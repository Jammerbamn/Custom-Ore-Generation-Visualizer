@echo off
setlocal
set SRC=src\main\java\com\jammerbam\cogvisualizer
set OUT=build\classes
if not exist "%OUT%" mkdir "%OUT%"
javac -source 1.8 -target 1.8 -d "%OUT%" ^
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
  "%SRC%\GeneratorSmokeTest.java"
if errorlevel 1 exit /b %errorlevel%
java -cp "%OUT%" com.jammerbam.cogvisualizer.GeneratorSmokeTest
