FROM maven:3-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY . .
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /build/target/quarkus-app/ /app/

EXPOSE 8080
CMD ["java", "-jar", "quarkus-run.jar"]
