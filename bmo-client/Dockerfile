FROM maven:3.8.5-openjdk-17 AS constructeur_bmo_client
WORKDIR /app_projet_bmo
COPY pom.xml ./pom.xml
COPY bmo-commun/pom.xml ./bmo-commun/pom.xml
COPY bmo-client/pom.xml ./bmo-client/pom.xml
COPY bmo-serveur/pom.xml ./bmo-serveur/pom.xml
RUN mvn -f /app_projet_bmo/pom.xml clean package -DskipTests -pl akandan.bahou.kassy:bmo-client -am

FROM openjdk:17-jdk-bullseye
ARG JAVAFX_VERSION=21.0.2
ENV JAVAFX_HOME=/opt/javafx-sdk
ENV LD_LIBRARY_PATH="${JAVAFX_HOME}/lib"
WORKDIR /app_client

# Le contexte de build est la racine du projet.
# Le Dockerfile est dans bmo-client/, donc le chemin vers le zip est bmo-client/javafx-sdk-linux.zip
COPY bmo-client/javafx-sdk-linux.zip /app_client/javafx-sdk.zip

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    unzip \
    libgtk-3-0 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libasound2 \
    libgl1-mesa-glx \
    libegl1 \
    libgles2 \
    fonts-liberation && \
    echo "Décompression du SDK JavaFX local..." && \
    rm -rf ${JAVAFX_HOME} && \
    mkdir -p /opt && \
    unzip javafx-sdk.zip -d /opt && \
    echo "Contenu de /opt après unzip:" && \
    ls -lA /opt && \
    echo "Renommage du répertoire SDK..." && \
    mv "/opt/javafx-sdk-${JAVAFX_VERSION}" "${JAVAFX_HOME}" && \
    echo "SDK JavaFX installé dans ${JAVAFX_HOME}" && \
    ls -l "${JAVAFX_HOME}/lib" && \
    rm javafx-sdk.zip && \
    rm -rf /var/lib/apt/lists/*

COPY --from=constructeur_bmo_client /app_projet_bmo/bmo-client/target/bmo-client-*.jar app-bmo-client.jar
CMD ["java", "-Dprism.lcdtext=false", "-Dprism.text=lcd", "--module-path", "${JAVAFX_HOME}/lib", "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.media", "-jar", "app-bmo-client.jar"]
LABEL maintainer="VotreNomOuPseudo"
LABEL version="1.0"
LABEL description="Image Docker pour l'application client BMO (JavaFX)."