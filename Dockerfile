FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.03.09.0820Z

COPY build/libs/*.jar app.jar

CMD ["-jar", "app.jar"]
