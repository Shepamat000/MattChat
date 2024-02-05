@echo off
javac ChatRoom/Server.java
if %errorlevel% neq 0 (
	echo There was an error; exiting now.	
) else (
	echo Compiled correctly!  Running Server...
	java ChatRoom.Server	
)
pause