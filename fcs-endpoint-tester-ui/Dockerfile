# ---------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /work

# --------------------------------------------------------
# build env

RUN mkdir -p /work/fcs-endpoint-tester-junit /work/fcs-endpoint-tester-ui
COPY pom.xml /work/
COPY fcs-endpoint-tester-junit/pom.xml /work/fcs-endpoint-tester-junit/
COPY fcs-endpoint-tester-ui/pom.xml /work/fcs-endpoint-tester-ui/

RUN mvn -B -ntp -Pproduction dependency:resolve-plugins

# --------------------------------------------------------
# build fcs endpoint tester core

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-tester-junit -am dependency:resolve

COPY fcs-endpoint-tester-junit /work/fcs-endpoint-tester-junit

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-tester-junit -am clean package install

# --------------------------------------------------------
# build fcs endpoint tester ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-tester-ui -am dependency:resolve

COPY fcs-endpoint-tester-ui /work/fcs-endpoint-tester-ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-tester-ui -am clean package

# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS endpoint-tester

COPY --from=builder /work/fcs-endpoint-tester-ui/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
