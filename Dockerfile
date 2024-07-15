FROM openjdk:19
WORKDIR /app
COPY target/SearchEngine-1.0-SNAPSHOT.jar searchengine.jar
CMD ["java", "-jar", "searchengine.jar"]