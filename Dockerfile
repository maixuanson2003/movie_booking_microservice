# Build from one Maven Spring Boot service directory (pom.xml + src/).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests && \
    find target -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' > /tmp/jars && \
    test "$(wc -l < /tmp/jars)" -eq 1 && \
    cp "$(cat /tmp/jars)" /build/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
RUN mkdir -p /app/data && chown spring:spring /app/data
COPY --from=build --chown=spring:spring /build/app.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
