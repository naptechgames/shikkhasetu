# ShikkhaSetu backend image for a cloud host (Render).
# Stage 1 builds the jar from source; stage 2 runs it on a small Java runtime.

FROM gradle:8.14.3-jdk17 AS build
WORKDIR /src
COPY build.gradle settings.gradle ./
COPY src/main src/main
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/libs/shikkhasetu-backend-1.0.0.jar app.jar

# The host tells the server which port to use through the PORT variable
# (see server.port in application.properties). The database address, user,
# password and the coordinator account come from environment variables set on
# the host - they are NOT stored in this repository.
CMD ["java", "-Xmx300m", "-jar", "app.jar"]
