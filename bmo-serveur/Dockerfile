FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_serveur

WORKDIR /app_projet_bmo

COPY pom.xml .
COPY bmo-commun ./bmo-commun
COPY bmo-serveur ./bmo-serveur
COPY bmo-client ./bmo-client

RUN mvn -pl bmo-serveur -am package -DskipTests

FROM openjdk:17-slim
WORKDIR /app_serveur
COPY --from=constructeur_bmo_serveur /app_projet_bmo/bmo-serveur/target/bmo-serveur-*.jar app-bmo-serveur.jar
ARG BMO_SERVER_PORT_ARG=5000
ENV BMO_SERVEUR_PORT=${BMO_SERVER_PORT_ARG}
EXPOSE ${BMO_SERVEUR_PORT}
CMD ["java", "-jar", "app-bmo-serveur.jar"]
LABEL maintainer="ManuelD-Aho"
LABEL version="1.0"
LABEL description="Image Docker pour l'application serveur BMO."