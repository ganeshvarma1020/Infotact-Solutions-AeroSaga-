# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Download dependencies first (cached while pom.xml is unchanged)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run in CI and on your laptop; skipping here keeps image builds fast
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
# Never run the app as root
RUN useradd --system --uid 10001 aerosaga
WORKDIR /app
COPY --from=build /src/target/aerosaga.jar app.jar
USER aerosaga
EXPOSE 8080
# Use up to 70% of the container's memory for the Java heap
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
