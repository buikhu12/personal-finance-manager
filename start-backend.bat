@echo off
REM Script khởi động backend Spring Boot với đúng JDK 25
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
echo [INFO] Dang su dung Java:
java -version
echo.
echo [INFO] Khoi dong Personal Finance Manager Backend...
mvn spring-boot:run
