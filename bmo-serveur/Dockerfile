FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_serveur
WORKDIR /app_projet_bmo

COPY pom.xml ./pom.xml
COPY bmo-commun ./bmo-commun
COPY bmo-serveur ./bmo-serveur
COPY bmo-client ./bmo-client
# Nécessaire si -am est utilisé sur le pom parent et qu'il y a des interdépendances

# Cette commande va construire bmo-commun d'abord (car -am) puis bmo-serveur
RUN mvn -f /app_projet_bmo/pom.xml clean package -DskipTests -pl akandan.bahou.kassy:bmo-serveur -am

FROM openjdk:17-slim
WORKDIR /app_serveur
COPY --from=constructeur_bmo_serveur /app_projet_bmo/bmo-serveur/target/bmo-serveur-*.jar app-bmo-serveur.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app-bmo-serveur.jar"]
LABEL maintainer="VotreNomOuPseudo"
LABEL version="1.0"
LABEL description="Image Docker pour l'application serveur BMO."