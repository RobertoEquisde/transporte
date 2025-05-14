# Utilizamos una imagen multi-etapa para optimizar el tamaño final

# Etapa de construcción
FROM maven:3.9-eclipse-temurin-21 AS build

# Directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración del proyecto
COPY pom.xml .
COPY src ./src

# Construir la aplicación y omitir tests
RUN mvn clean package -DskipTests

# Etapa final
FROM eclipse-temurin:21-jdk-alpine

# Puerto expuesto por la aplicación
EXPOSE 8080

# Variables de entorno (ajustar según tus necesidades)
ENV SPRING_PROFILES_ACTIVE=prod

# Directorio de trabajo
WORKDIR /app

# Copiar el JAR de la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Comando para ejecutar la aplicación con optimizaciones para contenedores
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

