FROM eclipse-temurin:19.0.2_7-jre

ENV TUPLES_SPACE_PORT_NUMBER=80
ENV TUPLES_SPACE_SERVICE_PATH=tuplespace

RUN mkdir /opt/app
COPY target/scala-3.3.0/main.jar /opt/app/
EXPOSE $TUPLES_SPACE_PORT_NUMBER
CMD ["java", "-jar", "/opt/app/main.jar"]
