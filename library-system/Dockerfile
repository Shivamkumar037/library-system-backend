# Stage 1: Build the Application
# Lightweight JDK image use karein compile aur package karne ke liye
FROM maven:3.8.5-openjdk-17 AS build

# WORKDIR set karein. Build context 'library-system' folder se hai (Render rootDir ki wajah se).
WORKDIR /app 

# Saare files (pom.xml, src/) ko /app ke andar copy karein
COPY . .

# Maven build karein
# Note: Final JAR file banega: /app/target/library-system-0.0.1-SNAPSHOT.jar
RUN mvn clean package -DskipTests

# Stage 2: Minimal Runtime Environment (FIXED)
# Ab hum openjdk:17-jre use kar rahe hain jo available hai.
FROM openjdk:17-jre
WORKDIR /app

# Stage 1 se final JAR file copy karein. JAR ka naam pom.xml ke hisaab se hona chahiye.
# Ye naam assume karta hai ki pom.xml ka <artifactId> 'library-system' hai.
COPY --from=build /app/target/library-system-0.0.1-SNAPSHOT.jar app.jar

# Port 8080 expose karein
EXPOSE 8080

# Application run karein
ENTRYPOINT ["java", "-jar", "app.jar"]
