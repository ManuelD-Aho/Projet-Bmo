FROM openjdk:17-jre-slim

# Installation des dépendances pour JavaFX
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libglu1-mesa \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY bmo-client/target/bmo-client-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
