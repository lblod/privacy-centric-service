FROM maven:3.8.4-openjdk-17 as builder
LABEL maintainer="info@redpencil.io"

WORKDIR /app

COPY pom.xml .

COPY .mvn .mvn

COPY settings.xml settings.xml

RUN mvn -B dependency:resolve-plugins dependency:resolve

COPY ./src ./src

RUN mvn package -DskipTests

FROM eclipse-temurin:17.0.2_8-jre

WORKDIR /app

COPY --from=builder /app/target/privacy-centric-service.jar ./app.jar

ENTRYPOINT ["sh", "-c", "java  -Dlog4j2.formatMsgNoLookups=true ${JAVA_OPTS} -jar /app/app.jar"]
