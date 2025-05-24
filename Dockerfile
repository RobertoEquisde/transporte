# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Configurar entorno UTF-8 antes de cualquier operación
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF-8

COPY transporte/pom.xml .
RUN mvn dependency:go-offline -B

COPY transporte/src ./src

# Build con parámetros de encoding forzados
RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8 -Dmaven.resources.filtering=true

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk-alpine
EXPOSE 8080
WORKDIR /app

# Configurar encoding para la etapa de runtime
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]