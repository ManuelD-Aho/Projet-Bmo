FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_client

WORKDIR /app_projet_bmo

COPY pom.xml ./pom.xml
COPY bmo-commun ./bmo-commun
COPY bmo-client ./bmo-client
COPY bmo-serveur ./bmo-serveur

RUN mvn -f /app_projet_bmo/pom.xml clean package -DskipTests -pl akandan.bahou.kassy:bmo-client -am

FROM openjdk:17-jdk-slim
# FROM bellsoft/liberica-openjdk-debian-javafx:17 # Alternative avec JavaFX pré-inclus

WORKDIR /app_client

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libgtk-3-0 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libasound2 \
    libgl1-mesa-glx \
    libegl1 \
    libgles2 && \
    rm -rf /var/lib/apt/lists/*

COPY --from=constructeur_bmo_client /app_projet_bmo/bmo-client/target/bmo-client-*.jar app-bmo-client.jar

CMD ["java", "-jar", "app-bmo-client.jar"]

LABEL maintainer="ManuelD-Aho"
LABEL version="1.0"
LABEL description="Image Docker pour l'application client BMO (JavaFX)."