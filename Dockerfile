# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Configuración agresiva de encoding
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Dsun.jnu.encoding=UTF8"
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

COPY transporte/pom.xml .
RUN mvn dependency:go-offline -B

COPY transporte/src ./src

# Build con filtrado desactivado y encoding forzado
RUN mvn clean package -DskipTests -Dmaven.resources.filtering=false -Dfile.encoding=UTF-8

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]