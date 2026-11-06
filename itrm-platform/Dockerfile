FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone
COPY target/itrm-platform.jar /app/itrm-platform.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/itrm-platform.jar"]
