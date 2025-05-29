# Étape 1 : Construction de l'application client avec Maven
FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_client

WORKDIR /app_source_client
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn package -DskipTests

# Étape 2 : Création de l'image d'exécution finale pour le client JavaFX
FROM openjdk:17-jdk-slim

WORKDIR /app_client

# Copier le JAR exécutable du client
COPY --from=constructeur_bmo_client /app_source_client/target/bmo-client-*.jar app-bmo-client.jar

# Dépendances pour les applications GUI sous Linux (exemple pour Debian/Ubuntu based)
# RUN apt-get update && apt-get install -y --no-install-recommends \
#     libgtk-3-0 \
#     libxrender1 \
#     libxtst6 \
#     libxi6 \
#  && rm -rf /var/lib/apt/lists/*

# Commande pour démarrer l'application client
CMD ["java", "-jar", "app-bmo-client.jar"]

LABEL maintainer="ManuelD-Aho"
LABEL version="1.0"
LABEL description="Image Docker pour l'application client BMO (JavaFX)."