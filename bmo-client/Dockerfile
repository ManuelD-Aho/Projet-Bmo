# Étape 1 : Construction de l'application client avec Maven
# Le contexte de build pour cette image est toujours le dossier 'bmo-client'
# MAIS nous allons copier les éléments nécessaires du projet parent et commun.
FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_client

# Créer une structure de projet à l'intérieur du conteneur de build
WORKDIR /app_projet_bmo_complet

# 1. Copier le POM parent (depuis la racine du projet BMO sur l'hôte)
#    Puisque le contexte Docker est bmo-client, nous devons remonter d'un niveau.
COPY ../pom.xml ./pom.xml

# 2. Copier le module bmo-commun (sources et pom)
COPY ../bmo-commun ./bmo-commun

# 3. Copier le module bmo-client lui-même (sources et pom du contexte actuel)
COPY pom.xml ./bmo-client/pom.xml
COPY src ./bmo-client/src

# 4. Exécuter la commande Maven depuis la racine de la structure copiée (/app_projet_bmo_complet)
#    Construire bmo-client et ses dépendances (comme bmo-commun)
RUN mvn -f /app_projet_bmo_complet/pom.xml clean install -DskipTests -pl akandan.bahou.kassy:bmo-client -am

# Étape 2 : Création de l'image d'exécution finale pour le client JavaFX
FROM openjdk:17-jdk-slim

WORKDIR /app_client

# Copier le JAR exécutable du client construit à l'étape précédente
# Le JAR se trouvera dans /app_projet_bmo_complet/bmo-client/target/
COPY --from=constructeur_bmo_client /app_projet_bmo_complet/bmo-client/target/bmo-client-*.jar app-bmo-client.jar

# Dépendances GUI Linux (à décommenter et adapter si besoin)
# RUN apt-get update && apt-get install -y --no-install-recommends \
#     libgtk-3-0 libxrender1 libxtst6 libxi6 libasound2 libgl1-mesa-glx \
#  && rm -rf /var/lib/apt/lists/*

CMD ["java", "-jar", "app-bmo-client.jar"]

LABEL maintainer="ManuelD-Aho"
LABEL version="1.0"
LABEL description="Image Docker pour l'application client BMO (JavaFX)."