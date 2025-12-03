# Multi-stage build for the legacy Spring Boot backend

FROM maven:3.9-eclipse-temurin-17 AS base
WORKDIR /workspace
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=25.0"
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline

FROM base AS dev
COPY src ./src
CMD ["mvn","spring-boot:run"]

FROM base AS build
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=25.0"
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/app/app.jar"]
