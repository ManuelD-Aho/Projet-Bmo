-- Schéma initial de la base de données BMO

CREATE TABLE IF NOT EXISTS utilisateur (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reunion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    organisateur_id BIGINT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organisateur_id) REFERENCES utilisateur(id)
);

CREATE TABLE IF NOT EXISTS message_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT NOT NULL,
    auteur_id BIGINT NOT NULL,
    reunion_id BIGINT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auteur_id) REFERENCES utilisateur(id),
    FOREIGN KEY (reunion_id) REFERENCES reunion(id)
);

-- Données de test
INSERT INTO utilisateur (nom, email, mot_de_passe) VALUES
    ('Admin BMO', 'admin@bmo.com', 'admin123'),
    ('Jean Dupont', 'jean.dupont@email.com', 'password123'),
    ('Marie Martin', 'marie.martin@email.com', 'password123');
