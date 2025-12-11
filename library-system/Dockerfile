# Stage 1: Build the application
# Use Java 21 to match your pom.xml configuration
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
# Clean package runs to generate the jar
RUN mvn clean package -DskipTests

# Stage 2: Run the application
# Use Java 21 JRE for running
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 👇 CRITICAL FIX:
# 1. Hum build stage se sahi file copy kar rahe hain (1.0.0-ENTERPRISE)
# 2. Hum use rename karke 'app.jar' bana rahe hain taaki naam ka confusion khatam ho jaye
COPY --from=build /app/target/library-system-1.0.0-ENTERPRISE.jar app.jar

# Render specific port handling
ENV PORT=8080
EXPOSE 8080

# Ab command hamesha 'app.jar' chalayega, chahe version kuch bhi ho
ENTRYPOINT ["java", "-jar", "app.jar"]
