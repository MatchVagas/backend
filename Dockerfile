# ESTÁGIO 1 — BUILD (sem alteração)
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ESTÁGIO 2 — RUNTIME (sem alteração)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S matchvagas && adduser -S matchvagas -G matchvagas

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown matchvagas:matchvagas app.jar

USER matchvagas

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]