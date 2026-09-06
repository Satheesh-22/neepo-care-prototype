FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY backend/src src
COPY contracts/generated/java /workspace/contracts/generated/java
COPY care-engine/src/main/java /workspace/care-engine/src/main/java
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S neepo && adduser -S neepo -G neepo
WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar app.jar
USER neepo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
