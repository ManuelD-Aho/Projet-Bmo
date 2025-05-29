# Étape 1 : Construction de l'application avec Maven
FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_serveur

# Définir le répertoire de travail pour l'ensemble du build multi-module
WORKDIR /app_projet_bmo

# Copier le pom.xml parent (depuis la racine du contexte de build)
COPY pom.xml .

# Copier le module bmo-commun
COPY bmo-commun ./bmo-commun

# Copier le module bmo-serveur
COPY bmo-serveur ./bmo-serveur

# Copier le module bmo-client (pour que Maven ne se plaigne pas du module manquant)
COPY bmo-client ./bmo-client

# Exécuter la commande Maven depuis la racine du projet copié,
# en ciblant spécifiquement le module bmo-serveur et ses dépendances (-am)
RUN mvn -pl bmo-serveur -am package -DskipTests

# Étape 2 : Création de l'image d'exécution finale
FROM openjdk:17-slim

# Définir le répertoire de travail dans le conteneur d'exécution
WORKDIR /app_serveur

# Copier le JAR exécutable construit à l'étape précédente
# Le JAR se trouvera dans bmo-serveur/target par rapport à /app_projet_bmo
COPY --from=constructeur_bmo_serveur /app_projet_bmo/bmo-serveur/target/bmo-serveur-*.jar app-bmo-serveur.jar

# Exposer le port sur lequel l'application serveur écoute
ARG BMO_SERVER_PORT_ARG=5000
ENV BMO_SERVEUR_PORT=${BMO_SERVER_PORT_ARG}
EXPOSE ${BMO_SERVEUR_PORT}

# Commande pour démarrer l'application serveur
CMD ["java", "-jar", "app-bmo-serveur.jar"]

# Ajouter une étiquette pour identifier l'image
LABEL maintainer="ManuelD-Aho"
LABEL version="1.0"
LABEL description="Image Docker pour l'application serveur BMO."