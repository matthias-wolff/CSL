@echo off
if defined CSL_HOME ( 
  echo CSL_HOME is pre-configured
) else (
  echo CSL_HOME is automatically set
  set CSL_HOME=.
)
set LAB_SERVER=FEHMARN
set PATH=%PATH%
set OPTIONS=--nomouse %*
set MAIN=

set CP=%CSL_HOME%/target/classes/;%CSL_HOME%/target/dependency-jars/*

if /i %COMPUTERNAME%==FARPADD2 ( goto SERVER )
if /i %COMPUTERNAME%==%LAB_SERVER% ( goto SERVER ) else ( goto CLIENT )

:SERVER
  echo CSL Server at %COMPUTERNAME%
  set MAIN=de.tucottbus.kt.csl.CslDemoPanel

  set OPTIONS=%OPTIONS% --musiclib=%USERPROFILE%/Music

  goto START
  
:CLIENT
  echo CSL Client at %COMPUTERNAME%
  set MAIN=de.tucottbus.kt.csl.CognitiveSystemsLab

  if /i %COMPUTERNAME%==KORFU set OPTIONS=%OPTIONS% --panel=de.tucottbus.kt.lcars.al.AudioLibraryPanel
  if /i %COMPUTERNAME%==TINOS set OPTIONS=%OPTIONS% --panel=de.tucottbus.kt.csl.lcars.MicrophoneArrayPanel
  set OPTIONS=%OPTIONS% --clientof=%LAB_SERVER%
  
:START
echo CSL_HOME: %CSL_HOME%
echo MAIN    : %MAIN%
echo OPTIONS : %OPTIONS%

java -cp "%CP%" %MAIN% %OPTIONS%