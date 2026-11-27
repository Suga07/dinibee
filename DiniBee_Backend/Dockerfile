FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /source
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# Runs unit tests during the image build. PostgreSQL integration tests run separately.
RUN mvn -B -ntp clean package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 10001 bookeat && useradd --system --uid 10001 --gid 10001 bookeat
WORKDIR /app
RUN mkdir -p /app/private-documents && chown 10001:10001 /app/private-documents
COPY --from=build /source/target/bookeat-backend-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
