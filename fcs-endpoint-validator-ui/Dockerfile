# ---------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /work

# --------------------------------------------------------
# build env

RUN mkdir -p /work/fcs-endpoint-validator-core /work/fcs-endpoint-validator-ui
COPY pom.xml /work/
COPY fcs-endpoint-validator-core/pom.xml /work/fcs-endpoint-validator-core/
COPY fcs-endpoint-validator-ui/pom.xml /work/fcs-endpoint-validator-ui/

RUN mvn -B -ntp -Pproduction dependency:resolve-plugins

# --------------------------------------------------------
# build fcs endpoint validator core

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-core -am dependency:resolve

COPY fcs-endpoint-validator-core /work/fcs-endpoint-validator-core

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-core -am clean package install

# --------------------------------------------------------
# build fcs endpoint validator ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-ui -am dependency:resolve

COPY fcs-endpoint-validator-ui /work/fcs-endpoint-validator-ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-ui -am clean package

# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS endpoint-validator

COPY --from=builder /work/fcs-endpoint-validator-ui/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
