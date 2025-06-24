# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /project
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /project/target/tt-storage-0.0.1-SNAPSHOT.jar tt-storage.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/tt-storage.jar"]