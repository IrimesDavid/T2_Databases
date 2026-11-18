
# --------------------------------------------------
# Stage 1: Build the application
# --------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Download dependencies first (Docker cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline

# Build the WAR
COPY src ./src
RUN mvn clean package -DskipTests


# --------------------------------------------------
# Stage 2: WildFly runtime
# --------------------------------------------------
FROM quay.io/wildfly/wildfly:41.0.1.Final-jdk21

# Copy JDBC drivers and WildFly configuration script
COPY --chown=jboss:root docker/wildfly/drivers/ /tmp/jdbc/
COPY --chown=jboss:root docker/wildfly/configure-datasources.cli /tmp/

# Configure JDBC drivers and datasources
RUN /opt/jboss/wildfly/bin/jboss-cli.sh \
    --file=/tmp/configure-datasources.cli && \
    rm -rf /tmp/jdbc /tmp/configure-datasources.cli \
           /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
           /opt/jboss/wildfly/standalone/tmp \
           /opt/jboss/wildfly/standalone/data

# Copy the generated WAR into WildFly deployments
COPY --from=build /app/target/*.war \
    /opt/jboss/wildfly/standalone/deployments/

# Expose application port
EXPOSE 8080

# Start WildFly
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]


#NOTES:
#   The CLI script must install the modules before starting the embedded server. It will register the drivers and then create PostgresDS, SqlServerDS, and OracleDS.
#   The passwords will not be hardcoded. We'll use expressions such as ${env.POSTGRES_PASSWORD} in the datasource configuration, with Compose passing the environment variables to WildFly.
#   The cleanup is intentional. WildFly's own container documentation demonstrates removing configuration history, temporary files, and generated data after embedded configuration to prevent startup problems.