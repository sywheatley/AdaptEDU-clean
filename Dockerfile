## Dockerfile for AdaptEDU
##
# Multi-stage build:
#  1) Build stage uses an official Maven image to download dependencies
#     and compile the Spring Boot JAR.
#  2) Runtime stage uses a slim JRE image to run the produced JAR.
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy only the pom first to take advantage of Docker layer caching for deps
COPY SpringBootTest/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline

# Copy sources and build
COPY SpringBootTest/src ./src
RUN mvn -B package -DskipTests

## Runtime image: smaller base JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a non-root user for running the app
RUN groupadd --system adaptedu && useradd --system --gid adaptedu adaptedu

# Copy the packaged JAR from the build stage
COPY --from=build /workspace/target/*.jar /app/app.jar

# Switch to the non-root user for safer runtime
USER adaptedu

# Expose application port (match Spring Boot server.port)
EXPOSE 8080

# Default entrypoint to run the Spring Boot JAR
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
