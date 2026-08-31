FROM eclipse-temurin:11-jre-alpine

LABEL maintainer="TNS-MES Team"
LABEL description="TNS-MES Server - Tunisia MES Application"

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/tns-mes-server.jar app.jar

# Expose port
EXPOSE 8080

# JVM options for containerized environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q -O - http://localhost:8080/tns-mes/actuator/health | grep -q UP || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
