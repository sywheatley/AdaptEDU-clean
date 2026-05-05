FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY SpringBootTest/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline

COPY SpringBootTest/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system adaptedu && useradd --system --gid adaptedu adaptedu

COPY --from=build /workspace/target/*.jar /app/app.jar

USER adaptedu

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
