# Multi-stage build for Spring Boot backend

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# Build without tests to speed up container build
RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
# Let Java size itself sanely within container limits
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=25.0"
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/app/app.jar"]
