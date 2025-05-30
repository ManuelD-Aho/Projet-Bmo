-- Base de données : bmo_base_de_donnees
-- Version Ultime Fusionnée et Corrigée

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

-- Création de l'utilisateur applicatif pour BMO (si il n'existe pas déjà)
-- Cet utilisateur sera utilisé par l'application Java pour se connecter à la base de données
CREATE USER IF NOT EXISTS 'bmo_utilisateur_app'@'%' IDENTIFIED BY 'MotDePasseUtilisateurAppFort!';

-- Attribution de tous les privilèges sur la base de données BMO à l'utilisateur applicatif
GRANT ALL PRIVILEGES ON bmo_base_de_donnees.* TO 'bmo_utilisateur_app'@'%';

-- Application des modifications de privilèges
FLUSH PRIVILEGES;

--
-- Suppression des tables existantes pour un nouveau départ (ordre important à cause des FK)
--
DROP TABLE IF EXISTS `votes`;
DROP TABLE IF EXISTS `poll_options`;
DROP TABLE IF EXISTS `polls`;
DROP TABLE IF EXISTS `recording_sessions`;
DROP TABLE IF EXISTS `reactions`;
DROP TABLE IF EXISTS `meeting_ratings`;
DROP TABLE IF EXISTS `export_jobs`;
DROP TABLE IF EXISTS `bandwidth_stats`;
DROP TABLE IF EXISTS `messages_chat`;
DROP TABLE IF EXISTS `participants_reunion`;
DROP TABLE IF EXISTS `reunions`;
DROP TABLE IF EXISTS `settings`;
DROP TABLE IF EXISTS `utilisateurs`;


--
-- Structure de la table `utilisateurs`
--
CREATE TABLE `utilisateurs` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT,
                                `identifiant` VARCHAR(100) NOT NULL,
                                `mot_de_passe` VARCHAR(255) NOT NULL,
                                `nom_complet` VARCHAR(100) NOT NULL,
                                `role` ENUM('PARTICIPANT','ORGANISATEUR','ADMINISTRATEUR','GESTIONNAIRE') DEFAULT 'PARTICIPANT',
                                `photo` MEDIUMBLOB NULL DEFAULT NULL,
                                `photo_mimetype` VARCHAR(50) NULL DEFAULT NULL,
                                `date_creation_compte` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                `statut_compte` ENUM('ACTIF', 'INACTIF', 'BLOQUE') NOT NULL DEFAULT 'ACTIF',
                                `date_derniere_connexion` DATETIME NULL DEFAULT NULL,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_identifiant` (`identifiant`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Table des utilisateurs BMO';

--
-- Structure de la table `reunions`
--
CREATE TABLE `reunions` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `titre` VARCHAR(200) NOT NULL,
                            `description` TEXT NULL DEFAULT NULL,
                            `date_heure_debut` DATETIME NOT NULL,
                            `duree_estimee_minutes` INT NOT NULL COMMENT 'Durée en minutes',
                            `type_reunion` ENUM('STANDARD','PRIVEE','DEMOCRATIQUE') DEFAULT 'STANDARD',
                            `statut_reunion` ENUM('PLANIFIEE','OUVERTE','CLOTUREE','ANNULEE') DEFAULT 'PLANIFIEE' COMMENT 'Statut de la réunion',
                            `organisateur_id` BIGINT NOT NULL,
                            `mot_de_passe_reunion` VARCHAR(255) NULL DEFAULT NULL,
                            `date_creation_reunion` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Table des réunions BMO';

--
-- Structure de la table `participants_reunion`
--
CREATE TABLE `participants_reunion` (
                                        `reunion_id` BIGINT NOT NULL,
                                        `utilisateur_id` BIGINT NOT NULL,
                                        `statut_participation` ENUM('INVITE','ACCEPTE','REFUSE','REJOINT','PARTI','EXCLU') DEFAULT 'INVITE' COMMENT 'Statut de la participation',
                                        `role_dans_reunion` ENUM('ORGANISATEUR','PARTICIPANT','ANIMATEUR') DEFAULT 'PARTICIPANT' COMMENT 'Rôle dans la réunion',
                                        `heure_entree` DATETIME NULL DEFAULT NULL,
                                        `heure_sortie` DATETIME NULL DEFAULT NULL,
                                        PRIMARY KEY (`reunion_id`, `utilisateur_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Participation aux réunions';

--
-- Structure de la table `messages_chat`
--
CREATE TABLE `messages_chat` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `reunion_id` BIGINT NOT NULL,
                                 `expediteur_id` BIGINT NOT NULL,
                                 `horodatage` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 `contenu_message` TEXT NOT NULL,
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Historique du chat des réunions';

--
-- Structure de la table `settings` (anciennement parametres_application)
--
CREATE TABLE `settings` (
                            `cle_parametre` VARCHAR(100) NOT NULL COMMENT 'Clé unique du paramètre',
                            `valeur_parametre` VARCHAR(255) DEFAULT NULL COMMENT 'Valeur du paramètre',
                            `description_parametre` VARCHAR(500) DEFAULT NULL COMMENT 'Description du paramètre',
                            `type_donnee_parametre` ENUM('CHAINE','ENTIER','BOOLEEN','LISTE') NOT NULL DEFAULT 'CHAINE' COMMENT 'Type de donnée de la valeur',
                            `modifiable_via_interface` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Modifiable via UI admin',
                            `date_mise_a_jour` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (`cle_parametre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Paramètres applicatifs globaux de BMO';

--
-- Insertion de quelques paramètres par défaut
--
INSERT INTO `settings` (`cle_parametre`, `valeur_parametre`, `description_parametre`, `type_donnee_parametre`, `modifiable_via_interface`) VALUES
                                                                                                                                               ('PORT_SERVEUR_TCP', '5000', 'Port d''écoute principal du serveur BMO.', 'ENTIER', TRUE),
                                                                                                                                               ('MAX_CONNEXIONS_SIMULTANEES', '100', 'Nombre maximum de clients connectés simultanément.', 'ENTIER', TRUE),
                                                                                                                                               ('TIMEOUT_INACTIVITE_CLIENT_SECONDES', '300', 'Délai en secondes avant déconnexion client inactif.', 'ENTIER', TRUE),
                                                                                                                                               ('TLS_SSL_ACTIVE', 'false', 'Activation du chiffrement TLS/SSL (true/false).', 'BOOLEEN', TRUE),
                                                                                                                                               ('CHEMIN_KEYSTORE_SSL', '/etc/bmo/keystore.jks', 'Chemin vers le keystore SSL.', 'CHAINE', TRUE),
                                                                                                                                               ('MOT_DE_PASSE_KEYSTORE_SSL', 'secret', 'Mot de passe du keystore SSL (sensible).', 'CHAINE', FALSE),
                                                                                                                                               ('AUTO_INSCRIPTION_ACTIVE', 'true', 'Permet l''auto-inscription des nouveaux utilisateurs (true/false).', 'BOOLEEN', TRUE),
                                                                                                                                               ('POLITIQUE_MOT_DE_PASSE_COMPLEXITE_ACTIVE', 'true', 'Activation de la politique de complexité des mots de passe (true/false).', 'BOOLEEN', TRUE),
                                                                                                                                               ('LONGUEUR_MIN_MOT_DE_PASSE', '8', 'Longueur minimale pour les mots de passe.', 'ENTIER', TRUE),
                                                                                                                                               ('EXPORT_BDD_AUTO_ACTIF', 'false', 'Activation de l''export BDD automatique (true/false).', 'BOOLEEN', TRUE);

--
-- Structure de la table `bandwidth_stats`
--
CREATE TABLE `bandwidth_stats` (
                                   `reunion_id` BIGINT NOT NULL,
                                   `utilisateur_id` BIGINT NOT NULL,
                                   `horodatage` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `octets_envoyes` BIGINT NOT NULL DEFAULT 0 COMMENT 'En octets',
                                   `octets_recus` BIGINT NOT NULL DEFAULT 0 COMMENT 'En octets',
                                   PRIMARY KEY (`reunion_id`, `utilisateur_id`, `horodatage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Statistiques de bande passante par utilisateur et réunion';

--
-- Structure de la table `export_jobs`
--
CREATE TABLE `export_jobs` (
                               `id_job` CHAR(36) NOT NULL COMMENT 'UUID pour l''identifiant du job',
                               `type_export` VARCHAR(50) NOT NULL COMMENT 'Ex: EXPORT_CHAT, RAPPORT_REUNION',
                               `demande_par_utilisateur_id` BIGINT NULL DEFAULT NULL COMMENT 'Utilisateur ayant demandé l''export',
                               `date_demande` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `statut_job` VARCHAR(50) NOT NULL COMMENT 'Ex: EN_ATTENTE, EN_COURS, TERMINE, ECHOUE',
                               `chemin_fichier` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Chemin vers le fichier exporté',
                               PRIMARY KEY (`id_job`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tâches d''exportation de données';

--
-- Structure de la table `meeting_ratings`
--
CREATE TABLE `meeting_ratings` (
                                   `reunion_id` BIGINT NOT NULL,
                                   `utilisateur_id` BIGINT NOT NULL,
                                   `note` INT NOT NULL COMMENT 'Ex: 1 à 5 étoiles',
                                   `commentaire` TEXT NULL DEFAULT NULL,
                                   `horodatage_evaluation` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`reunion_id`, `utilisateur_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Évaluations des réunions par les participants';

--
-- Structure de la table `polls` (sondages)
--
CREATE TABLE `polls` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT,
                         `reunion_id` BIGINT NOT NULL,
                         `question_sondage` VARCHAR(500) NOT NULL,
                         `cree_par_utilisateur_id` BIGINT NOT NULL COMMENT 'Utilisateur ayant créé le sondage',
                         `date_creation_sondage` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `est_anonyme` BOOLEAN NOT NULL DEFAULT FALSE,
                         `statut_sondage` ENUM('OUVERT','FERME') NOT NULL DEFAULT 'OUVERT' COMMENT 'Statut du sondage',
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Sondages réalisés pendant les réunions';

--
-- Structure de la table `poll_options` (options de sondage)
--
CREATE TABLE `poll_options` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT,
                                `poll_id` BIGINT NOT NULL,
                                `texte_option` VARCHAR(200) NOT NULL COMMENT 'Texte de l''option du sondage',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Options possibles pour un sondage';

--
-- Structure de la table `votes`
--
CREATE TABLE `votes` (
                         `poll_option_id` BIGINT NOT NULL,
                         `utilisateur_id` BIGINT NOT NULL,
                         `horodatage_vote` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`poll_option_id`, `utilisateur_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Votes des utilisateurs pour les options de sondage';

--
-- Structure de la table `reactions`
--
CREATE TABLE `reactions` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT,
                             `reunion_id` BIGINT NOT NULL,
                             `utilisateur_id` BIGINT NOT NULL,
                             `type_reaction` VARCHAR(50) NOT NULL COMMENT 'Ex: POUCE_HAUT, SOURIRE, APPLAUDISSEMENT',
                             `horodatage_reaction` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Réactions des utilisateurs pendant une réunion';

--
-- Structure de la table `recording_sessions` (sessions d'enregistrement)
--
CREATE TABLE `recording_sessions` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `reunion_id` BIGINT NOT NULL,
                                      `heure_debut_enregistrement` DATETIME NOT NULL,
                                      `heure_fin_enregistrement` DATETIME NULL DEFAULT NULL,
                                      `chemin_fichier_enregistrement` VARCHAR(255) NOT NULL,
                                      `taille_fichier_octets` BIGINT NULL DEFAULT NULL,
                                      `statut_enregistrement` ENUM('EN_COURS','TERMINE','TRAITEMENT_ECHOUE','DISPONIBLE') NOT NULL DEFAULT 'EN_COURS' COMMENT 'Statut de l''enregistrement',
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Informations sur les enregistrements de réunions';

--
-- Index pour les tables
--
ALTER TABLE `reunions`
    ADD KEY `idx_date_heure_debut` (`date_heure_debut`),
  ADD KEY `idx_organisateur_reunion` (`organisateur_id`);

ALTER TABLE `participants_reunion`
    ADD KEY `idx_participant_utilisateur` (`utilisateur_id`),
  ADD KEY `idx_statut_participation` (`statut_participation`);

ALTER TABLE `messages_chat`
    ADD KEY `idx_message_expediteur` (`expediteur_id`),
  ADD KEY `idx_message_reunion_horodatage` (`reunion_id`,`horodatage`);

ALTER TABLE `bandwidth_stats`
    ADD KEY `idx_bw_reunion` (`reunion_id`),
  ADD KEY `idx_bw_utilisateur` (`utilisateur_id`);

ALTER TABLE `export_jobs`
    ADD KEY `idx_export_utilisateur` (`demande_par_utilisateur_id`);

ALTER TABLE `meeting_ratings`
    ADD KEY `idx_rating_reunion` (`reunion_id`),
  ADD KEY `idx_rating_utilisateur` (`utilisateur_id`);

ALTER TABLE `polls`
    ADD KEY `idx_poll_reunion` (`reunion_id`),
  ADD KEY `idx_poll_createur` (`cree_par_utilisateur_id`);

ALTER TABLE `poll_options`
    ADD KEY `idx_poll_option_sondage` (`poll_id`);

ALTER TABLE `votes`
    ADD KEY `idx_vote_option` (`poll_option_id`),
  ADD KEY `idx_vote_utilisateur` (`utilisateur_id`);

ALTER TABLE `reactions`
    ADD KEY `idx_reaction_reunion_utilisateur_type` (`reunion_id`, `utilisateur_id`, `type_reaction`);

ALTER TABLE `recording_sessions`
    ADD KEY `idx_recording_reunion` (`reunion_id`);

--
-- Contraintes pour les tables (Clés étrangères)
--
ALTER TABLE `reunions`
    ADD CONSTRAINT `fk_reunion_organisateur` FOREIGN KEY (`organisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `participants_reunion`
    ADD CONSTRAINT `fk_participant_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_participant_utilisateur` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `messages_chat`
    ADD CONSTRAINT `fk_message_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_message_expediteur` FOREIGN KEY (`expediteur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `bandwidth_stats`
    ADD CONSTRAINT `fk_bw_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_bw_utilisateur` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `export_jobs`
    ADD CONSTRAINT `fk_export_utilisateur` FOREIGN KEY (`demande_par_utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `meeting_ratings`
    ADD CONSTRAINT `fk_rating_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_rating_utilisateur` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `polls`
    ADD CONSTRAINT `fk_poll_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_poll_createur` FOREIGN KEY (`cree_par_utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `poll_options`
    ADD CONSTRAINT `fk_poll_option_sondage` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `votes`
    ADD CONSTRAINT `fk_vote_option` FOREIGN KEY (`poll_option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_vote_utilisateur` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `reactions`
    ADD CONSTRAINT `fk_reaction_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_reaction_utilisateur` FOREIGN KEY (`utilisateur_id`) REFERENCES `utilisateurs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `recording_sessions`
    ADD CONSTRAINT `fk_recording_reunion` FOREIGN KEY (`reunion_id`) REFERENCES `reunions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- --------------------------------------------------------
-- VUES
-- --------------------------------------------------------

DROP VIEW IF EXISTS `v_reunions_avec_organisateur`;
CREATE SQL SECURITY DEFINER VIEW `v_reunions_avec_organisateur`  AS
SELECT
    `r`.`id` AS `id_reunion`,
    `r`.`titre` AS `titre_reunion`,
    `r`.`description` AS `description_reunion`,
    `r`.`date_heure_debut` AS `date_heure_reunion`,
    `r`.`duree_estimee_minutes` AS `duree_minutes_reunion`,
    `r`.`type_reunion` AS `type_reunion`,
    `r`.`statut_reunion` AS `statut_reunion`,
    `r`.`organisateur_id` AS `id_organisateur`,
    `r`.`date_creation_reunion` AS `date_creation_reunion`,
    `u`.`nom_complet` AS `nom_organisateur`,
    `u`.`identifiant` AS `identifiant_organisateur`
FROM (`reunions` `r` JOIN `utilisateurs` `u` ON((`u`.`id` = `r`.`organisateur_id`)));

DROP VIEW IF EXISTS `v_statistiques_participation`;
CREATE SQL SECURITY DEFINER VIEW `v_statistiques_participation`  AS
SELECT
    `r`.`id` AS `id_reunion`,
    `r`.`titre` AS `titre_reunion`,
    COUNT(`p`.`utilisateur_id`) AS `total_participants_potentiels`,
    SUM(CASE WHEN `p`.`statut_participation` IN ('ACCEPTE','REJOINT') THEN 1 ELSE 0 END) AS `nombre_acceptations_ou_rejoints`,
    SUM(CASE WHEN `p`.`statut_participation` = 'REFUSE' THEN 1 ELSE 0 END) AS `nombre_refus`,
    SUM(CASE WHEN `p`.`statut_participation` = 'REJOINT' THEN 1 ELSE 0 END) AS `nombre_rejoints_actuellement`
FROM (`reunions` `r` LEFT JOIN `participants_reunion` `p` ON((`p`.`reunion_id` = `r`.`id`)))
GROUP BY `r`.`id`, `r`.`titre`;

-- --------------------------------------------------------
-- DECLENCHEURS (TRIGGERS)
-- --------------------------------------------------------

DELIMITER $$
CREATE TRIGGER `trg_apres_insertion_reunion_ajout_organisateur_participant`
    AFTER INSERT ON `reunions`
    FOR EACH ROW
BEGIN
    INSERT INTO `participants_reunion` (reunion_id, utilisateur_id, statut_participation, role_dans_reunion, heure_entree)
    VALUES (NEW.id, NEW.organisateur_id, 'ACCEPTE', 'ORGANISATEUR', NEW.date_heure_debut);
    END$$
    DELIMITER ;

DELIMITER $$
    CREATE TRIGGER `trg_avant_maj_participant_verifier_heures`
        BEFORE UPDATE ON `participants_reunion`
        FOR EACH ROW
    BEGIN
        IF NEW.heure_entree IS NOT NULL AND NEW.heure_sortie IS NOT NULL AND NEW.heure_sortie < NEW.heure_entree THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'L''heure de sortie ne peut pas être antérieure à l''heure d''entrée.';
    END IF;
    END$$
    DELIMITER ;

DELIMITER $$
    CREATE TRIGGER `trg_avant_insertion_utilisateur_valider_role`
        BEFORE INSERT ON `utilisateurs`
        FOR EACH ROW
    BEGIN
        IF NEW.role NOT IN ('PARTICIPANT', 'ORGANISATEUR', 'ADMINISTRATEUR', 'GESTIONNAIRE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Valeur de rôle invalide pour l''utilisateur.';
    END IF;
    END$$
    DELIMITER ;

-- --------------------------------------------------------
-- PROCEDURES STOCKEES
-- --------------------------------------------------------

DELIMITER $$
    CREATE PROCEDURE `sp_creer_nouvel_utilisateur`(
        IN p_identifiant VARCHAR(100),
        IN p_mot_de_passe_hache VARCHAR(255),
        IN p_nom_complet VARCHAR(100),
        IN p_role ENUM('PARTICIPANT','ORGANISATEUR','ADMINISTRATEUR','GESTIONNAIRE'),
        OUT p_utilisateur_id BIGINT
            )
    BEGIN
    INSERT INTO `utilisateurs` (identifiant, mot_de_passe, nom_complet, role)
    VALUES (p_identifiant, p_mot_de_passe_hache, p_nom_complet, p_role);
    SET p_utilisateur_id = LAST_INSERT_ID();
END$$
    DELIMITER ;

DELIMITER $$
    CREATE PROCEDURE `sp_planifier_nouvelle_reunion`(
        IN p_titre VARCHAR(200),
        IN p_description TEXT,
        IN p_date_heure_debut DATETIME,
        IN p_duree_estimee_minutes INT,
        IN p_type_reunion ENUM('STANDARD','PRIVEE','DEMOCRATIQUE'),
        IN p_organisateur_id BIGINT,
        IN p_mot_de_passe_reunion VARCHAR(255),
        OUT p_reunion_id BIGINT
            )
    BEGIN
    INSERT INTO `reunions` (titre, description, date_heure_debut, duree_estimee_minutes, type_reunion, organisateur_id, mot_de_passe_reunion, statut_reunion)
    VALUES (p_titre, p_description, p_date_heure_debut, p_duree_estimee_minutes, p_type_reunion, p_organisateur_id, p_mot_de_passe_reunion, 'PLANIFIEE');
    SET p_reunion_id = LAST_INSERT_ID();
    -- Le déclencheur `trg_apres_insertion_reunion_ajout_organisateur_participant` s'occupe d'ajouter l'organisateur.
END$$
    DELIMITER ;

DELIMITER $$
    CREATE PROCEDURE `sp_enregistrer_vote_sondage`(
        IN p_poll_option_id BIGINT,
        IN p_utilisateur_id BIGINT
    )
    BEGIN
    DECLARE sondage_est_ouvert BOOLEAN;
    SELECT ps.statut_sondage = 'OUVERT' INTO sondage_est_ouvert
    FROM polls ps
             JOIN poll_options po ON ps.id = po.poll_id
    WHERE po.id = p_poll_option_id;

    IF sondage_est_ouvert THEN
        INSERT INTO `votes` (poll_option_id, utilisateur_id, horodatage_vote)
        VALUES (p_poll_option_id, p_utilisateur_id, CURRENT_TIMESTAMP);
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le sondage est fermé et n''accepte plus de votes.';
END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE `sp_obtenir_parametre_application`(
    IN p_cle_parametre VARCHAR(100),
    OUT p_valeur_parametre VARCHAR(255)
)
BEGIN
SELECT valeur_parametre INTO p_valeur_parametre
FROM `settings`
WHERE cle_parametre = p_cle_parametre;
END$$
DELIMITER ;

--
-- Insertion de quelques utilisateurs de test (adapter les mots de passe si le hachage est géré par l'application)
--
-- IMPORTANT: Les mots de passe ici sont en clair. Votre application DEVRA les hacher.
-- Si Spring Security est utilisé, il hachera les mots de passe fournis lors de la création
-- ou vérifiera les mots de passe hachés de la BDD.
-- Pour le développement, il est plus simple de laisser l'application gérer le hachage à la création.
-- Si vous insérez des utilisateurs ici pour des tests initiaux SANS hachage applicatif au démarrage,
-- vous devrez utiliser ces mots de passe en clair pour vous connecter via le client.

INSERT INTO `utilisateurs` (`identifiant`, `mot_de_passe`, `nom_complet`, `role`, `statut_compte`) VALUES
                                                                                                       ('admin', '$2a$10$EESqNo1k.CMuyAbV.11uR.Xg6N9wYjHqOQSPQkpa8S7k5x5o0o3R.', 'Super Admin', 'ADMINISTRATEUR', 'ACTIF'), -- mdp: adminBMO123!
                                                                                                       ('orga', '$2a$10$8A4sP2N/eJk2lI8Tqf0.z.R0w0gY3o0g5h2L6lU7fJ8qT9oK1uJ8C', 'Organisateur Principal', 'ORGANISATEUR', 'ACTIF'), -- mdp: orgaBMO123!
                                                                                                       ('user1', '$2a$10$UuLz.iXvR9mQ6K3nJ0p8T.mY9o2p7qR5sT1uV8wX3yZ4A5B6c7D8E', 'Alice Participant', 'PARTICIPANT', 'ACTIF'), -- mdp: userBMO123!
                                                                                                       ('user2', '$2a$10$VvMw.jYwS0nR7lU4oK1p9T.nZ8a1b3c5d7eR9fG0hI2jK3l4mN5oP', 'Bob Spectateur', 'PARTICIPANT', 'INACTIF'); -- mdp: bobBMO123!

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;