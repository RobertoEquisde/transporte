# Etapa de construcción
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar pom.xml primero (para cache de dependencias)
COPY transporte/pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY transporte/src ./src

# Construir con encoding explícito y más memoria
RUN mvn clean package -DskipTests \
    -Dfile.encoding=UTF-8 \
    -Dproject.build.sourceEncoding=UTF-8

# Etapa final
FROM eclipse-temurin:21-jdk-alpine
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app

# Copiar el JAR
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", \
"-XX:+UseContainerSupport", \
"-XX:MaxRAMPercentage=75.0", \
"-Dfile.encoding=UTF-8", \
"-Djava.security.egd=file:/dev/./urandom", \
"-jar", "app.jar"]