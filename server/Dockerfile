FROM eclipse-temurin:21.0.7_6-jre

RUN mkdir /opt/app
COPY target/scala-3.3.1/main.jar /opt/app/
CMD ["java", "-jar", "/opt/app/main.jar"]
