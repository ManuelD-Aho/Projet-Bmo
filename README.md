# BMO - Business Meeting Organizer

## Description
BMO est une application de gestion de réunions d'entreprise développée en Java avec une architecture modulaire.

## Architecture
- **bmo-commun** : Module contenant les classes partagées (DTOs, utilitaires, exceptions)
- **bmo-serveur** : Serveur backend gérant la logique métier et la persistance
- **bmo-client** : Client JavaFX pour l'interface utilisateur

## Technologies
- Java 17
- Spring Boot
- JavaFX
- Maven
- Docker

## Installation
```bash
mvn clean install
docker-compose up
```

## Utilisation
1. Démarrer le serveur : `java -jar bmo-serveur/target/bmo-serveur-1.0.0.jar`
2. Lancer le client : `java -jar bmo-client/target/bmo-client-1.0.0.jar`

## Licence
MIT License
