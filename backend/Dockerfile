# nombre: Dockerfile (backend)
# descripcion: Imagen multi-etapa: compila con Maven y ejecuta con JRE 21.
# fecha_creacion: 2026-10-07
# actualizacion: 2026-10-07
# autor: Camilo1408
# version: 1.0.0
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/inventario-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
