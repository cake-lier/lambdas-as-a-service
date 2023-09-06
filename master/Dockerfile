FROM eclipse-temurin:19.0.2_7-jre

RUN mkdir /opt/app
COPY target/scala-3.3.0/main.jar /opt/app/
CMD ["java", "-jar", "/opt/app/main.jar"]
