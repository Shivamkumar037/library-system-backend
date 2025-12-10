# Stage 1: Build the Application
# Maven image ka use karke app build karein
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
# Package create karein (tests skip karke taki jaldi ho)
RUN mvn clean package -DskipTests

# Stage 2: Run the Application
# Lightweight JDK image use karein run karne ke liye
FROM openjdk:17-jdk-slim
WORKDIR /app

# Build stage se JAR file copy karein
# Note: Check karein ki target folder me aapka jar name yahi ban raha hai ya nahi.
# Usually ye 'artifactId-version.jar' hota hai pom.xml ke hisab se.
COPY --from=build /app/target/library-system-0.0.1-SNAPSHOT.jar app.jar

# Port expose karein
EXPOSE 8080

# App run karein
ENTRYPOINT ["java", "-jar", "app.jar"]