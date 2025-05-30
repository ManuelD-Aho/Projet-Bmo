#!/bin/bash

# ==============================================================================
# Script de Lancement Avancé pour BMO Client avec X11 Forwarding
# ==============================================================================
# Objectif: Tenter d'automatiser et de simplifier la configuration X11
#           pour lancer le client JavaFX BMO conteneurisé.
# Auteur: VotreNom/Pseudo
# Version: 1.0
#
# IMPORTANT: Ce script fait de son mieux mais ne peut garantir un
#            fonctionnement parfait sur toutes les configurations.
#            Une compréhension basique de X11 sur votre système est utile.
# ==============================================================================

# --- Configuration et Constantes ---
SCRIPT_NAME=$(basename "$0")
DOCKER_COMPOSE_FILE="docker-compose.yml" # Assurez-vous qu'il est au même niveau ou ajustez le chemin
CLIENT_SERVICE_NAME="bmo-client-app"
REQUIRED_COMMANDS=("docker" "docker-compose") # docker-compose pourrait être "docker compose" (v2)

# Couleurs pour les messages (optionnel, mais améliore la lisibilité)
C_RESET='\033[0m'
C_RED='\033[0;31m'
C_GREEN='\033[0;32m'
C_YELLOW='\033[0;33m'
C_BLUE='\033[0;34m'
C_CYAN='\033[0;36m'

# --- Fonctions Utilitaires ---

log_info() {
    echo -e "${C_BLUE}[INFO]${C_RESET} $1"
}

log_warn() {
    echo -e "${C_YELLOW}[AVERTISSEMENT]${C_RESET} $1"
}

log_error() {
    echo -e "${C_RED}[ERREUR]${C_RESET} $1"
}

log_success() {
    echo -e "${C_GREEN}[SUCCÈS]${C_RESET} $1"
}

log_debug() { # Mettre DEBUG=true pour activer
    if [ "$DEBUG" = "true" ]; then
        echo -e "${C_CYAN}[DEBUG]${C_RESET} $1"
    fi
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "La commande '$1' est requise mais n'a pas été trouvée. Veuillez l'installer."
        return 1
    fi
    return 0
}

# Vérifie si Docker Compose V2 (docker compose) doit être utilisé
get_docker_compose_cmd() {
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        log_error "Ni 'docker-compose' (V1) ni 'docker compose' (V2) n'ont été trouvés."
        log_error "Veuillez installer Docker Compose."
        exit 1
    fi
    log_debug "Utilisation de Docker Compose: $DOCKER_COMPOSE_CMD"
}

# --- Détection de l'OS et Configuration Spécifique ---

detect_os() {
    local uname_s
    uname_s=$(uname -s)
    case "${uname_s}" in
        Linux*)
            if grep -qEi "(Microsoft|WSL)" /proc/version &> /dev/null; then
                OS_TYPE="WSL"
            elif [ -f /etc/os-release ] && grep -qEi "Ubuntu" /etc/os-release; then
                OS_TYPE="Ubuntu" # Plus spécifique que Linux générique
            elif [ -f /etc/os-release ] && grep -qEi "Fedora" /etc/os-release; then
                OS_TYPE="Fedora"
            else
                OS_TYPE="Linux"
            fi
            ;;
        Darwin*)
            OS_TYPE="macOS"
            ;;
        CYGWIN*|MINGW*|MSYS*)
            OS_TYPE="Windows_GitBash" # Ou autre environnement simulant Unix sur Windows
            ;;
        *)
            OS_TYPE="unknown:${uname_s}"
            ;;
    esac
    log_info "Système d'exploitation détecté : ${OS_TYPE}"
}

configure_display_wsl() {
    log_info "Configuration pour WSL..."
    if ! pgrep -x "vcxsrv.exe" &> /dev/null && ! pgrep -x "XWin.exe" &> /dev/null && ! pgrep -x "Xming.exe" &> /dev/null ; then
        log_warn "Aucun serveur X (VcXsrv, Xming, Cygwin/X) ne semble tourner sur Windows."
        log_warn "Veuillez lancer un serveur X sur Windows (ex: VcXsrv avec 'Disable access control')."
    fi

    # Tentative de récupération de l'IP de l'hôte Windows
    HOST_IP=$(cat /etc/resolv.conf 2>/dev/null | grep nameserver | awk '{print $2}')

    if [ -z "$HOST_IP" ]; then
        log_warn "Impossible de déterminer l'IP de l'hôte Windows depuis /etc/resolv.conf."
        log_warn "Si vous utilisez WSL1, essayez 'export DISPLAY=:0.0'."
        log_warn "Si vous utilisez WSL2, la méthode ci-dessus est généralement correcte."
        log_warn "Alternative: essayez 'export DISPLAY=$(hostname -I | awk '{print $1}'):0.0' SI votre serveur X écoute sur l'IP de WSL."
        # Tenter une valeur par défaut souvent utilisée avec WSL1 ou si VcXsrv écoute sur localhost
        # Ceci est moins fiable pour WSL2 car le réseau est séparé.
        # export DISPLAY=:0.0
        # log_info "Tentative avec DISPLAY=:0.0 (plus adapté pour WSL1 ou configuration spécifique)."
    else
        export DISPLAY="${HOST_IP}:0.0"
    fi

    export LIBGL_ALWAYS_INDIRECT=1 # Souvent nécessaire pour WSL
    log_info "DISPLAY configuré à : $DISPLAY (pour WSL)"
    log_info "LIBGL_ALWAYS_INDIRECT configuré à 1."
    log_info "Assurez-vous que votre serveur X (ex: VcXsrv) est lancé sur Windows et que le contrôle d'accès est désactivé."
}

configure_display_macos() {
    log_info "Configuration pour macOS..."
    if ! pgrep -x "XQuartz" &> /dev/null; then
        log_warn "XQuartz ne semble pas être lancé. Veuillez le démarrer."
        log_warn "Assurez-vous que dans XQuartz > Préférences > Sécurité, 'Autoriser les connexions du réseau' est coché."
    fi

    # Sur macOS, Docker Desktop expose souvent l'hôte via 'host.docker.internal'
    # XQuartz doit être configuré pour écouter sur le réseau.
    # L'IP de la machine peut aussi fonctionner si XQuartz est bien configuré.
    # Parfois, il faut utiliser l'IP de l'interface réseau (en0, etc.)
    # Laisser l'utilisateur gérer DISPLAY est parfois le plus simple si les détections échouent.

    if [ -z "$DISPLAY" ]; then
        # Tentative avec l'IP de l'interface en0 (Wi-Fi/Ethernet principal)
        # Cela suppose que XQuartz écoute sur cette IP.
        # Note: `ifconfig` est déprécié sur certains systèmes Linux, mais souvent présent sur macOS.
        # `ip addr` est l'alternative Linux.
        MAC_IP=$(ifconfig en0 2>/dev/null | grep "inet " | awk '{print $2}')
        if [ -n "$MAC_IP" ]; then
            export DISPLAY="${MAC_IP}:0"
            log_info "Tentative de configuration de DISPLAY avec l'IP de en0 : $DISPLAY"
        else
            # Fallback à host.docker.internal, nécessite Docker Desktop récent
            export DISPLAY="host.docker.internal:0"
            log_info "Tentative de configuration de DISPLAY avec 'host.docker.internal:0'"
        fi
    else
        log_info "Utilisation de la variable DISPLAY existante : $DISPLAY"
    fi
    log_info "Assurez-vous que XQuartz est lancé, configuré pour les connexions réseau."
    log_info "Vous pourriez avoir besoin d'exécuter 'xhost +' dans un terminal XQuartz pour autoriser les connexions."
    # Alternative pour xhost sur macOS si XQuartz est bien configuré:
    # Parfois, il faut ajouter l'IP de la gateway Docker ou du conteneur.
    # `xhost +$(docker inspect bridge -f '{{(index .IPAM.Config 0).Gateway}}')`
}

configure_display_linux() {
    log_info "Configuration pour Linux natif..."
    if [ -z "$XDG_SESSION_TYPE" ]; then
        log_warn "Impossible de déterminer le type de session (X11 ou Wayland)."
    elif [ "$XDG_SESSION_TYPE" = "wayland" ]; then
        log_warn "Vous semblez utiliser Wayland. Le X11 forwarding peut être plus complexe."
        log_warn "Assurez-vous que XWayland est fonctionnel et que votre compositeur Wayland le gère correctement."
        log_warn "Certaines applications X11 peuvent ne pas fonctionner nativement ou nécessiter des configurations supplémentaires."
    fi

    if [ -z "$DISPLAY" ]; then
        log_info "Variable DISPLAY non définie. Tentative avec ':0'."
        export DISPLAY=":0"
    fi
    log_info "DISPLAY configuré à : $DISPLAY (pour Linux)"
    log_info "Si l'application ne s'affiche pas, vérifiez les permissions X11."
    log_info "Vous pourriez avoir besoin d'exécuter 'xhost +local:docker' ou 'xhost +SI:localuser:$(id -un)'."
    log_info "Pour un test rapide (moins sécurisé), vous pouvez utiliser 'xhost +' (et 'xhost -' ensuite)."
}

configure_display_unknown() {
    log_warn "Système d'exploitation non reconnu ou configuration spécifique."
    if [ -z "$DISPLAY" ]; then
        log_error "La variable DISPLAY n'est pas définie. Le client graphique ne pourra pas s'afficher."
        log_error "Veuillez configurer manuellement votre serveur X11 et la variable DISPLAY."
        log_error "Exemple: export DISPLAY=adresse_ip_serveur_x:0.0"
        return 1
    else
        log_info "Utilisation de la variable DISPLAY existante : $DISPLAY"
        log_info "Assurez-vous que votre serveur X11 est correctement configuré pour accepter les connexions."
        return 0
    fi
}

# --- Vérification des Prérequis X11 ---
check_x11_prerequisites() {
    log_info "Vérification des prérequis X11..."

    if [ -z "$DISPLAY" ]; then
        log_error "La variable DISPLAY n'est pas configurée. Impossible de continuer."
        return 1
    fi

    # Tenter une commande xhost simple pour voir si elle est disponible et si on a accès au serveur X
    # Cela ne garantit pas que les permissions sont correctes pour Docker, mais c'est un indicateur.
    if command -v xhost &> /dev/null; then
        log_debug "Commande xhost disponible."
        # Tenter une requête xhost non modifiante. Si cela échoue, le serveur X n'est probablement pas accessible.
        if ! xhost &> /dev/null; then
            log_warn "Impossible d'accéder au serveur X11 via 'xhost'. Le serveur X est-il lancé et accessible ?"
            log_warn "Les permissions X11 pourraient être incorrectes."
        else
            log_debug "Accès de base au serveur X11 via 'xhost' semble fonctionner."
        fi
    else
        log_warn "La commande 'xhost' n'est pas trouvée. Impossible de vérifier/modifier les permissions X11 automatiquement."
        log_warn "Assurez-vous que votre serveur X autorise les connexions des conteneurs Docker."
        log_warn "(ex: VcXsrv avec 'Disable access control', XQuartz avec 'Allow connections from network clients')."
    fi

    # Test de base avec xdpyinfo (si installé)
    if command -v xdpyinfo &> /dev/null; then
        if ! xdpyinfo -display "$DISPLAY" &> /dev/null; then
            log_error "xdpyinfo n'a pas pu se connecter à l'affichage '$DISPLAY'."
            log_error "Vérifiez que votre serveur X est lancé et que DISPLAY est correct."
            return 1
        else
            log_success "xdpyinfo a pu se connecter à l'affichage '$DISPLAY'."
        fi
    else
        log_warn "xdpyinfo non trouvé. Impossible de tester la connexion à l'affichage de manière avancée."
    fi
    return 0
}


# --- Logique Principale du Script ---

main() {
    log_info "Démarrage du script de lancement BMO Client: ${SCRIPT_NAME}"
    DEBUG="${DEBUG:-false}" # Mettre à true pour les logs de débogage: DEBUG=true ./script.sh

    # Vérifier les commandes requises
    for cmd in "${REQUIRED_COMMANDS[@]}"; do
        if ! check_command "$cmd"; then exit 1; fi
    done
    get_docker_compose_cmd # Définit DOCKER_COMPOSE_CMD

    # Détecter l'OS et tenter de configurer DISPLAY
    detect_os
    case $OS_TYPE in
        "WSL")
            configure_display_wsl
            ;;
        "macOS")
            configure_display_macos
            ;;
        "Linux"|"Ubuntu"|"Fedora")
            configure_display_linux
            ;;
        "Windows_GitBash")
            log_warn "Exécution depuis GitBash/MinGW/MSYS sur Windows."
            log_warn "Le X11 forwarding est généralement géré par un serveur X Windows (ex: VcXsrv)."
            log_warn "Assurez-vous que VcXsrv (ou équivalent) est lancé avec le contrôle d'accès désactivé."
            log_warn "La variable DISPLAY doit pointer vers ce serveur X (ex: export DISPLAY=localhost:0.0)."
            if [ -z "$DISPLAY" ]; then export DISPLAY=localhost:0.0; fi # Tentative
            log_info "Tentative avec DISPLAY=$DISPLAY"
            ;;
        *)
            if ! configure_display_unknown; then exit 1; fi
            ;;
    esac

    # Vérifier les prérequis X11 après configuration de DISPLAY
    if ! check_x11_prerequisites; then
        log_error "Les prérequis X11 ne sont pas satisfaits. Veuillez vérifier les messages ci-dessus."
        log_error "Pour obtenir de l'aide sur la configuration X11 pour Docker, consultez la documentation de Docker et de votre serveur X."
        exit 1
    fi

    log_info "Variable DISPLAY finale utilisée : $DISPLAY"

    # S'assurer que le fichier docker-compose.yml existe
    if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
        log_error "Fichier '$DOCKER_COMPOSE_FILE' non trouvé dans le répertoire courant."
        exit 1
    fi

    # Option: Nettoyer les anciens conteneurs du client avant de lancer
    # log_info "Nettoyage d'une éventuelle instance précédente du conteneur client..."
    # $DOCKER_COMPOSE_CMD rm -f -s -v "$CLIENT_SERVICE_NAME" &> /dev/null

    # Lancer les services requis (BDD, Serveur) en mode détaché s'ils ne tournent pas déjà.
    # --remove-orphans est utile si la composition a changé.
    log_info "Démarrage/Vérification des services dépendants (BDD, Serveur BMO)..."
    if ! $DOCKER_COMPOSE_CMD up -d --remove-orphans bmo-db bmo-serveur-app bmo-phpmyadmin; then
        log_error "Échec lors du démarrage des services dépendants avec docker-compose."
        exit 1
    fi
    log_success "Services dépendants démarrés."

    # Lancer le client BMO de manière interactive
    log_info "Lancement du client BMO ($CLIENT_SERVICE_NAME)..."
    log_info "Si l'interface graphique ne s'affiche pas :"
    log_info "1. Vérifiez que votre serveur X11 (VcXsrv, XQuartz, etc.) est bien lancé."
    log_info "2. Vérifiez que le contrôle d'accès de votre serveur X11 est désactivé ou configuré pour autoriser les connexions."
    log_info "3. Vérifiez que la variable DISPLAY ($DISPLAY) est correcte pour votre configuration."
    log_info "4. Consultez les logs du conteneur client pour des erreurs spécifiques (docker logs $(${DOCKER_COMPOSE_CMD} ps -q ${CLIENT_SERVICE_NAME}) )."

    # La variable DISPLAY est passée au conteneur via la section 'environment' du docker-compose.yml
    if ! $DOCKER_COMPOSE_CMD run --rm --service-ports "$CLIENT_SERVICE_NAME"; then
        log_error "Échec lors du lancement du client BMO avec '$DOCKER_COMPOSE_CMD run'."
        log_warn "Essayez de vérifier les logs du conteneur client qui a échoué (s'il n'a pas été supprimé par --rm)."
        exit 1
    fi

    log_success "Le client BMO a été lancé. Fermez la fenêtre du client pour terminer ce script."
    log_info "Pour arrêter tous les services BMO: '$DOCKER_COMPOSE_CMD down'"
}

# --- Exécution ---
main "$@"