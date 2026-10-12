FROM maven:3.9.10-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ENV APP_ENV=prod \
    JAVA_OPTS=""

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
