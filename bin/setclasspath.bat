cd ..
set "THINSERVER_HOME=%cd%"
set EXECUTABLE="%JAVA_HOME%\bin\java.exe"
for %%a in ("%cd%\lib\*.jar") do call :addcp %%a
goto exec
:addcp
SET CLASSPATH=%CLASSPATH%;%1
goto :eof
:exec
echo CLASSPATH:%CLASSPATH%