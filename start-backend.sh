#!/usr/bin/env bash
# Script khởi động backend Spring Boot với đúng JDK 25
export JAVA_HOME="C:/Program Files/Java/jdk-25.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[INFO] Đang sử dụng Java:"
java -version
echo ""
echo "[INFO] Khởi động Personal Finance Manager Backend..."
mvn spring-boot:run
