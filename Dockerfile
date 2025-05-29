# Use an official Maven image to build the app, with OpenJDK 17
FROM maven:3.8.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Use an official OpenJDK runtime
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copy wait-for-it script
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Copy the JAR file from the build stage
COPY --from=build /app/target/DiagnosticRuntime-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Use wait-for-it to wait for mongo
ENTRYPOINT ["/wait-for-it.sh", "mongo:27017", "--timeout=60", "--", "java", "-jar", "app.jar"]
