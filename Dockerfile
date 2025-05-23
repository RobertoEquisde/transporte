FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY transporte/pom.xml .
RUN mvn dependency:go-offline -B

COPY transporte/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
EXPOSE 8080
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]