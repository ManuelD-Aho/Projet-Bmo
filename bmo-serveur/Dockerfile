FROM openjdk:17-jdk-slim

ARG JAR_FILE=bmo-serveur/target/bmo-serveur-*.jar
ARG APP_PORT=5000

WORKDIR /opt/bmo

COPY ${JAR_FILE} app-serveur.jar

EXPOSE ${APP_PORT}

# Commande pour exécuter l'application serveur
ENTRYPOINT ["java", "-jar", "app-serveur.jar"]

# Ce Dockerfile construit l'image pour le serveur BMO.
# Il utilise une image de base OpenJDK 17 légère.
# Le JAR de l'application serveur est copié dans l'image.
# Le port applicatif est exposé, configurable via l'argument APP_PORT.