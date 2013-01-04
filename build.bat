@echo off
cmd /c ant.bat clean-and-fat-jar
cmd /c launch4jc.exe tools\launch4j\configuration.xml
