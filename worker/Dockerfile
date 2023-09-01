FROM eclipse-temurin:19.0.2_7-jre

ENV WORKER_ID=00000000-0000-0000-0000-000000000000
ENV WORKER_TS_URI=ws://localhost:8080/tuplespace
ENV WORKER_HTTP_CLIENT_URI=http://localhost:8081/service/files
ENV WORKER_ACCEPTED_EXECUTABLES=Java;Python
ENV WORKER_AVAILABLE_SLOTS=100

RUN mkdir /opt/app
COPY target/scala-3.3.0/main.jar /opt/app/
CMD ["java", "-jar", "/opt/app/main.jar"]
