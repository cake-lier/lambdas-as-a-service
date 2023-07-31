FROM eclipse-temurin:17.0.8_7-jre

RUN mkdir /opt/app
COPY application.conf /opt/app/
COPY target/scala-3.3.0/main.jar /opt/app/
CMD ["java", "-jar", "/opt/app/main.jar", "/opt/app/application.conf"]
