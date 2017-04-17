@echo off
call setclasspath.bat
%EXECUTABLE% -classpath "%CLASSPATH%" com.sanluan.server.ThinHttpServer>%cd%/logs/out.log