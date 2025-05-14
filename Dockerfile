# Etapa de construcción
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY transporte/pom.xml .
COPY transporte/src ./src

RUN mvn clean package -DskipTests

# Etapa final
FROM eclipse-temurin:21-jdk-alpine

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

