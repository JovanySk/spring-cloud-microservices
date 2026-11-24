# One multi-stage Dockerfile shared by every module.
# Build a specific service with:  docker build --build-arg MODULE=catalog-service -t bakery/catalog-service .

# ---------- 1) build stage: compile only the requested module ----------
FROM eclipse-temurin:17-jdk-jammy AS build
ARG MODULE
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY catalog-service/pom.xml catalog-service/
COPY inventory-service/pom.xml inventory-service/
COPY order-service/pom.xml order-service/
RUN chmod +x mvnw

COPY ${MODULE}/src ${MODULE}/src
RUN ./mvnw -B -q -pl ${MODULE} -am package -DskipTests

# ---------- 2) runtime stage: small JRE image with just the jar ----------
FROM eclipse-temurin:17-jre-jammy
ARG MODULE

# curl is used by the docker-compose healthchecks (GET /actuator/health)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home bakery
USER bakery
WORKDIR /app

COPY --from=build /workspace/${MODULE}/target/${MODULE}.jar app.jar

ENV JAVA_OPTS="-Xmx256m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
