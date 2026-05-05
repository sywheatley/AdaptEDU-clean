# Use Java
FROM openjdk:17-jdk-slim

# Copy jar
COPY target/*.jar app.jar

# Run app
ENTRYPOINT ["java","-jar","/app.jar"]