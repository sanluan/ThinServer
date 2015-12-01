@echo off
call setclasspath.bat
%EXECUTABLE% -classpath "%CLASSPATH%" com.sanluan.server.ThinHttpServer>%cd%/out.log