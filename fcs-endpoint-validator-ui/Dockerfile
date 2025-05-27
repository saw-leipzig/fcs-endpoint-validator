# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /work

# --------------------------------------------------------
# build env

RUN mkdir -p /work/fcs-endpoint-validator-core /work/fcs-endpoint-validator-ui /work/fcs-endpoint-validator-cli
COPY pom.xml /work/
COPY fcs-endpoint-validator-core/pom.xml /work/fcs-endpoint-validator-core/
COPY fcs-endpoint-validator-ui/pom.xml /work/fcs-endpoint-validator-ui/
COPY fcs-endpoint-validator-cli/pom.xml /work/fcs-endpoint-validator-cli/

RUN mvn -B -ntp -Pproduction -pl .,fcs-endpoint-validator-core,fcs-endpoint-validator-ui dependency:resolve-plugins

# --------------------------------------------------------
# build fcs endpoint validator core

RUN mvn -B -ntp -pl fcs-endpoint-validator-core -am dependency:resolve

COPY fcs-endpoint-validator-core /work/fcs-endpoint-validator-core

RUN mvn -B -ntp -pl fcs-endpoint-validator-core -am clean package install

# --------------------------------------------------------
# build fcs endpoint validator ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-ui -am dependency:resolve

COPY fcs-endpoint-validator-ui /work/fcs-endpoint-validator-ui

RUN mvn -B -ntp -Pproduction -pl fcs-endpoint-validator-ui -am clean package

# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS endpoint-validator

COPY --from=builder /work/fcs-endpoint-validator-ui/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
