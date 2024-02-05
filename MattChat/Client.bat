@echo off
javac ChatRoom/Client.java
if %errorlevel% neq 0 (
	echo There was an error; exiting now.	
) else (
	echo Compiled correctly!  Running Server...
	java ChatRoom.Client	
)
pause