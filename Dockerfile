FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :error-monitor-server:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/error-monitor-server/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
