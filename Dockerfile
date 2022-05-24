FROM openjdk/java:17-alpine

COPY build/libs/*.jar app.jar
