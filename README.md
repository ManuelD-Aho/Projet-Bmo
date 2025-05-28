# **BMO (Bureau de Réunions Ouvertes)**

## **Description du Projet**

BMO est une application Java Client-Serveur conçue pour la gestion de réunions virtuelles multimédias. L'objectif principal est d'optimiser l'organisation et la planification des réunions en entreprise, en commençant par des échanges textuels en temps réel. La communication repose sur des sockets TCP/IP, et la sécurité des connexions est une priorité.

L'application supporte la gestion des utilisateurs (authentification, comptes), la gestion complète des réunions (planification, accès, types de réunions), et la communication en temps réel.

## **Prérequis**

* Java Development Kit (JDK) version 17 ou ultérieure.
* Apache Maven 3.6.x ou ultérieure.
* Docker Engine.
* Docker Compose.

## **Structure du Projet**

Le projet est organisé en plusieurs modules Maven :

* `bmo` : Module parent.
* `bmo-commun` : Contient les classes partagées (DTOs, constantes, utilitaires).
* `bmo-serveur` : Contient la logique backend du serveur BMO.
* `bmo-client` : Contient l'application cliente JavaFX.

## **Instructions de Build**

Pour construire l'ensemble des modules du projet, exécutez la commande Maven suivante à la racine du projet (répertoire contenant ce README.md) :
```bash
mvn clean install
```

Cette commande compilera le code source, exécutera les tests (s'ils sont définis), et packagera les applications client et serveur en fichiers JAR exécutables dans leurs répertoires `target` respectifs.

## Instructions de Lancement avec Docker Compose

L'application BMO, incluant le serveur, la base de données MySQL et PhpMyAdmin, peut être lancée facilement à l'aide de Docker Compose.

1.  Assurez-vous que Docker et Docker Compose sont installés et en cours d'exécution.
2.  À la racine du projet, exécutez la commande :

    ```bash
    docker-compose up -d
    ```
    Le `-d` permet de lancer les conteneurs en mode détaché (en arrière-plan).

Services lancés :
*   **Serveur BMO** : Accessible sur le port `5000` de votre machine hôte.
*   **Base de Données MySQL** : Accessible sur le port `3306`. Les données sont persistées dans un volume Docker (`bmo_db_volume`).
*   **PhpMyAdmin** : Accessible via votre navigateur à l'adresse `http://localhost:8081`.
    *   Serveur : `bmo-db`
    *   Utilisateur Root MySQL : `root`
    *   Mot de Passe Root MySQL : `MotDePasseRootFort!` (tel que défini dans `docker-compose.yml`)

## Lancement du Client BMO (via Docker)

Le client JavaFX BMO peut également être lancé via Docker Compose.

**Attention pour les utilisateurs Windows et macOS :**
L'exécution d'une application graphique Linux (comme le client JavaFX dans son conteneur) sur Windows ou macOS via Docker nécessite une configuration de **X11 forwarding**. Cela implique généralement :
1.  L'installation d'un serveur X sur votre machine hôte (par exemple, VcXsrv pour Windows, XQuartz pour macOS).
2.  La configuration correcte de la variable d'environnement `DISPLAY`.
3.  L'autorisation des connexions depuis le conteneur Docker à votre serveur X (par exemple, avec `xhost +`).

Consultez la documentation spécifique à votre système d'exploitation et à votre serveur X pour les instructions détaillées de configuration du X11 forwarding. Sans cette configuration, l'interface graphique du client ne s'affichera pas.

Une fois Docker Compose lancé avec `docker-compose up -d`, le client (s'il n'est pas déjà lancé) peut être démarré via :
```bash
docker-compose start bmo-client
```
Ou pour voir les logs :
```bash
docker-compose logs -f bmo-client
```

Si vous souhaitez le lancer séparément ou le relancer :

```bash
docker-compose run --rm bmo-client
```

## **Avertissement sur les Identifiants**

**IMPORTANT :** Le fichier `docker-compose.yml` fourni dans ce projet contient des identifiants (mots de passe pour la base de données, PhpMyAdmin) en clair. Ceci est acceptable et pratique pour un environnement de développement local uniquement.

**NE PAS UTILISER CES IDENTIFIANTS EN CLAIR EN PRODUCTION.**

Pour un déploiement en production, utilisez des mécanismes sécurisés pour la gestion des secrets, tels que :

* Docker Secrets.
* Variables d'environnement injectées de manière sécurisée par votre système d'orchestration (Kubernetes, Swarm, etc.).
* Solutions de gestion de secrets externes (Vault, etc.).

## **Configuration**

La configuration principale du serveur se trouve dans `bmo-serveur/src/main/resources/application.properties`. Les configurations de connexion à la base de données dans `docker-compose.yml` (pour l'environnement Docker) sont prioritaires si `ConfigurateurServeur.java` est adapté pour lire les variables d'environnement.