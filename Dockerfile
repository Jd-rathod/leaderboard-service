# === Stage 1: Build JAR using Maven ===
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better Docker cache use)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# === Stage 2: Run app with lightweight JDK ===
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the jar from build stage
COPY --from=build /app/target/leaderboard-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]