#!/bin/bash

# Script d'entrée pour l'application client BMO
# Résout les variables d'environnement pour JavaFX

echo "Démarrage de l'application client BMO..."
echo "JAVAFX_HOME: ${JAVAFX_HOME}"

# Configuration des variables d'environnement par défaut
export BMO_SERVEUR_ADRESSE=${BMO_SERVEUR_ADRESSE:-bmo-serveur}
export BMO_SERVEUR_PORT=${BMO_SERVEUR_PORT:-5000}

echo "Configuration réseau:"
echo "- Serveur BMO: ${BMO_SERVEUR_ADRESSE}:${BMO_SERVEUR_PORT}"

# Vérification de l'existence de JavaFX
echo "Vérification de l'installation JavaFX..."
if [ ! -d "${JAVAFX_HOME}" ]; then
    echo "ERREUR: JAVAFX_HOME introuvable : ${JAVAFX_HOME}"
    exit 1
fi

if [ ! -d "${JAVAFX_HOME}/lib" ]; then
    echo "ERREUR: Répertoire JavaFX lib introuvable : ${JAVAFX_HOME}/lib"
    exit 1
fi

echo "Contenu de ${JAVAFX_HOME}/lib:"
ls -la "${JAVAFX_HOME}/lib"

# Vérification des modules JavaFX essentiels
REQUIRED_MODULES=("javafx.controls" "javafx.fxml" "javafx.graphics")
for module in "${REQUIRED_MODULES[@]}"; do
    if [ ! -f "${JAVAFX_HOME}/lib/${module}.jar" ]; then
        echo "ERREUR: Module JavaFX manquant: ${module}.jar"
        exit 1
    fi
done

# Vérification de l'existence du JAR de l'application
JAR_FILE="app-bmo-client.jar"
if [ ! -f "${JAR_FILE}" ]; then
    echo "ERREUR: Fichier JAR de l'application introuvable: ${JAR_FILE}"
    exit 1
fi

# Variables pour la commande Java
JAVA_OPTS="-Dprism.lcdtext=false -Dprism.text=lcd -Dprism.verbose=true"
JAVA_OPTS="$JAVA_OPTS -Dbmo.serveur.adresse=${BMO_SERVEUR_ADRESSE}"
JAVA_OPTS="$JAVA_OPTS -Dbmo.serveur.port=${BMO_SERVEUR_PORT}"
MODULE_PATH="${JAVAFX_HOME}/lib"
ADD_MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.media"

echo "Commande à exécuter:"
echo "java ${JAVA_OPTS} --module-path ${MODULE_PATH} --add-modules ${ADD_MODULES} -jar ${JAR_FILE}"

# Test de connectivité réseau vers le serveur
echo "Test de connectivité vers le serveur BMO..."
if command -v nc >/dev/null 2>&1; then
    if nc -z "${BMO_SERVEUR_ADRESSE}" "${BMO_SERVEUR_PORT}"; then
        echo "✓ Connectivité vers ${BMO_SERVEUR_ADRESSE}:${BMO_SERVEUR_PORT} établie"
    else
        echo "⚠ Impossible de se connecter à ${BMO_SERVEUR_ADRESSE}:${BMO_SERVEUR_PORT}"
        echo "Le serveur n'est peut-être pas encore démarré"
    fi
else
    echo "⚠ nc (netcat) non disponible pour tester la connectivité"
fi

# Exécution de l'application
echo "Lancement de l'application client BMO..."
exec java ${JAVA_OPTS} --module-path "${MODULE_PATH}" --add-modules "${ADD_MODULES}" -jar "${JAR_FILE}"
