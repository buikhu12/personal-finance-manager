# Stage 1: Build JAR using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Directory for persistent database storage
RUN mkdir -p /app/data

# Copy built JAR from builder stage
COPY --from=build /app/target/finance-manager-*.jar app.jar

EXPOSE 8080

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
