FROM openjdk:17-jdk-slim

ARG JAR_FILE=bmo-client/target/bmo-client-*.jar

# Installation des dépendances graphiques pour JavaFX sur Debian/Ubuntu
# Ces bibliothèques sont nécessaires pour l'affichage des interfaces graphiques.
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libxtst6 \
    libxrender1 \
    libasound2 \
    libgl1-mesa-glx \
    libgl1-mesa-dri \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/bmo

COPY ${JAR_FILE} app-client.jar

# Commande pour exécuter l'application cliente JavaFX
ENTRYPOINT ["java", "-jar", "app-client.jar"]

# Ce Dockerfile construit l'image pour le client JavaFX BMO.
# Il installe les bibliothèques graphiques Linux nécessaires.
# L'affichage nécessitera une configuration de X11 forwarding sur l'hôte.