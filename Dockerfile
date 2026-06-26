FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /data/incoming /data/processed /data/error /app/templates /app/images /app/fonts
COPY --from=build /workspace/target/report-generator-*.jar /app/app.jar
COPY src/main/resources/templates/ /app/templates/
COPY src/main/resources/images/ /app/images/
COPY src/main/resources/fonts/ /app/fonts/
ENTRYPOINT ["java","-jar","/app/app.jar"]
