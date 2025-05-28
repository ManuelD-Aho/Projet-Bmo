-- Base de données : bmo_db (ou bmo_base_de_donnees selon votre préférence)
-- Version Ultime Fusionnée

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Structure de la table `users`
--
CREATE TABLE `users` (
                         `id` INT NOT NULL AUTO_INCREMENT,
                         `login` VARCHAR(50) NOT NULL,
                         `password` VARCHAR(255) NOT NULL,
                         `name` VARCHAR(100) NOT NULL,
                         `role` ENUM('USER','ADMIN') DEFAULT 'USER',
                         `photo` MEDIUMBLOB NULL DEFAULT NULL,
                         `photo_mimetype` VARCHAR(50) NULL DEFAULT NULL,
                         `date_created` DATETIME DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `login` (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Table des utilisateurs BMO';

--
-- Structure de la table `meetings`
--
CREATE TABLE `meetings` (
                            `id` INT NOT NULL AUTO_INCREMENT,
                            `title` VARCHAR(200) NOT NULL,
                            `agenda` TEXT NULL DEFAULT NULL,
                            `datetime` DATETIME NOT NULL,
                            `duration` INT NOT NULL COMMENT 'Durée en minutes',
                            `type` ENUM('Standard','Privée','Démocratique') DEFAULT 'Standard',
                            `status` ENUM('Planifiée','Ouverte','Terminée','Annulée') DEFAULT 'Planifiée' COMMENT 'Statut de la réunion',
                            `organizer_id` INT NOT NULL,
                            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `participants`
--
CREATE TABLE `participants` (
                                `meeting_id` INT NOT NULL,
                                `user_id` INT NOT NULL,
                                `status` ENUM('invited','accepted','declined','joined','left','excluded') DEFAULT 'invited' COMMENT 'Statut de la participation',
                                `role` ENUM('organizer','participant','presenter') DEFAULT 'participant' COMMENT 'Rôle dans la réunion',
                                `join_time` DATETIME NULL DEFAULT NULL,
                                `leave_time` DATETIME NULL DEFAULT NULL,
                                PRIMARY KEY (`meeting_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Participation aux réunions';

--
-- Structure de la table `messages`
--
CREATE TABLE `messages` (
                            `id` INT NOT NULL AUTO_INCREMENT,
                            `meeting_id` INT NOT NULL,
                            `user_id` INT NOT NULL,
                            `timestamp` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            `content` TEXT NOT NULL,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Historique du chat';

--
-- Structure de la table `settings` (fusion de `settings` et `parametres_application`)
--
CREATE TABLE `settings` (
                            `key_setting` VARCHAR(100) NOT NULL COMMENT 'Clé unique du paramètre',
                            `value_setting` VARCHAR(255) DEFAULT NULL COMMENT 'Valeur du paramètre',
                            `description` VARCHAR(500) DEFAULT NULL COMMENT 'Description du paramètre',
                            `type_donnee_parametre` ENUM('CHAINE','ENTIER','BOOLEEN','LISTE') NOT NULL DEFAULT 'CHAINE' COMMENT 'Type de donnée de la valeur',
                            `modifiable_via_interface` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Modifiable via UI admin',
                            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (`key_setting`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Paramètres applicatifs';

--
-- Insertion de quelques paramètres par défaut
--
INSERT INTO `settings` (`key_setting`, `value_setting`, `description`, `type_donnee_parametre`, `modifiable_via_interface`) VALUES
                                                                                                                                ('PORT_SERVEUR_TCP', '5000', 'Port d\'écoute principal du serveur BMO.', 'ENTIER', TRUE),
('MAX_CONNEXIONS_SIMULTANEES', '100', 'Nombre maximum de clients connectés simultanément.', 'ENTIER', TRUE),
('TIMEOUT_INACTIVITE_CLIENT_SECONDES', '300', 'Délai en secondes avant déconnexion client inactif.', 'ENTIER', TRUE),
('TLS_SSL_ACTIVE', 'FALSE', 'Activation du chiffrement TLS/SSL.', 'BOOLEEN', TRUE),
('CHEMIN_KEYSTORE_SSL', '/etc/bmo/keystore.jks', 'Chemin vers le keystore SSL.', 'CHAINE', TRUE),
('MOT_DE_PASSE_KEYSTORE_SSL', 'secret', 'Mot de passe du keystore SSL.', 'CHAINE', FALSE),
('AUTO_INSCRIPTION_ACTIVE', 'TRUE', 'Permet l\'auto-inscription des nouveaux utilisateurs.', 'BOOLEEN', TRUE),
                                                                                                                                ('POLITIQUE_MOT_DE_PASSE_COMPLEXITE_ACTIVE', 'TRUE', 'Activation de la politique de complexité des mots de passe.', 'BOOLEEN', TRUE),
                                                                                                                                ('LONGUEUR_MIN_MOT_DE_PASSE', '8', 'Longueur minimale pour les mots de passe.', 'ENTIER', TRUE),
                                                                                                                                ('EXPORT_BDD_AUTO_ACTIF', 'FALSE', 'Activation de l\'export BDD automatique.', 'BOOLEEN', TRUE);

--
-- Structure de la table `bandwidth_stats`
--
CREATE TABLE `bandwidth_stats` (
  `meeting_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `upload` BIGINT NOT NULL COMMENT 'En octets',
  `download` BIGINT NOT NULL COMMENT 'En octets',
  PRIMARY KEY (`meeting_id`, `user_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `export_jobs`
--
CREATE TABLE `export_jobs` (
  `job_id` CHAR(36) NOT NULL COMMENT 'UUID pour l''identifiant du job',
  `type` VARCHAR(50) NOT NULL COMMENT 'Ex: CHAT_EXPORT, MEETING_REPORT',
  `requested_by_user_id` INT NULL DEFAULT NULL COMMENT 'Utilisateur ayant demandé l''export',
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` VARCHAR(50) NOT NULL COMMENT 'Ex: PENDING, PROCESSING, COMPLETED, FAILED',
  `file_path` VARCHAR(255) NULL DEFAULT NULL COMMENT 'Chemin vers le fichier exporté',
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `meeting_ratings`
--
CREATE TABLE `meeting_ratings` (
  `meeting_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `rating` INT NOT NULL COMMENT 'Ex: 1 à 5 étoiles',
  `comment` TEXT NULL DEFAULT NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`meeting_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `polls`
--
CREATE TABLE `polls` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meeting_id` INT NOT NULL,
  `question` VARCHAR(500) NOT NULL,
  `created_by_user_id` INT NOT NULL COMMENT 'Utilisateur ayant créé le sondage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_anonymous` BOOLEAN NOT NULL DEFAULT FALSE,
  `status` ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN' COMMENT 'Statut du sondage',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `poll_options`
--
CREATE TABLE `poll_options` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `poll_id` INT NOT NULL,
  `text_option` VARCHAR(200) NOT NULL COMMENT 'Texte de l''option du sondage',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `votes`
--
CREATE TABLE `votes` (
  `poll_option_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`poll_option_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `reactions`
--
CREATE TABLE `reactions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `meeting_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `type_reaction` VARCHAR(50) NOT NULL COMMENT 'Ex: THUMBS_UP, SMILE, CLAP',
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Structure de la table `recording_sessions`
--
CREATE TABLE `recording_sessions` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `meeting_id` INT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NULL DEFAULT NULL,
  `file_path` VARCHAR(255) NOT NULL,
  `file_size_bytes` BIGINT NULL DEFAULT NULL,
  `status_recording` ENUM('RECORDING','COMPLETED','FAILED_PROCESSING','AVAILABLE') NOT NULL DEFAULT 'RECORDING' COMMENT 'Statut de l''enregistrement',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Index pour les tables
--
ALTER TABLE `meetings`
  ADD KEY `idx_datetime` (`datetime`),
  ADD KEY `idx_organizer` (`organizer_id`);

ALTER TABLE `participants`
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_status` (`status`);

ALTER TABLE `messages`
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_meeting_time` (`meeting_id`,`timestamp`);

ALTER TABLE `bandwidth_stats`
  ADD KEY `idx_bandwidth_stats_meeting` (`meeting_id`),
  ADD KEY `idx_bandwidth_stats_user` (`user_id`);

ALTER TABLE `export_jobs`
  ADD KEY `idx_export_jobs_user` (`requested_by_user_id`);

ALTER TABLE `meeting_ratings`
  ADD KEY `idx_meeting_ratings_meeting` (`meeting_id`),
  ADD KEY `idx_meeting_ratings_user` (`user_id`);

ALTER TABLE `polls`
  ADD KEY `idx_polls_meeting` (`meeting_id`),
  ADD KEY `idx_polls_creator` (`created_by_user_id`);

ALTER TABLE `poll_options`
  ADD KEY `idx_poll_options_poll` (`poll_id`);

ALTER TABLE `votes`
  ADD KEY `idx_votes_option` (`poll_option_id`),
  ADD KEY `idx_votes_user` (`user_id`);

ALTER TABLE `reactions`
  ADD KEY `idx_reactions_meeting_user_type` (`meeting_id`, `user_id`, `type_reaction`);

ALTER TABLE `recording_sessions`
  ADD KEY `idx_recording_sessions_meeting` (`meeting_id`);

--
-- Contraintes pour les tables
--
ALTER TABLE `meetings`
  ADD CONSTRAINT `fk_meetings_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `participants`
  ADD CONSTRAINT `fk_participants_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `messages`
  ADD CONSTRAINT `fk_messages_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `bandwidth_stats`
  ADD CONSTRAINT `fk_bandwidth_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_bandwidth_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `export_jobs`
  ADD CONSTRAINT `fk_export_jobs_user` FOREIGN KEY (`requested_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE `meeting_ratings`
  ADD CONSTRAINT `fk_ratings_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_ratings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `polls`
  ADD CONSTRAINT `fk_polls_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_polls_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `poll_options`
  ADD CONSTRAINT `fk_poll_options_poll` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `votes`
  ADD CONSTRAINT `fk_votes_option` FOREIGN KEY (`poll_option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_votes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `reactions`
  ADD CONSTRAINT `fk_reactions_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_reactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `recording_sessions`
  ADD CONSTRAINT `fk_recording_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- --------------------------------------------------------
-- VUES
-- --------------------------------------------------------

DROP VIEW IF EXISTS `v_meetings_with_organizer`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `v_meetings_with_organizer`  AS
SELECT
    `m`.`id` AS `id_reunion`,
    `m`.`title` AS `titre_reunion`,
    `m`.`agenda` AS `agenda_reunion`,
    `m`.`datetime` AS `date_heure_reunion`,
    `m`.`duration` AS `duree_minutes_reunion`,
    `m`.`type` AS `type_reunion`,
    `m`.`status` AS `statut_reunion`,
    `m`.`organizer_id` AS `id_organisateur`,
    `m`.`created_at` AS `date_creation_reunion`,
    `u`.`name` AS `nom_organisateur`,
    `u`.`login` AS `login_organisateur`
FROM (`meetings` `m` JOIN `users` `u` ON((`u`.`id` = `m`.`organizer_id`)));

DROP VIEW IF EXISTS `v_participation_stats`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `v_participation_stats`  AS
SELECT
    `m`.`id` AS `id_reunion`,
    `m`.`title` AS `titre_reunion`,
    COUNT(`p`.`user_id`) AS `total_participants_potentiels`,
    SUM(CASE WHEN `p`.`status` IN ('accepted','joined') THEN 1 ELSE 0 END) AS `nombre_acceptations_ou_rejoints`,
    SUM(CASE WHEN `p`.`status` = 'declined' THEN 1 ELSE 0 END) AS `nombre_refus`,
    SUM(CASE WHEN `p`.`status` = 'joined' THEN 1 ELSE 0 END) AS `nombre_rejoints_actuellement`
FROM (`meetings` `m` LEFT JOIN `participants` `p` ON((`p`.`meeting_id` = `m`.`id`)))
GROUP BY `m`.`id`, `m`.`title`;

-- --------------------------------------------------------
-- DECLENCHEURS
-- --------------------------------------------------------

DELIMITER $$
CREATE TRIGGER `trg_after_meeting_insert_add_organizer_as_participant`
AFTER INSERT ON `meetings`
FOR EACH ROW
BEGIN
    INSERT INTO `participants` (meeting_id, user_id, status, role, join_time)
    VALUES (NEW.id, NEW.organizer_id, 'accepted', 'organizer', NEW.datetime);
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER `trg_before_participant_update_check_times`
BEFORE UPDATE ON `participants`
FOR EACH ROW
BEGIN
    IF NEW.join_time IS NOT NULL AND NEW.leave_time IS NOT NULL AND NEW.leave_time < NEW.join_time THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'L\'heure de sortie ne peut pas être antérieure à l\'heure d\'entrée.';
END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER `trg_before_user_insert_validate_role`
    BEFORE INSERT ON `users`
    FOR EACH ROW
BEGIN
    IF NEW.role NOT IN ('USER', 'ADMIN') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Valeur de rôle invalide pour l''utilisateur.';
END IF;
END$$
DELIMITER ;

-- --------------------------------------------------------
-- PROCEDURES STOCKEES
-- --------------------------------------------------------

DELIMITER $$
CREATE PROCEDURE `sp_CreerNouvelUtilisateur`(
    IN p_login VARCHAR(50),
    IN p_password_hache VARCHAR(255),
    IN p_name VARCHAR(100),
    IN p_role ENUM('USER','ADMIN'),
    OUT p_user_id INT
        )
BEGIN
INSERT INTO `users` (login, password, name, role)
VALUES (p_login, p_password_hache, p_name, p_role);
SET p_user_id = LAST_INSERT_ID();
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE `sp_PlanifierNouvelleReunion`(
    IN p_title VARCHAR(200),
    IN p_agenda TEXT,
    IN p_datetime DATETIME,
    IN p_duration INT,
    IN p_type ENUM('Standard','Privée','Démocratique'),
    IN p_organizer_id INT,
    OUT p_meeting_id INT
        )
BEGIN
INSERT INTO `meetings` (title, agenda, datetime, duration, type, organizer_id, status)
VALUES (p_title, p_agenda, p_datetime, p_duration, p_type, p_organizer_id, 'Planifiée');
SET p_meeting_id = LAST_INSERT_ID();
    -- Le déclencheur `trg_after_meeting_insert_add_organizer_as_participant` s'occupe d'ajouter l'organisateur.
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE `sp_EnregistrerVoteSondage`(
    IN p_poll_option_id INT,
    IN p_user_id INT
)
BEGIN
    -- Logique pour vérifier si l'utilisateur a déjà voté pour ce sondage (si une seule réponse par utilisateur)
    -- ou si plusieurs votes sont autorisés par option.
    -- Pour cet exemple simple, on insère directement.
    -- Une logique plus complexe pourrait vérifier l'anonymat du sondage, etc.
    DECLARE poll_is_open BOOLEAN;
SELECT p.status = 'OPEN' INTO poll_is_open
FROM polls p
         JOIN poll_options po ON p.id = po.poll_id
WHERE po.id = p_poll_option_id;

IF poll_is_open THEN
        INSERT INTO `votes` (poll_option_id, user_id, timestamp)
        VALUES (p_poll_option_id, p_user_id, CURRENT_TIMESTAMP);
ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Le sondage est fermé et n''accepte plus de votes.';
END IF;
END$$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE `sp_ObtenirSetting`(
    IN p_key_setting VARCHAR(100),
    OUT p_value_setting VARCHAR(255)
)
BEGIN
SELECT value_setting INTO p_value_setting
FROM `settings`
WHERE key_setting = p_key_setting;
END$$
DELIMITER ;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;