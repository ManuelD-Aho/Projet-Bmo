package akandan.bahou.kassy.serveur.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import akandan.bahou.kassy.serveur.configuration.ConfigurateurServeur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GestionnaireConnexionBaseDeDonnees {

    private final String urlJDBCBaseDeDonnees;
    private final String utilisateurBaseDeDonnees;
    private final String motDePasseBaseDeDonnees;
    private static final Logger journal = LoggerFactory.getLogger(GestionnaireConnexionBaseDeDonnees.class);

    public GestionnaireConnexionBaseDeDonnees(ConfigurateurServeur configurateur) {
        this.urlJDBCBaseDeDonnees = configurateur.recupererProprieteChaine("bmo.db.url");
        this.utilisateurBaseDeDonnees = configurateur.recupererProprieteChaine("bmo.db.utilisateur");
        this.motDePasseBaseDeDonnees = configurateur.recupererProprieteChaine("bmo.db.motdepasse");

        if (this.urlJDBCBaseDeDonnees == null || this.urlJDBCBaseDeDonnees.trim().isEmpty() ||
                this.utilisateurBaseDeDonnees == null || this.utilisateurBaseDeDonnees.trim().isEmpty()) {
            String messageErreur = "Les paramètres de connexion à la base de données (URL, utilisateur) sont manquants ou invalides.";
            journal.error(messageErreur);
            throw new IllegalStateException(messageErreur);
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            journal.error("Driver JDBC MySQL introuvable. Vérifiez les dépendances Maven.", e);
            throw new RuntimeException("Driver JDBC MySQL introuvable.", e);
        }
        journal.info("Gestionnaire de connexion à la base de données initialisé pour l'URL : {}", this.urlJDBCBaseDeDonnees);
    }

    public Connection etablirNouvelleConnexion() throws ExceptionPersistance {
        journal.debug("Tentative d'établissement d'une nouvelle connexion JDBC à : {}", this.urlJDBCBaseDeDonnees);
        try {
            Connection connexion = DriverManager.getConnection(this.urlJDBCBaseDeDonnees, this.utilisateurBaseDeDonnees, this.motDePasseBaseDeDonnees);
            journal.debug("Nouvelle connexion JDBC établie avec succès.");
            return connexion;
        } catch (SQLException e) {
            journal.error("Échec de l'établissement de la connexion JDBC à '{}' pour l'utilisateur '{}'. Erreur : {}", this.urlJDBCBaseDeDonnees, this.utilisateurBaseDeDonnees, e.getMessage(), e);
            throw new ExceptionPersistance("Impossible d'établir une connexion à la base de données. Vérifiez la configuration et l'état du serveur de base de données.", e);
        }
    }
}