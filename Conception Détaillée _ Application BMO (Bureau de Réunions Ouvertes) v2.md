**Rapport de Conception Détaillé : Application BMO (Bureau de Réunions Ouvertes)** 

**Introduction** 

- **Objectif du Rapport :** Le présent document a pour vocation de présenter une conception architecturale exhaustive pour l'application BMO (Bureau de Réunions Ouvertes). Il servira de fondation technique pour guider son développement, son déploiement et ses évolutions futures, en s'assurant que les principes fondamentaux de modularité, robustesse, sécurité et facilité de déploiement sont respectés. 
- **Contexte de BMO :** BMO est une application client-serveur, développée en Java, destinée à l'organisation et à la gestion de réunions virtuelles multimédias au sein des entreprises. L'objectif premier est d'optimiser les processus d'organisation et de planification, en commençant par des échanges textuels en temps réel. La communication repose sur des sockets TCP/IP, et la sécurité des connexions est une priorité. Un aspect crucial et impératif du projet est l'utilisation de Maven pour la gestion des dépendances et du build, et de Docker pour la conteneurisation du client et du serveur, afin d'assurer la portabilité et de faciliter les échanges entre développeurs et environnements. 1 
- **Principes Directeurs de Conception :** La conception de BMO s'articule autour de quatre piliers essentiels : 
- **Modularité :** Séparation claire des préoccupations en modules distincts pour faciliter la maintenance, l'évolution et les tests. 
- **Robustesse :** Conception d'un système capable de gérer les erreurs, de maintenir sa stabilité en charge et d'assurer la persistance fiable des données. 
- **Sécurité :** Implémentation de mécanismes de protection des données et des communications à tous les niveaux de l'application. 
- **Facilité de Déploiement :** Utilisation de technologies de conteneurisation et d'outils de build standardisés pour simplifier la mise en place et la gestion des environnements. 
1. **Analyse Fonctionnelle Exhaustive** 

Cette section a pour objectif de décomposer de manière rigoureuse les besoins de l'application BMO en fonctionnalités granulaires. Ces fonctionnalités sont ensuite regroupées en modules logiques, chacun doté de responsabilités clairement établies, afin de structurer la conception et le développement de l'application. 

1. **Identification et Liste des Fonctionnalités Requises** 

L'analyse croisée des documents de référence, notamment le cahier des charges initial et l'analyse de l'existant 1, permet d'établir une liste complète des fonctionnalités attendues pour BMO. L'application doit couvrir l'intégralité du cycle de vie des réunions virtuelles, depuis l'authentification des utilisateurs et la planification des sessions, jusqu'à la participation active et la gestion administrative avancée. 

- **Fonctionnalités générales de BMO :** 
- Organisation et gestion de réunions virtuelles, avec une perspective multimédia (bien que la première version se concentre sur le texte). 1 
- Communication textuelle en temps réel entre les participants d'une réunion. 1 Le potentiel pour des flux audio/vidéo est explicitement mentionné comme une évolution possible. 1 
- Garantie de la sécurité des connexions et des échanges de données. 1 
- Conception pour la montée en charge (scalabilité) afin de supporter un nombre croissant d'utilisateurs et de réunions simultanées. 1 
- Gestion stricte et différenciée des rôles utilisateurs (Administrateur, Organisateur, Participant) avec des permissions spécifiques pour chaque rôle. 

  1 

- Mise en œuvre d'une architecture client-serveur, avec un client graphique développé en JavaFX (suivant un motif MVC) et une logique serveur en Java. 1 
- Utilisation de sockets TCP/IP comme protocole de transport pour la communication entre le client et le serveur. 1 
- Persistance des données applicatives (utilisateurs, réunions, etc.) au moyen d'une base de données relationnelle MySQL, accessible via JDBC. 1 
- Compatibilité multiplateforme pour l'application cliente (Windows, macOS, Linux). 1 
- Déploiement standardisé et portable grâce à la conteneurisation Docker, tant pour le composant serveur que pour le composant client. 1 

L'orientation "entreprise" de BMO se traduit par des exigences spécifiques telles que le contrôle d'accès fin, la possibilité de créer des réunions privées, et l'option d'utiliser TLS/SSL pour chiffrer les communications. 1 Cette approche distingue BMO des plateformes grand public en privilégiant le contrôle et la sécurité. 

2. **Regroupement en Modules Logiques** 

Pour une meilleure organisation et une conception modulaire, les fonctionnalités identifiées sont regroupées en modules logiques. Chaque module encapsule un ensemble cohérent de responsabilités. Tous les noms de classes et méthodes Java 

développées pour ces modules devront être en français. 

1. **Module : GestionUtilisateurs** 
- **Responsabilités :** Ce module est central pour la sécurité et la personnalisation de l'expérience BMO. Il prend en charge l'ensemble du cycle de vie des utilisateurs, incluant leur identification, leur authentification, la gestion de leurs profils et la définition de leurs droits d'accès au sein de l'application. 
- **Fonctionnalités Détaillées :** 
- **Authentification Utilisateur :** 
- Connexion sécurisée via un identifiant (nom d'utilisateur ou adresse e-mail) et un mot de passe personnel. 1 
- Le processus d'authentification implique une vérification des identifiants saisis par le client auprès du serveur, qui consulte la base de données des utilisateurs. 1 
- Le serveur retourne une réponse explicite : AUTH\_OK en cas de succès, potentiellement accompagnée d'informations sur le profil utilisateur (notamment son rôle), ou AUTH\_ECHEC en cas d'échec. 1 
- Suite à une authentification réussie, l'application cliente redirige 

  l'utilisateur vers le tableau de bord correspondant à son rôle (Admin, Organisateur ou Participant). 1 

- **Création de Compte (Auto-inscription) :** 
- Une option permettant aux nouveaux utilisateurs de créer leur propre compte peut être proposée sur l'interface de connexion (LoginView.fxml). Cette fonctionnalité est conditionnée par son activation par un administrateur de l'application. 1 
- **Gestion de Profil Utilisateur (par l'utilisateur lui-même) :** 
- Possibilité pour l'utilisateur connecté de modifier certaines informations de son profil, telles que son nom d'affichage et, potentiellement, son mot de passe. 1 
- Configuration des préférences personnelles liées à l'utilisation de l'application, notamment pour les aspects audio et vidéo (activation/désactivation par défaut de la caméra/microphone, choix des périphériques). 1 
- **Déconnexion Sécurisée :** 
- Mise à disposition d'une fonctionnalité de déconnexion explicite, permettant à l'utilisateur de clore sa session de manière sécurisée. 1 
- Lors de la déconnexion, l'application cliente doit retourner à l'interface de connexion (LoginView.fxml). 1 
- Une notification de déconnexion (commande DECONNEXION) est envoyée 

  au serveur afin qu'il puisse libérer les ressources associées à la session de l'utilisateur. 1 

- **Gestion des Rôles et Permissions (principalement par l'Administrateur) :** 
- L'application définit trois rôles utilisateurs principaux avec des ensembles de permissions distincts 1 : 
  - **Administrateur :** Dispose de l'intégralité des droits sur l'application. Ses responsabilités incluent la gestion complète des comptes utilisateurs (création, modification, suppression), la configuration globale des paramètres de l'application, ainsi que la capacité d'organiser des réunions et d'y participer. L'administrateur accède à une interface dédiée, l'AdminDashboardView.fxml. 1 
  - **Organisateur :** Est un utilisateur standard doté de permissions spécifiques pour la planification et la gestion des réunions. Il peut créer de nouvelles réunions, y inviter des participants, et superviser leur déroulement. Les fonctionnalités de création et de gestion des réunions lui sont accessibles via l'OrganizerDashboardView.fxml (ou un mode spécifique du DashboardView.fxml unifié). 1 
  - **Participant :** Est un utilisateur standard dont le rôle principal est d'assister aux réunions. Il peut rejoindre les réunions publiques ou celles pour lesquelles il a reçu une invitation, et y contribuer activement via les outils de communication (chat, vidéo si activée). 1 Son interface principale est le ParticipantDashboardView.fxml (ou un mode spécifique du DashboardView.fxml unifié). 
- **Stockage Sécurisé des Mots de Passe :** 
- Une exigence de sécurité fondamentale est que les mots de passe des utilisateurs ne soient jamais stockés en clair. Ils doivent être systématiquement hachés à l'aide d'algorithmes robustes (par exemple, bcrypt ou SHA-256 avec un sel aléatoire unique par utilisateur) avant leur enregistrement en base de données. 1 
- **Politique de Complexité des Mots de Passe :** 
- L'administrateur de l'application doit avoir la possibilité d'activer et de configurer une politique de complexité pour les mots de passe. Si activée, cette politique impose des critères minimaux lors de la création ou de la modification d'un mot de passe (longueur minimale, présence de majuscules, minuscules, chiffres, caractères spéciaux). 1 

Il est à noter une subtilité dans la gestion des rôles : la base de données pourrait stocker un rôle système de base (par exemple, UTILISATEUR ou ADMINISTRATEUR), tandis que la distinction plus fine entre Organisateur et Participant serait une attribution de capacité ou un contexte applicatif pour un utilisateur de type 

UTILISATEUR. 1 Cette approche offre une flexibilité appréciable. La logique de détermination des capacités spécifiques d'un utilisateur sera implémentée dans la couche service de l'application. 

2. **Module : GestionReunions** 
- **Responsabilités :** Ce module est au cœur de la proposition de valeur de BMO. Il est responsable de la gestion complète du cycle de vie des réunions virtuelles, depuis leur planification initiale et la configuration de leurs paramètres, jusqu'à leur déroulement et leur clôture. Il inclut également la gestion des accès et la différenciation des types de réunions. 
- **Fonctionnalités Détaillées :** 
- **Planification de Réunion (par un Organisateur ou un Administrateur) :** 
- Permettre la création d'une nouvelle réunion en définissant ses attributs essentiels : titre, sujet ou ordre du jour détaillé, date et heure de début programmées, durée prévue (en minutes ou heures), et type de réunion (Standard/Publique, Privée, Démocratique). 1 
- La validation de ce formulaire de planification côté client déclenche l'envoi d'une commande NOUVELLE\_REUNION au serveur, qui se charge de créer l'entrée correspondante en base de données et de notifier les clients concernés. 1 
- **Consultation des Détails de Réunion :** 
- Les utilisateurs doivent pouvoir visualiser les informations clés des réunions auxquelles ils ont accès : titre, date et heure, type, statut actuel (Planifiée, Ouverte, Terminée), et nom de l'organisateur. 1 
- **Modification des Détails de Réunion (par l'Organisateur ou un Administrateur) :** 
- L'organisateur d'une réunion (ou un administrateur) doit avoir la possibilité de modifier les attributs d'une réunion déjà planifiée, tant qu'elle n'est pas terminée. 1 Une fonctionnalité de modification "à la volée" de certains détails pendant une réunion ouverte peut aussi être envisagée. 1 
- **Suppression de Réunion (par l'Organisateur ou un Administrateur) :** 
- La capacité de supprimer une réunion planifiée (avant son démarrage) est une fonctionnalité implicite de la "gestion des réunions". 1 
- **Gestion des Différents Types de Réunion :** 
- **Réunions Publiques/Standards :** Ouvertes à tous les utilisateurs authentifiés. 1 
- **Réunions Privées :** Accès limité aux utilisateurs invités par l'organisateur. 1 
- **Réunions Démocratiques :** Gestion de la prise de parole plus souple et automatisée (premier demandeur, premier servi). 1 
- **Invitation de Participants (par l'Organisateur pour les réunions privées) :** 
- L'organisateur doit pouvoir sélectionner les utilisateurs à inviter. 1 
- Le système doit gérer les invitations et le statut de réponse. 1 
- **Ouverture de Réunion (par l'Organisateur) :** 
- L'organisateur "démarre" officiellement une réunion planifiée (statut "Ouverte"). 1 
- **Clôture de Réunion (par l'Organisateur) :** 
- L'organisateur clôt la réunion (statut "Terminée"). Commande CLOTURER\_REUNION. 1 
- **Rejoindre une Réunion (par tout utilisateur autorisé) :** 
- Les utilisateurs peuvent rejoindre une réunion "Ouverte" et autorisée. Commande REJOINDRE. 1 
- **Quitter une Réunion (par tout participant) :** 
- Chaque participant peut quitter une réunion en cours. Commande QUITTER. 1 
- **Fonctionnalités Avancées pour les Organisateurs (configurables par l'Administrateur) :** 
- Vue Calendrier interactive. 1 
- Modèles de réunion (templates). 1 
- Statistiques et Rapports. 1 
- Planification de réunions récurrentes. 1 
- Mode "Démarrage Rapide". 1 
- Export/Import de listes de réunions (JSON). 1 

La gestion de la prise de parole dans les réunions "démocratiques" 1 nécessite une file d'attente claire et visible pour les demandes de parole. Le protocole devra inclure des messages spécifiques (ex: DEMANDER\_PAROLE\_DEMOCRATIQUE, PROCHAIN\_ORATEUR\_DEMOCRATIQUE). 

3. **Module : CommunicationTempsReel** 
- **Responsabilités :** Ce module assure la fluidité des échanges d'informations, incluant la communication au sein d'une réunion et les notifications serveur. 
- **Fonctionnalités Détaillées :** 
- **Chat Textuel Instantané au sein d'une Réunion :** 
- Permettre l'envoi et la réception de messages texte en temps réel. 1 
- Les messages sont transmis au serveur, qui les diffuse aux autres participants. 1 
- Interface client avec champ de saisie et bouton "Envoyer". 1 
- **Gestion de la Prise de Parole (Réunions Standard/Privé) :** 
- Un participant signale son intention via "Demander la parole" (requête 

  DEMANDER\_PAROLE). 1 

- Le serveur relaie à l'organisateur. 1 
- L'organisateur "Accepte" (AUTORISER\_PAROLE) ou "Refuse" (REFUSER\_PAROLE). 1 
- Si acceptée, notification PAROLE\_ACCORDEE au participant. 1 
- **Notifications Push en Temps Réel (Serveur vers Clients) :** 
- Le serveur envoie des messages proactifs aux clients. 1 
- Exemples : mise à jour liste réunions, alertes réunions imminentes, 

  nouvelles invitations, notifications "in-situ" (MeetingView.fxml : nouveau message, demande parole, UTILISATEUR\_REJOINT, UTILISATEUR\_PARTI). 1 

- **Transmission Audio/Vidéo (Fonctionnalité Potentielle Future) :** 
- L'architecture doit anticiper l'intégration de flux audio/vidéo. 1 
- Fonctionnalités client pour démarrer/arrêter caméra/microphone, afficher flux vidéo. 1 
- La méthode de transmission (TCP, UDP, P2P, serveurs média) est une décision de conception majeure. 1 
- **Fonctionnalités de Communication Avancées (configurables par l'Administrateur) :** 
- Partage d'écran. 1 
- Enregistrement de la réunion. 1 
- Réactions émojis. 1 
- Sondages et quiz en temps réel. 1 
- Gestion audio avancée (couper son tous/individuel). 1 
- Améliorations du Chat (recherche, mentions, export). 1 

L'architecture de communication centralisée 1 pourrait devenir un goulot d'étranglement pour les flux multimédias si elle n'évolue pas vers des protocoles et architectures plus adaptés (UDP, RTP/SRTP, WebRTC, SFU/MCU). 1 

4. **Module : NoyauServeur** 
- **Responsabilités :** Épine dorsale de la logique applicative côté serveur, gérant connexions, commandes, persistance, sécurité et configuration. 
- **Fonctionnalités Détaillées :** 
- **Gestion des Connexions Client Multiples :** 
- Écoute des nouvelles connexions sur port TCP/IP configurable. 1 
- Modèle avec un thread par client (issu d'un pool de threads ExecutorService). 1 Une instance de GestionnaireClient.java par connexion. 

  1 

- Limite configurable du nombre de connexions simultanées. 1 
- Timeout pour connexions inactives. 1 
- **Traitement des Commandes Client :** 
- Réception et analyse des commandes textuelles. 1 
- Routage vers services appropriés. 
- Préparation et envoi des réponses et notifications. 
- **Persistance des Données (Interaction avec MySQL via JDBC) :** 
- Interactions avec MySQL via JDBC. 1 
- Pool de connexions JDBC configurable. 1 
- Option de reconnexion automatique à la base de données. 1 
- **Gestion de la Configuration Applicative :** 
- Chargement et gestion de la configuration (application.properties, AdminDashboardView.fxml). 1 
- Paramètres configurables : Réseau, Sécurité (TLS/SSL, politique mdp), Base de Données (H2/MySQL), Fonctionnalités (Chat, Vidéo, UI). 1 
- Sauvegarde des modifications, redémarrage serveur. 1 
- **Sécurité des Communications Côté Serveur :** 
- Option d'activer le chiffrement TLS/SSL (certificat SSL, keystore). 1 
- **Export Automatique de la Base de Données (Fonctionnalité à Risque) :** 
- Export via mysqldump après modifications majeures (fichier 

  Bmo-app.sql). 1 

L'export systématique de la base via mysqldump 1 peut impacter les performances et devrait être optionnel, asynchrone et moins fréquent. Le caractère optionnel de TLS/SSL 1 est une préoccupation majeure ; TLS/SSL devrait être activé par défaut pour une application d'entreprise. 1 

5. **Module : InterfaceClient** 
- **Responsabilités :** Interaction avec l'utilisateur final, affichage des informations, capture des entrées, communication avec le serveur, gestion de l'état local. L'utilisation de multiples fichiers FXML est possible et encouragée pour une interface utilisateur riche et modulaire. 
- **Fonctionnalités Détaillées (structurées par les vues FXML principales) :** 
- **LoginView.fxml (Interface de Connexion)** 1 **:** 
- Champs identifiant/mot de passe, bouton "Se connecter". 
- Option "Créer un compte" (si activée). 
- Affichage des messages d'erreur. 
- **DashboardView.fxml (Tableau de Bord Principal – Unifié)** 1 **:** 
- S'adapte au rôle (Participant/Organisateur). 
- Liste des réunions accessibles (publiques, privées invitées, organisées). 
- Infos par réunion : titre, date/heure, type, statut. 
- **Fonctionnalités communes :** "Rejoindre", recherche/filtres, rafraîchissement auto (push), notifications, "Se déconnecter". 
- **Fonctionnalités Organisateur :** "Planifier une réunion", "Ouvrir" une réunion. 
- **Extensions possibles :** Vue Calendrier, gestion invitations, profil, historique, thèmes. 1 
- **AdminDashboardView.fxml (Tableau de Bord Administrateur)** 1 **:** 
- Accès aux fonctionnalités du tableau de bord standard. 
- Panneau de configuration : Gestion comptes utilisateurs, Paramètres application (Réseau, Sécurité, BD, Chat, Vidéo, UI, Fonctionnalités métier). 
- Actions : "Enregistrer les paramètres", "Redémarrer le serveur". 
- **MeetingView.fxml (Interface de Réunion)** 1 **:** 
- Espace central de la réunion (vidéo, chat, participants, contrôles). 
- **Contrôles tous utilisateurs :** Caméra/micro, chat, demander parole, quitter. 
- **Contrôles Organisateur :** Ouvrir/Clore réunion, gérer demandes parole, modifier détails réunion. 
- Liste dynamique des participants, notifications in-situ. 
- **Extensions possibles :** Partage écran, enregistrement, réactions, sondages. 1 
- **Gestion de Session Locale :** 
- Maintien local des infos session utilisateur (identifiant, nom, rôle, jeton optionnel). 1 
- **Communication avec le Serveur :** 
- Envoi commandes, réception/traitement réponses et notifications push, mise à jour UI. 

Le déploiement du client JavaFX via Docker avec redirection X11 1 standardise l'environnement mais peut complexifier l'usage sur Windows/macOS. Des alternatives pour une expérience utilisateur plus fluide pourraient être explorées. 1 

3. **Liste Exhaustive des Fonctionnalités par Module (Synthèse)** 
- **Module : GestionUtilisateurs** 
- Authentification (Login/Mot de passe, vérification serveur, réponse AUTH\_OK/AUTH\_ECHEC, redirection) 
- Création de Compte (Auto-inscription optionnelle) 
- Gestion de Profil Utilisateur (modification informations personnelles, préférences audio/vidéo) 
- Déconnexion Sécurisée (action explicite, retour LoginView.fxml, notification DECONNEXION serveur) 
- Gestion des Rôles (Administrateur, Organisateur, Participant) 
- Stockage Sécurisé des Mots de Passe (hachage + sel) 
- Politique de Complexité des Mots de Passe (configurable) 
- **Module : GestionReunions** 
- Planification de Réunion (titre, agenda, date/heure, durée, type, commande NOUVELLE\_REUNION) 
- Consultation des Détails de Réunion 
- Modification des Détails de Réunion 
- Suppression de Réunion 
- Gestion des Types de Réunion (Publique/Standard, Privée, Démocratique) 
- Invitation de Participants 
- Ouverture de Réunion 
- Clôture de Réunion (commande CLOTURER\_REUNION) 
- Rejoindre une Réunion (commande REJOINDRE) 
- Quitter une Réunion (commande QUITTER) 
- Fonctionnalités Avancées Organisateurs (Vue Calendrier, Modèles, Stats, Récurrence, Démarrage Rapide, Export/Import) 
- **Module : CommunicationTempsReel** 
- Chat Textuel Instantané 
- Gestion Prise de Parole (Standard/Privé) : DEMANDER\_PAROLE, AUTORISER\_PAROLE/REFUSER\_PAROLE, PAROLE\_ACCORDEE 
- Gestion Prise de Parole (Démocratique) : Automatisée, file d'attente visible. 
- Notifications Push (MàJ listes réunions, alertes, invitations, in-situ : UTILISATEUR\_REJOINT/UTILISATEUR\_PARTI) 
- Transmission Audio/Vidéo (Potentiel) 
- Fonctionnalités Avancées (Partage écran, Enregistrement, Réactions, Sondages, Gestion audio, Chat amélioré) 
- **Module : NoyauServeur** 
- Gestion Connexions Client Multiples (écoute TCP/IP, pool de threads, GestionnaireClient.java, limite connexions, timeout) 
- Traitement Commandes Client 
- Persistance Données (MySQL via JDBC, pool connexions, reconnexion auto) 
- Gestion Configuration Applicative (application.properties, AdminDashboardView.fxml : Réseau, Sécurité, BD, Fonctionnalités) 
- Sécurité Communications (option TLS/SSL) 
- Export Automatique BD (via mysqldump - à risque) 
- **Module : InterfaceClient** 
- Gestion LoginView.fxml 
- Gestion DashboardView.fxml (unifié Participant/Organisateur) 
- Gestion AdminDashboardView.fxml 
- Gestion MeetingView.fxml 
- Gestion Session Locale 
- Communication avec Serveur 

**Tableau : Rôles Utilisateurs et Permissions Détaillées** 



|**Rôle** |**Description des Droits Clés (Résumé)** |**Fonctionnalités Clés Accessibles (Exemples)** |**Vue Principale Associée** |
| - | :- | :- | :- |
|Administrateur |Tous droits système. |Gestion comptes utilisateurs (CRUD), configuration globale de l'app (réseau, sécurité, BD, fonctionnalités), organisation/particip ation réunions, redémarrage serveur. |AdminDashboardVie w.fxml |
|Organisateur |Droits de gestion du cycle de vie de ses réunions. |Planifier réunion, inviter participants, ouvrir/clore réunion, gérer parole (standard/privé), accès aux fonctionnalités avancées (templates, stats, récurrence si activées). |DashboardView.fxml (mode Orga) |
|Participant |Droits de participation et contribution aux réunions. |Rejoindre réunions (publiques/invité), participer au chat/vidéo, demander la parole (standard/privé), accès aux fonctionnalités participatives (sondages, réactions si activées). |DashboardView.fxml (mode Part) |

*(Basé sur 1)* 

2. **Architecture Technique** 

Cette section se penche sur l'ossature technique de l'application BMO, en détaillant les technologies fondamentales employées, les motifs de conception architecturaux retenus, et la manière dont les différents composants du système interagissent. Les noms des classes Java seront en français. 

1. **Architecture Générale** 
- **Modèle Client-Serveur Fondamental :** L'application BMO est structurée selon une architecture client-serveur. 1 Les applications clientes (JavaFX) initient des requêtes vers un serveur centralisé (Java) qui héberge la logique métier, gère l'état, assure la persistance et orchestre la communication. 
- **Client JavaFX avec Modèle MVC (Modèle-Vue-Contrôleur) :** 
- L'interface utilisateur cliente est développée avec JavaFX. 1 
- Le motif MVC est adopté pour structurer le code client. 1 
- **Modèle (Modele) :** Représente les données côté client (utilisateur connecté, liste réunions, etc.). 
- **Vue (Vue) :** Présentation des données et capture des interactions, définies par des fichiers FXML (ex: LoginView.fxml, DashboardView.fxml). 1 
- **Contrôleur (Controleur) :** Classes Java (ex: ControleurConnexion.java) associées aux vues FXML, gérant événements, mettant à jour le modèle et rafraîchissant la vue. 
- **Serveur Java Centralisé :** 
- Logique métier principale, gestion des sessions, coordination des réunions, persistance des données. 1 
- Gestion des connexions multiples, traitement des requêtes, interaction avec MySQL, envoi de notifications. 
2. **Protocole de Communication Textuel Simplifié (Sockets TCP/IP)** 
- **Fondation sur Sockets TCP/IP :** Communication via sockets TCP/IP pour sa fiabilité et son mode orienté connexion. 1 
- **Format du Protocole :** Protocole applicatif textuel simplifié, requête/réponse. 1 
- Commandes et réponses en chaînes ASCII, structurées avec délimiteurs (ex: |). 1 
- Chaque message terminé par \n. 1 
- **Structure des Commandes (Exemples Client vers Serveur, en français) :** 
- CONNEXION|{nom\_utilisateur\_ou\_email}|{mot\_de\_passe} 1 
- INSCRIPTION|{nom\_utilisateur}|{mot\_de\_passe}|{nom\_complet} 
- NOUVELLE\_REUNION|{titre}|{agenda}|{datetime\_iso8601}|{duree\_minutes}|{ty pe\_reunion}|[{id\_invite1},...] 1 
- REJOINDRE|{id\_reunion} 1 
- QUITTER|{id\_reunion} 1 
- MESSAGE\_CHAT|{id\_reunion}|{contenu\_message} 
- DEMANDER\_PAROLE|{id\_reunion} 1 
- AUTORISER\_PAROLE|{id\_reunion}|{id\_utilisateur\_demandeur} 
- REFUSER\_PAROLE|{id\_reunion}|{id\_utilisateur\_demandeur} 
- CLOTURER\_REUNION|{id\_reunion} 1 
- DECONNEXION 1 
- OBTENIR\_REUNIONS 1 
- ADMIN\_OBTENIR\_UTILISATEURS 
- ADMIN\_CREER\_UTILISATEUR|{login}|{password}|{name}|{role\_systeme} 
- ADMIN\_MODIFIER\_UTILISATEUR|{id\_user}|{login}|{name}|{role\_systeme} 
- ADMIN\_SUPPRIMER\_UTILISATEUR|{id\_user} 
- ADMIN\_OBTENIR\_CONFIG 
- ADMIN\_DEFINIR\_CONFIG|{cle\_config}|{valeur\_config} 
- **Structure des Réponses et Notifications Serveur (Exemples Serveur vers Client, en français) :** 
- AUTH\_OK|{id\_utilisateur}|{nom\_utilisateur}|{role\_applicatif} 1 
- AUTH\_ECHEC|{message\_erreur} 1 
- INSCRIPTION\_OK / INSCRIPTION\_ECHEC|{message\_erreur} 
- REUNION\_CREEE|{id\_reunion}|{titre}|... 
- REUNION\_MISE\_A\_JOUR|{id\_reunion}|... 
- REUNION\_SUPPRIMEE|{id\_reunion} 
- LISTE\_REUNIONS|{donnees\_reunions\_json\_ou\_delimite} 
- REJOINDRE\_OK|{id\_reunion}|{liste\_participants\_json\_ou\_delimite} 1 
- REJOINDRE\_ECHEC|{message\_erreur} 
- NOUVEAU\_MESSAGE\_CHAT|{id\_reunion}|{id\_emetteur}|{nom\_emetteur}|{cont enu\_message} 
- UTILISATEUR\_REJOINT\_REUNION|{id\_reunion}|{id\_utilisateur}|{nom\_utilisateur } 1 
- UTILISATEUR\_QUITTE\_REUNION|{id\_reunion}|{id\_utilisateur} 1 
- DEMANDE\_PAROLE\_ENTRANTE|{id\_reunion}|{id\_demandeur}|{nom\_demande ur} 
- PAROLE\_ACCORDEE|{id\_reunion}|{id\_utilisateur} 1 
- PAROLE\_REFUSEE|{id\_reunion}|{id\_utilisateur} 
- NOTIFICATION\_REUNION\_CLOTUREE|{id\_reunion} 1 
- VALEUR\_CONFIG|{cle\_config}|{valeur\_config} 
- LISTE\_UTILISATEURS|{donnees\_utilisateurs\_json\_ou\_delimite} 
- ERREUR|{code\_erreur}|{message\_descriptif} 
- **Centralisation des Constantes de Protocole :** Dans une classe ConstantesProtocole.java (module bmo-commun). 1

  Un protocole textuel simple est facile à implémenter mais peut manquer de robustesse et d'extensibilité. 1 Une évolution vers JSON sur TCP ou gRPC est recommandée pour le long terme, et RTP/SRTP sur UDP pour les médias. 1 

**Tableau : Commandes Principales du Protocole de Communication BMO (en français)** 



|**Type** |**Commande/Répons e** |**Paramètres (Exemples Clés)** |**Description Succincte** |
| - | :- | :- | :- |
|Requête |CONNEXION |nom\_utilisateur\_ou\_e mail, mot\_de\_passe |Tente d'authentifier l'utilisateur. |
|Réponse |AUTH\_OK |id\_utilisateur, nom\_utilisateur, role\_applicatif |Authentification réussie. |
|Réponse |AUTH\_ECHEC |message\_erreur |Échec de l'authentification. |
|Requête |NOUVELLE\_REUNION |titre, agenda, datetime\_iso8601, duree\_min, type |Demande de création d'une nouvelle réunion. |
|Notif. |REUNION\_CREEE |id\_reunion, titre, ...autres\_details |Notification de création de réunion. |
|Requête |REJOINDRE |id\_reunion |Demande pour rejoindre une réunion. |
|Réponse |REJOINDRE\_OK |id\_reunion, liste\_participants\_jso n |Autorisation de rejoindre, avec liste des participants. |
|Requête |MESSAGE\_CHAT |id\_reunion, contenu\_message |Envoi d'un message chat. |



|Notif. |NOUVEAU\_MESSAGE \_CHAT |id\_reunion, id\_emetteur, nom\_emetteur, message |Notification d'un nouveau message chat. |
| - | :- | :- | :- |
|Requête |DEMANDER\_PAROLE |id\_reunion |Demande de prise de parole. |
|Notif. |PAROLE\_ACCORDEE |id\_reunion, id\_utilisateur |Notification : parole accordée. |
|Requête |ADMIN\_OBTENIR\_UTI LISATEURS |(aucun) |Demande (admin) de la liste des utilisateurs. |
|Réponse |LISTE\_UTILISATEURS |liste\_utilisateurs\_json |Réponse contenant la liste des utilisateurs. |
|Notif. |UTILISATEUR\_REJOIN T\_REUNION |id\_reunion, id\_utilisateur, nom\_utilisateur |Notification : nouveau participant. |
|Notif. |UTILISATEUR\_QUITTE \_REUNION |id\_reunion, id\_utilisateur |Notification : participant parti. |

*(Basé sur 1 et commandes déduites)* 

3. **Gestion des Connexions Multiples Côté Serveur** 
- **Modèle de Gestion des Threads :** Le serveur gère multiples connexions clientes simultanément. Chaque connexion est prise en charge par un thread dédié issu d'un pool de threads (ExecutorService). 1 Une instance de GestionnaireClient.java est associée à chaque thread. 1 
- **Recommandation : Pool de Threads (ExecutorService) :** Plus robuste et performant que "un thread par client", limitant la consommation de ressources. 
- **API de Socket Bloquante :** Utilisation de l'API de socket bloquante standard de Java. 1 
4. **Interaction avec la Base de Données (MySQL via JDBC)** 
- **SGBD :** MySQL pour la persistance des données. 1 
- **Connectivité :** Via JDBC et le pilote MySQL Connector/J. 1 
- **Structure des Tables Principales et Relations (Exhaustive) :** 1 
- **Table utilisateurs : Informations sur les utilisateurs** 
- id (INT, Clé Primaire, Auto-Incrémenté) : Identifiant unique. 
- identifiant\_connexion (VARCHAR(50), Unique, Non Nul) : Nom d'utilisateur ou email. 
- mot\_de\_passe (VARCHAR(255), Non Nul) : Mot de passe haché et salé. 
- nom\_complet (VARCHAR(100), Non Nul) : Nom complet ou d'affichage. 
- role\_systeme (ENUM('UTILISATEUR', 'ADMINISTRATEUR'), Non Nul, Défaut 'UTILISATEUR') : Rôle système. 
- date\_creation\_compte (DATETIME, Non Nul, Défaut CURRENT\_TIMESTAMP) : Date de création. 
- derniere\_connexion (DATETIME, Peut être Nul) : Date de la dernière connexion. 
- statut\_compte (ENUM('ACTIF', 'INACTIF', 'BLOQUE'), Non Nul, Défaut 'ACTIF') : Statut du compte. 
- **Table reunions : Informations sur les réunions** 
- id (INT, Clé Primaire, Auto-Incrémenté) : Identifiant unique. 
- titre (VARCHAR(200), Non Nul) : Titre de la réunion. 
- ordre\_du\_jour (TEXT, Peut être Nul) : Description détaillée. 
- date\_heure\_debut (DATETIME, Non Nul) : Date et heure de début. 
- type\_reunion (ENUM('STANDARD', 'PRIVEE', 'DEMOCRATIQUE'), Non Nul, Défaut 'STANDARD') : Type. 
- duree\_minutes (INT, Non Nul) : Durée prévue. 
- statut\_reunion (ENUM('PLANIFIEE', 'OUVERTE', 'CLOTUREE', 'ANNULEE'), Non Nul, Défaut 'PLANIFIEE') : Statut. 
- organisateur\_id (INT, Non Nul, Clé Étrangère vers utilisateurs.id) : Organisateur. 
- mot\_de\_passe\_reunion (VARCHAR(255), Peut être Nul) : Mot de passe pour réunions privées. 
- date\_creation\_reunion (DATETIME, Non Nul, Défaut 

  CURRENT\_TIMESTAMP) : Date de création. 

- **Table participants\_reunion : Participation aux réunions** 
- reunion\_id (INT, Clé Primaire partielle, Clé Étrangère vers reunions.id) 
- utilisateur\_id (INT, Clé Primaire partielle, Clé Étrangère vers utilisateurs.id) 
- statut\_participation (ENUM('INVITE', 'ACCEPTE', 'REFUSE', 'REJOINT', 'PARTI', 'EXCLU'), Non Nul) 
- role\_dans\_reunion (ENUM('ORGANISATEUR', 'PARTICIPANT', 'ANIMATEUR'), Non Nul, Défaut 'PARTICIPANT') 
- heure\_entree (DATETIME, Peut être Nul) 
- heure\_sortie (DATETIME, Peut être Nul) 
- (Clé Primaire Composite : (reunion\_id, utilisateur\_id)) 
- **Table messages\_chat : Historique des messages de chat** 
- id (BIGINT, Clé Primaire, Auto-Incrémenté) 
- reunion\_id (INT, Non Nul, Clé Étrangère vers reunions.id) 
- utilisateur\_id (INT, Non Nul, Clé Étrangère vers utilisateurs.id) 
- contenu\_message (TEXT, Non Nul) 
- horodatage (DATETIME, Non Nul, Défaut CURRENT\_TIMESTAMP) 
- **Table parametres\_application : Configuration de l'application** 
- cle\_parametre (VARCHAR(100), Clé Primaire) 
- valeur\_parametre (VARCHAR(255), Peut être Nul) 
- description\_parametre (TEXT, Peut être Nul) 
- **Couche d'Accès aux Données (DAO - Objet d'Accès aux Données) :** Utilisation du motif DAO (ex: UtilisateurDAO.java, ReunionDAO.java). 1 
- **Intégrité Référentielle et Types ENUM :** Utilisation de clés étrangères et de types ENUM MySQL. 1 Les valeurs des ENUMs seront en français. 

**Tableau : Structure Détaillée de la Base de Données BMO (en français) Table: utilisateurs** 



|**Nom de Colonne** |**Type de Données (MySQL)** |**Contraintes (PK, FK, Unique, Not Null, Default)** |**Description** |
| - | :- | :- | - |
|id |INT |PK, AUTO\_INCREMENT, NOT NULL |Identifiant unique. |
|identifiant\_connexion |VARCHAR(50) |UNIQUE, NOT NULL |Identifiant de connexion. |
|mot\_de\_passe |VARCHAR(255) |NOT NULL |Mot de passe haché. |
|nom\_complet |VARCHAR(100) |NOT NULL |Nom complet. |
|role\_systeme |ENUM('UTILISATEUR',' ADMINISTRATEUR') |NOT NULL, DEFAULT 'UTILISATEUR' |Rôle système. |
|date\_creation\_compt|DATETIME |NOT NULL, DEFAULT CURRENT\_TIMESTAM|Date de création. |



|e ||P ||
| - | :- | - | :- |
|derniere\_connexion |DATETIME ||Date dernière connexion. |
|statut\_compte |ENUM('ACTIF','INACTI F','BLOQUE') |NOT NULL, DEFAULT 'ACTIF' |Statut du compte. |

**Table: reunions** 



|**Nom de Colonne** |**Type de Données (MySQL)** |**Contraintes (PK, FK, Unique, Not Null, Default)** |**Description** |
| - | :- | :- | - |
|id |INT |PK, AUTO\_INCREMENT, NOT NULL |Identifiant unique. |
|titre |VARCHAR(200) |NOT NULL |Titre. |
|ordre\_du\_jour |TEXT ||Ordre du jour. |
|date\_heure\_debut |DATETIME |NOT NULL |Date et heure de début. |
|type\_reunion |ENUM('STANDARD','P RIVEE','DEMOCRATIQ UE') |NOT NULL, DEFAULT 'STANDARD' |Type. |
|duree\_minutes |INT |NOT NULL |Durée en minutes. |
|statut\_reunion |ENUM('PLANIFIEE','O UVERTE','CLOTUREE',' ANNULEE') |NOT NULL, DEFAULT 'PLANIFIEE' |Statut. |
|organisateur\_id |INT |NOT NULL, FK (utilisateurs.id) |Organisateur. |
|mot\_de\_passe\_reunio n |VARCHAR(255) ||Mot de passe (réunions privées). |



|date\_creation\_reunio n |DATETIME |NOT NULL, DEFAULT CURRENT\_TIMESTAM P |Date création. |
| :- | - | - | - |

**Table: participants\_reunion** 



|**Nom de Colonne** |**Type de Données (MySQL)** |**Contraintes (PK, FK, Unique, Not Null, Default)** |**Description** |
| - | :- | :- | - |
|reunion\_id |INT |PK (part), FK (reunions.id), NOT NULL |Identifiant réunion. |
|utilisateur\_id |INT |PK (part), FK (utilisateurs.id), NOT NULL |Identifiant utilisateur. |
|statut\_participation |ENUM('INVITE','ACCE PTE','REFUSE','REJOI NT','PARTI','EXCLU') |NOT NULL |Statut participation. |
|role\_dans\_reunion |ENUM('ORGANISATE UR','PARTICIPANT','AN IMATEUR') |NOT NULL, DEFAULT 'PARTICIPANT' |Rôle dans la réunion. |
|heure\_entree |DATETIME ||Heure d'entrée. |
|heure\_sortie |DATETIME ||Heure de sortie. |

*(Basé sur 1 et enrichissements pour exhaustivité)* 

3. **Arborescence de Projet Optimisée** 

Une structure de projet Maven multi-modules est préconisée. Les noms de fichiers et de classes Java principaux seront en français. 

1. **Structure Maven Multi-Modules** 
- **bmo (Parent) :** pom.xml parent. 
- **bmo-commun :** Code partagé (DTOs, constantes, utilitaires). 
- **bmo-serveur :** Logique serveur. 
- **bmo-client :** Application cliente JavaFX. 1 
2. **Nomenclature des Packages Java** 

Strictement : akandan.bahou.kassy.<nom\_module\_en\_francais>.<sous\_module\_ou\_classe\_principale\_en\_fra ncais>. 

Exemples : 

- akandan.bahou.kassy.commun.dto.UtilisateurDTO.java 
- akandan.bahou.kassy.serveur.gestionutilisateurs.ServiceGestionUtilisateurs.java 
- akandan.bahou.kassy.client.vues.controleurs.ControleurConnexion.java 
3. **Structure Conceptuelle des Fichiers Sources (Respectant la contrainte de 12 éléments visibles dans src)** 

   La liste suivante présente les 12 éléments principaux de l'arborescence des sources du projet, en se concentrant sur les répertoires et fichiers essentiels dans src/main/java et src/main/resources pour chaque module. Les fichiers de configuration racines comme pom.xml, Dockerfile.\*, .gitignore, README.md ne sont pas comptés dans cette limite. Bien que cette vue soit de haut niveau, la conception permet l'utilisation d'autant de fichiers FXML que nécessaire pour une interface utilisateur riche, organisés au sein de la structure fxml/. 

   **Tableau : Arborescence de Projet Conceptuelle pour BMO (12 Éléments Clés des Sources)** 

 

|**N°** |**Chemin** |**Description Concise du Rôle** |
| - | - | - |
|1 |bmo-commun/src/main/java/a kandan/bahou/kassy/commun/ dto/ |Contient les Objets de Transfert de Données (ex: UtilisateurDTO.java, ReunionDTO.java) partagés entre le client et le serveur. |
|2 |bmo-commun/src/main/java/a kandan/bahou/kassy/commun/ utilitaires/ |Regroupe les classes utilitaires communes, comme ConstantesProtocole.java pour les définitions du protocole de communication. |
|3 |bmo-serveur/src/main/java/ak andan/bahou/kassy/serveur/n|Paquetage principal du noyau serveur, incluant la classe de démarrage |



||oyau/ |(ServeurPrincipal.java) et les gestionnaires de connexions (GestionnaireClient.java). |
| :- | - | - |
|4 |bmo-serveur/src/main/java/ak andan/bahou/kassy/serveur/se rvices/ |Couche des services métier du serveur (ex: ServiceGestionUtilisateurs.jav a, ServiceGestionReunions.java), encapsulant la logique applicative. |
|5 |bmo-serveur/src/main/java/ak andan/bahou/kassy/serveur/p ersistance/ |Couche d'accès aux données du serveur, avec les Entités et les DAOs (ex: UtilisateurDAO.java, ReunionDAO.java) pour l'interaction avec MySQL. |
|6 |bmo-serveur/src/main/resourc es/application.properties |Fichier de configuration principal du serveur BMO, contenant les paramètres de l'application (port, base de données, etc.). |
|7 |bmo-client/src/main/java/akan dan/bahou/kassy/client/coeur/ |Paquetage principal du client, contenant la classe de démarrage de l'application JavaFX (AppPrincipale.java) et l'initialisation de l'UI. |
|8 |bmo-client/src/main/java/akan dan/bahou/kassy/client/servic es/ |Services côté client, comme ServiceReseau.java pour la communication avec le serveur et ServiceSessionUtilisateur.java pour gérer la session locale. |
|9 |bmo-client/src/main/java/akan dan/bahou/kassy/client/vues/c ontroleurs/ |Paquetage pour les classes de contrôleurs JavaFX (ex: ControleurConnexion.java, ControleurTableauDeBord.jav a), liant les vues FXML à la logique applicative. |



|10 |bmo-client/src/main/java/akan dan/bahou/kassy/client/model es/ |Contient les classes de modèle de données utilisées par l'interface client pour l'affichage et la gestion de l'état (ex: ModeleUtilisateurConnecte.ja va). |
| - | - | :- |
|11 |bmo-client/src/main/resource s/akandan/bahou/kassy/client/ vues/fxml/ |Répertoire contenant les fichiers FXML (ex: VueConnexion.fxml, VueTableauDeBord.fxml) définissant la structure des interfaces utilisateur. |
|12 |bmo-client/src/main/resource s/akandan/bahou/kassy/client/ vues/css/ |Répertoire pour les feuilles de style CSS (ex: style.css) utilisées pour personnaliser l'apparence de l'application cliente JavaFX. |

4. **Intégration Docker & Maven** 

L'utilisation combinée de Maven et Docker est impérative pour la portabilité, la cohérence des environnements et la simplification du déploiement. 1 

1. **Structure du pom.xml (Parent et Modules)** 
- **pom.xml Parent (bmo/pom.xml) :** 
- Déclaration des modules (bmo-commun, bmo-serveur, bmo-client). 
- Gestion centralisée des dépendances (<dependencyManagement>) et plugins (<pluginManagement>). 
- Propriétés communes (version Java, encodage). 
- **pom.xml des Modules :** 
- **bmo-commun/pom.xml :** Dépendances minimales pour DTOs, constantes. Packaging jar. 
- **bmo-serveur/pom.xml :** Dépendance vers bmo-commun. Dépendances serveur (MySQL Connector/J, HikariCP). Plugin pour JAR exécutable (ex: maven-shade-plugin) avec classe principale akandan.bahou.kassy.serveur.noyau.ServeurPrincipal. 
- **bmo-client/pom.xml :** Dépendance vers bmo-commun. Dépendances JavaFX. Plugin pour JAR exécutable (ex: maven-shade-plugin ou 

  javafx-maven-plugin) avec classe principale akandan.bahou.kassy.client.coeur.AppPrincipale. 

2. **Dockerfile pour le Serveur (bmo/Dockerfile.serveur)** 
- **Contenu type :**

  Dockerfile

  FROM openjdk:17-jdk-slim 

  ARG JAR\_FILE=bmo-serveur/target/bmo-serveur-\*.jar ARG APP\_PORT=5000

  WORKDIR /opt/bmo

  COPY ${JAR\_FILE} app-serveur.jar

  EXPOSE ${APP\_PORT} ENTRYPOINT

- **Rôle :** Crée une image Docker standardisée, légère et autonome pour le serveur BMO. 1 
3. **Dockerfile pour le Client JavaFX (bmo/Dockerfile.client)** 
- **Contenu type :**

  Dockerfile

  FROM openjdk:17-jdk-slim 

  ARG JAR\_FILE=bmo-client/target/bmo-client-\*.jar 

  RUN apt-get update && apt-get install -y \ 

  `    `libgtk-3-0 libxtst6 libxrender1 libasound2 libgl1-mesa-glx libgl1-mesa-dri \     && rm -rf /var/lib/apt/lists/\*

  WORKDIR /opt/bmo

  COPY ${JAR\_FILE} app-client.jar

  ENTRYPOINT ["java", "-jar", "app-client.jar"]

- **Rôle :** Crée une image Docker pour l'application cliente JavaFX, incluant les dépendances graphiques pour Linux. 1 
- **Affichage via X11 Forwarding :** Nécessite un serveur X sur l'hôte, montage du socket X11, variable DISPLAY, et autorisation xhost. 1 
4. **Orchestration avec docker-compose.yml (bmo/docker-compose.yml)** 

Ce fichier orchestre le lancement de l'application (serveur, client, base de données MySQL, et PhpMyAdmin pour la gestion de la base de données). 

- **Contenu type (Exemple) :**

  YAML version: '3.8'

  services:

bmo-db:

image: mysql:8.0

container\_name: bmo\_mysql\_db

restart: unless-stopped

environment:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGMAAAAXCAYAAAAfiPFCAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAMJJREFUaIHt2bFpA1EQRdH7vhY7ESh2O47cgBpxJ87VwZahXJEbMTgQGM1zshhnynYH9E428IOBy0Qfog0BfH1f38x4t2u39UKPRBqFaz7sn0+SagIQ48X4VVJirMqFdGE5irHxNvFPYjSSGI0kRiOJ0UhiNJIYjSRGI4nRSGI0khiNJEYjidFIYjSSGI0kRiMTwI0bYhTLJ0esxoX/hgngSbvPH/tDKJeyIoORz0DBcgm2Nc+M43HT3R5VSfL9Z7GqX3QFKILO8khdAAAAAElFTkSuQmCC)

MYSQL\_ROOT\_PASSWORD: MotDePasseRootFort!

MYSQL\_DATABASE: bmo\_base\_de\_donnees

MYSQL\_USER: bmo\_utilisateur\_app

MYSQL\_PASSWORD: MotDePasseUtilisateurAppFort! ports:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHIAAABDCAYAAABA+1F1AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAfZJREFUeJzt3cFuEmEUhuH3zAwEQ0y6cO2q92LsBZC40dvSazBxb0y8hcZNN/UWNIjaSGf6HxcwdFvNhOl8fM8KWB14GRLCyQ+YhADIzPrqino+392fusvL991qtbobe45jCoAfv25fF/JtFVRjDzSELOXi7OmTz2PPcUwNQEAdMM+kHnugQURoPI9/IHEFmkPKcEgRDinCIUU4pAiHFOGQIhxShEOKcEgRDinCIUU4pAiHFHEIGZBjDjKkpqllnstDNQBJ3iaxBo0fltu268ae4dh2GwJl+zHrxQvQ2Nkp25vrsWcw+y/9Fl3sb0tckSciASKiwP6j9efv9k1GvpNZvjoNCXzIzFcRUZr9g7GP6JDTUcj7bx3+HinCIUU4pAiHFOGQIhxShEOKcEgRDinCIUU4pAiHFOGQIhxShEOKcEgRDUBV5Z9S8htUDjsdScWmv9MAlG77iXrxEu/sTElSlzX97s7h0d0Clk1MRNyHVDuLbgjn5xTgrt9Se+wkz6IbQsn8DtXF2XL2pX/XP2aaZ9ENIGBG19YwG3uUB/EVKMIhRTikCIcU4ZAiHFKEQ4pwSBEOKcIhRTikCIcU4ZAiHFKEQ4qQPItuMLPZZF4TybPohhDBuiST+evCANhsNs9KvXiOt+gOmsx2uZx/jYibsWexE/IXetl/61xbKHwAAAAASUVORK5CYII=)

- "3306:3306"

volumes:

- bmo\_db\_volume:/var/lib/mysql

networks:![ref1]

- bmo\_app\_reseau

phpmyadmin:

image: phpmyadmin/phpmyadmin

container\_name: bmo\_phpmyadmin

restart: unless-stopped

depends\_on:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGIAAABDCAYAAABnVdCdAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAcBJREFUeJzt3DFOG0EYhuH3n92YgihNqnAbkCKlpbAPgpRjpEC5A82egEtQAT03CBFIjpydP4UdaFlhvB/r76ncjPTLr3Zc7HjAJATA/cPyW1LOMmsz9kD7IqJkZD5++jg7jYjaAgTlS5InEeEQO5Mk/Oq69cNQxh7H1hxChEOIcAgRDiHCIUQ4hAiHEOEQIhxChEOIcAgRDiHCIUQ4hAiHENEC9PQEpbJ5Y2c7k/M5CZsQs2iuV5nnQfgJ2aGkX8I6RABkZnQdZT4fda6903WwWEQPzyHK5rO3pu2oEVGHLGgBfj/++ZrEGd6atiL6+iMzLyMiX7qmBajkUYlynJnt2423P6IpF0PXtAANDfV5e7JX6mP49+itSIRDiHAIEQ4hwiFEOIQIhxDhECIcQoRDiHAIEQ4hwiFEOIQIhxDhUxxvoEle/GbuvxbgoDQ3q8pP/IRsRfZ/b+HD2GOYvWOTvItjVsr3w8PZ1dhzDDHJuzhWWT+PPcNQ/nEW4RAiHEKEQ4hwCBEOIcIhRDiECIcQ4RAiHEKEQ4hwCBEOIcIhRDiEiKme4hh8imJs07yLo8+7sUcYaqp3cdQh//o3e/IP6INyExayqAEAAAAASUVORK5CYII=)

- bmo-db

ports:

- "8081:80" # Port 8081 sur l'hôte pour éviter conflit avec d'autres services sur 8080

environment:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJoAAABDCAYAAABkzhzwAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAfZJREFUeJzt3T+KFFEURvFzqwsVFMZUF2KuILiBwX3MBoZegIGZgTuYRAMD40nFyBW4A0UHdZh3DeYPaGr319J1flDQlb3g9Kvuph8XpIAC+PLtx7NmOuoeq10vaEmqpkGPk4N7t19X1dj1erZpBiimB00/qSpDi+pB1Ueu3vD7bNr1ArQMhqYIQ1OEoSnC0BRhaIowNEUYmiIMTRGGpghDU4ShKcLQFGFoijA0RRiaImaACy4opsEC/oD3n+lB73oNETPArVp9Ou9+WZQ7XFBDU30K7PXfuOFqB+vuOjlhOjzc9XIWaVTV3m9r16FNV699dGpTGujrQzczwNfvP582dYSPTm1I0WMMXnX3m6rqGWDQD6eaHndf3kv/quFiqn63XlNATwArVn8/Pr28/vkaRR0fA/g7mkIMTRGGpghDU4ShKcLQFGFoijA0RRiaIgxNEYamCENThKEpwtAUYWiK8BSUtqKqRo/u9Xp9eQ9wdvbr0fngOe5w2pwxVvX2/p35dAmHbyQtSYGzoHSpoKnV+4O784tNz6ZyFpRuVDGa/swWvhT64V8RhqYIQ1OEoSnC0BRhaIowNEUYmiIMTRGGpghDU4ShKcLQFGFoijA0RRiaIjwFpRvdNLWd4VTOgtKNhq7RH9brzcdW4Cwo/WERs6m0p34DStKLTxmfJ1IAAAAASUVORK5CYII=)

PMA\_HOST: bmo-db

PMA\_PORT: 3306

MYSQL\_ROOT\_PASSWORD: MotDePasseRootFort!

networks:![ref2]

- bmo\_app\_reseau

bmo-serveur:

build:![ref3]

context:.

dockerfile: Dockerfile.serveur container\_name: bmo\_app\_serveur restart: unless-stopped

depends\_on:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHoAAACECAYAAABfwT+NAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAxlJREFUeJzt3bFqFFEUxvHv3J0kQoIKVvoaPoGCYGuxPogg+BIWYmttM08gltZJY7SzsLFWUVHj3GOhG8EqrtmZnfv9f1VSzWX/7GWXHE4kWAhJ+vDp6+1UuZdZF1MfyEVEycj8fPFg905E1E0/r5OkULmaypsRQejRpFJ63/e/3mybVsZ4CKZHaBOENkFoE4Q2QWgThDZBaBOENkFoE4Q2QWgThDZBaBOENkFoE50kDRoUKlUa54/gOJXLpXKMB3WStBuL45PMR6HgHT6i1PBVGid0SFJmRt+rLJdjPBIrfS/dvRvDGM9ahS6/f+bqPh91jIG/f9FJ0sfP326l4p64us9FDPVhZj6LiFGu5bPoJKkqr5UoNzKzm/pALYhFeTr1Gf7WSdJCC9U/1zf+0xDb9zpyVZsgtAlCmyC0CUKbILQJQpsgtAlCmyC0CUKbILQJQpsgtAlCm2AKdAMWOc7A37/oJGmvLF6dVD0W7/BzkcOP19LO1McA0Kwmd4HulnJ/f3/3aOpzbJMmd4GeZL0y9Rm2DR++TBDaBKFNENoEoU0Q2gShTRDaBKFNENoEoU0Q2gShTRDaBKFNENpEq1OgWzeFObU2d4EO+XbqI2ybVneB1m3a2rcN2AXarpSUq52k7AJtVn6v0oPMPI6IZBdoqyK+dCUur35lF2ijMrMMP4b4nZjv0S4IbYLQJghtgtAmCG2C0CYIbYLQJghtgtAmCG2C0CYIbYLQJlqdArUXoVoW3encHLtAG5WZJ/Xb8E4XGBoC2tPkLtC5iChVWftLB3tPVmO5m9LkLtD5yKqII43wIZgPXyYIbYLQJghtgtAmCG2C0CYIbYLQJghtgtAmCG2C0CYIbYLQJghtginQaWUdaW1pm7tAZyKlVOQLSRsdI5La3QU6J6PsLWUX6Hqqfu3ZnM1iWXaBriGll8/3d+5LGqY+y1mxC3QdmTvLmd1+7AJdTxwezuu14qo2QWgThDZBaBOENkFoE4Q2QWgThDZBaBOENkFoE4Q2QWgThDbBFOh68vr1ef3XeXaBrqEq3/R9P6vQMPETLTf3PS1bXVIAAAAASUVORK5CYII=)

- bmo-db

ports:

- "5000:5000"

environment:

DB\_URL: jdbc:mysql://bmo-db:3306/bmo\_base\_de\_donnees?useSSL=false&allowPublicKeyRetrieval=true& serverTimezone=UTC

DB\_USERNAME: bmo\_utilisateur\_app

DB\_PASSWORD: MotDePasseUtilisateurAppFort! networks:![ref2]

- bmo\_app\_reseau

bmo-client:

build:![ref3]

context:.

dockerfile: Dockerfile.client container\_name: bmo\_app\_client

restart: "no"![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKcAAABvCAYAAACJrdA1AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAt5JREFUeJzt3bFqk1EYxvHnPfmaSjt01snBzatQEFwdsgreQsG9FyCIOIpXkOtwdqmzDg5OYktqtW3O69AWERooNMl5evz/pmQ7CX++k8A5vBJgKiTpYPbraarsZtZR6wX1KqJkKL+9fjV+vrcXtfV6boOQpMPZyYuqfCeJOFcnJX3Z2R4/iCDO6yitFwAsQpywRZywRZywRZywRZywRZywRZywRZywRZywRZywRZywRZywRZywRZywRZywNUjSXHOFStXF4WOsSGa2XsJtMkjSOEb7p5lvQsGTdEWqpCz6rvMT8biGkKTMjOlUZTJpvZzuJVc0ru8yznLxmm19vTIi5q0X4WqQpMOj309SsSu29bWKzKPMfBYRbPVXGCSpKu+VKI8yc2i9oP9IpnSg892KOK8wSNJII9W/WzvWIyTFdMp3vgjbOGwRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2xx+7KtnEw4aLzIIEmbZfTptOqteJKuVUYei1PwAIClYfZlJ0qU/a/bGy8fRpy0XsuyDJIUKndT+TgiiPOWqlk3tzr7z9DVh0FfiBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2iBO2uH3Zj+4uyjH7shMxis/3pbPW61gmZl/2o7u5msy+bK/qPKzutuWbYvZlW6mYT3e27rxXh78Zb4rZl21VZXwUO9aVmH3ZVvCVL8Y2DlvECVvECVvECVvECVvECVvECVvECVvECVvECVvECVvECVvECVvECVvECVvcvmwrVTgBvwizL9vK+Vn9IK5oAACWgtmXsBFR6s72xiQifkjMvoSRiJzPZhpfvucPEGwRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2wRJ2xx+xI2Mv+96MfsS9hIKY+P9fPyPbMvYSVCVWLUIsz9Ad5ZyZOqPkIfAAAAAElFTkSuQmCC)

depends\_on:

- bmo-serveur

environment:

- DISPLAY=${DISPLAY} BMO\_SERVER\_ADDRESS: bmo-serveur BMO\_SERVER\_PORT: 5000

volumes:![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEoAAAAXCAYAAACswNlYAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAMhJREFUWIXt2DFuwkAURdH7PB4rkpsU1KlYDYtgXWwrDRVrQMRCKSDMTxFw/Ts70jvd756uphqwFAFERDkeKcPwdxtstzTgIanBM9TX9bZvxKET3aLrVqRFnKHbvY/1U1L0AIIiGCIoSw9cC0Hl516gAvgFZTlUkkMlOVSSQyU5VJJDJTlUkkMlOVSSQyU5VJJDJTlUkkMlzaEEseSQVap1btIDBHELdAF/3L1IXFrwmG+AaZo2rbx9vG6DPuI+jsNJ0vfSW/6VX+z5LB1bUDuaAAAAAElFTkSuQmCC)

- /tmp/.X11-unix:/tmp/.X11-unix:rw

networks:![ref1]

- bmo\_app\_reseau

networks: bmo\_app\_reseau:

driver: bridge![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGYAAAAXCAYAAAD5oToGAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAANdJREFUaIHt2TGKAkEUhOGqHhk3EDH1OkZeoFNhr+ANDMR4w42MpS/hIQy8hQgDLqwztdEaiWk/sL6ss4Kfjh5gIREArt3PUkhraWhqD3pXZOrVcDf7GB1JagQARJoLWpB0mEpI3Yf+vt9stgSgVHuQPecwQTlMUA4TlMME5TBBOUxQDhOUwwTlMEE5TFAOE5TDBOUwQTlMUA4TFAHg0t0+ifQNwIeyenoKq+mkPTwumC2b06/0RdA/qBJBQzNuz/9vAoAkloKUc71h766UgpzzQFK1t9gLf2dzMPigODrwAAAAAElFTkSuQmCC)

volumes:

bmo\_db\_volume:

- **Rôle :** Simplifie la gestion de l'environnement de développement et de test, permettant de construire et lancer tous les services avec docker-compose up. 1 Facilite le partage d'un environnement cohérent. L'ajout de PhpMyAdmin offre 

  une interface web pour gérer facilement la base de données MySQL. 

- **Sécurité des Identifiants :** Les identifiants en clair sont pour le développement local. Utiliser des secrets Docker en production. 
5. **Avantages de la Combinaison Docker & Maven** 
- **Portabilité Supérieure :** Docker encapsule les artefacts Maven et leurs dépendances. 
- **Cohérence des Environnements :** Identique pour tous les développeurs et tous les stades (développement, test, production). 
- **Gestion Simplifiée des Dépendances :** Maven pour Java, Docker pour le système. 
- **Facilitation des Échanges et Collaboration :** Processus standardisé (mvn install, docker-compose up). 
- **Isolation des Services :** Chaque composant (BDD MySQL, PhpMyAdmin, serveur, client) dans son conteneur. 
- **Préparation à la Scalabilité :** Base pour orchestrateurs (Kubernetes, Swarm). 

L'utilisation de MySQL comme base de données, gérée via Docker et accessible via PhpMyAdmin, combinée à la puissance de Maven pour le build et Docker pour la conteneurisation, résout efficacement les problèmes de compatibilité entre les environnements de développement et de production. Elle assure que chaque développeur travaille avec la même pile logicielle et les mêmes configurations, simplifiant ainsi les échanges, le débogage et le déploiement final de l'application BMO. 

**Conclusion** 

Ce rapport de conception pour BMO établit une fondation technique détaillée, visant une architecture moderne, robuste et évolutive. L'accent sur la modularité, la sécurité (avec la recommandation forte d'activer TLS/SSL par défaut), et la facilité de déploiement via Java, Maven et Docker, positionne BMO comme une solution d'entreprise de nouvelle génération. L'utilisation exclusive du français pour les éléments de code et la structure de base de données exhaustive, incluant l'accès via PhpMyAdmin, renforcent la clarté et l'adaptabilité du projet. 

Les points d'attention concernant le protocole de communication, la performance du serveur sous forte charge multimédia, et l'expérience utilisateur du client JavaFX conteneurisé restent pertinents. Les recommandations formulées visent à guider une implémentation de haute qualité pour assurer la robustesse, la maintenabilité et la 

pérennité de BMO. 

**Sources des citations** 

1\.  Cahier des Charges Résumé.pdf 

[ref1]: data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAI8AAAAuCAYAAAARI0rYAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAXtJREFUeJzt20FKw1AUheFzk7QWBZ04duRCuhBR3JVuQVxEV9GpuAClVgWp+o4DE3WgEa9CCv7fKMnoEX5eINwnAUkhSbbr+Vz1ePx6j8/t78uSniWViPDQ6xlaSNLN3eqwyCdVqBp6QWvOcpxtb42OI6IMvZihNZIUUh3S2FY99ILWWUQU281sNqsk/ft42GmQRjxIIx6kEQ/SiAdpxIM04kEa8SCNeJBGPEgjHqQRD9KIB2nEgzTiQdpbPCH9+8m479i8oo8aSbK8smIhMQzWy7bk++l0SkVqx1CXy+VuqSd73T2+5I1qdD2Z6JIZZuAXutMT0V6z86CPJbnbdRtJur1/PHL4lAF49ImI4vCB7fOIKE33vA2HePAl26F4/zrxnwdpxIM04kEa8SCNeJBGPEgjHqQRD9KIB2nEgzTiQRrxII14kEY8SCMepBEP0hpJqio/lOIrqSIm9Cj2U6zUHtPi9AR+wtocXexIi4jwWyztEDzQiyNH+BMv2mVnAAIXkB4AAAAASUVORK5CYII=
[ref2]: data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAI8AAAAsCAYAAABc6+vTAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAXRJREFUeJzt2zGKVEEURuFz6z1GEEFTXU1rohuYjcwKZhIxFWNXMHuQdgUmmrgDQ8FAGPr9Bj3YBnYj1+CNeL6oKrvBoSqpAqmpAL5++/4ijItkmdYe6C4rCKPePbx/9qqqlrXnWdsMUIzHIc+qynhOqKolS75st9sB/PfxjLUH0L/LeNRmPGozHrUZj9qMR23GozbjUZvxqM141GY8ajMetRmP2oxHbcajNuNR2wywY0cxFm5fFur3kiRFNptN1p7lLpgBzmr6eJO8LsqT6IRAxpQP+6UKIEldXzPOz9ce5+67uiKXlxUM6Gc843bttaWjtsAGdlUVOPyeeB7qAq8tnZakXj56ML+vqswAC3kyajxN9nvpiKXIW/aHzj6WiYnlcHVJxxS/fM7ymlKb8ajNeNRmPGozHrUZj9qMR23GozbjUZvxqM141GY8ajMetRmP2oxHbf6e0B+rYsnusJ8B7o3p083CGzyJdEpIBp/x8b/+1g+KYF4a6P7kxQAAAABJRU5ErkJggg==
[ref3]: data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAAsCAYAAADy8T8XAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAOxAAADsQBlSsOGwAAAV9JREFUaIHt2TFOAzEQheE3s06ygEC0cBto4AI5BV0K6uQIiGPkOqSEEgpEEUFBslo/CjZKCWKQo43eV2031i9bK9mAhBgALD8+rwmfkLna9YL+yit/TgeD2yOzl5JzEwAY/IzgpZn1NiDb/NSsVockzcxYaq6XGrSvFDBIAYMUMEgBgxQwSAGDFDBIAYMUMEgBgxQwSAGDFDBIAYMUMCgBQIsWBs/obqj7yNzyCMNiF6kbCQCGVj005J3BersjCb6t1+/Luj4pGtEAgKTN5/DxuOTof0cALHmdD2wDevfd2yNcGGczcDq1vHmVuyJsgh4f4bLYIvHmtK4fEwBk8NzNL0imXS+tJ5rkfgx0P5EKFfL2GMvPrGm+W+nIBilgkAIGKWCQAgYpYJACBilgkAIGKWCQAgYpYJACBilgkAIG7c2rXElmlhNBoAs48mrRZNxDO/JXSLTug9ddr2MvfAHc2VquV7npEQAAAABJRU5ErkJggg==
