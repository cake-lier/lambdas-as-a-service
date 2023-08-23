FROM eclipse-temurin:19.0.2_7-jre

ENV MASTER_TS_URI=ws://localhost:8080/tuplespace
ENV MASTER_PORT_NUMBER=8081
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=postgres
ENV DB_NAME=laas
ENV DB_PORT_NUMBER=8082
ENV DB_HOSTNAME=localhost
ENV DB_TIMEOUT=30000

RUN mkdir /opt/app
COPY target/scala-3.3.0/main.jar /opt/app/
CMD ["java", "-jar", "/opt/app/main.jar"]
