FROM maven:3.6.3-jdk-11 AS build

ADD pom.xml /pom.xml
ADD src /src

RUN mvn clean install

FROM openjdk:11.0.10-jre

COPY --from=build target/smart-service-registration.jar /smart-service-registration.jar

ENV TZ="Europe/Amsterdam"

EXPOSE 8080

ENTRYPOINT [ "sh", "-c", "java -jar /smart-service-registration.jar" ]
