package akandan.bahou.kassy.serveur.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur; // Not in schema, cannot be used for DB
import akandan.bahou.kassy.commun.util.ExceptionPersistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilisateurDAOImpl implements InterfaceUtilisateurDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(UtilisateurDAOImpl.class);

    private static final String REQUETE_CREER_UTILISATEUR = "INSERT INTO users (login, password, name, role, photo, photo_mimetype) VALUES (?, ?, ?, ?, ?, ?)"; // date_created is DEFAULT
    private static final String REQUETE_TROUVER_PAR_ID = "SELECT id, login, password, name, role, photo, photo_mimetype, date_created FROM users WHERE id = ?";
    private static final String REQUETE_TROUVER_PAR_IDENTIFIANT_CONNEXION = "SELECT id, login, password, name, role, photo, photo_mimetype, date_created FROM users WHERE login = ?";
    private static final String REQUETE_TROUVER_TOUS = "SELECT id, login, password, name, role, photo, photo_mimetype, date_created FROM users ORDER BY name ASC";
    private static final String REQUETE_METTRE_A_JOUR_UTILISATEUR = "UPDATE users SET login=?, password=?, name=?, role=?, photo=?, photo_mimetype=? WHERE id = ?";
    private static final String REQUETE_SUPPRIMER_UTILISATEUR = "DELETE FROM users WHERE id = ?";
    private static final String REQUETE_EXISTE_PAR_IDENTIFIANT_CONNEXION = "SELECT COUNT(*) AS nombre FROM users WHERE login = ?";

    public UtilisateurDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private Utilisateur hydraterUtilisateurDepuisResultSet(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(rs.getInt("id"));
        utilisateur.setIdentifiantConnexion(rs.getString("login"));
        utilisateur.setMotDePasseHache(rs.getString("password"));
        // selMotDePasse n'est pas dans le schéma 'users'
        utilisateur.setNomComplet(rs.getString("name"));

        String roleDb = rs.getString("role");
        if (roleDb != null) {
            if ("ADMIN".equalsIgnoreCase(roleDb)) {
                utilisateur.setRoleSysteme(RoleUtilisateur.ADMINISTRATEUR);
            } else if ("USER".equalsIgnoreCase(roleDb)) {
                // Par défaut, un 'USER' de la DB est un 'PARTICIPANT'.
                // La distinction 'ORGANISATEUR' est une logique applicative au-dessus.
                utilisateur.setRoleSysteme(RoleUtilisateur.PARTICIPANT);
            } else {
                journal.warn("Rôle système inconnu depuis DB: '{}'", roleDb);
            }
        }
        // statutCompte n'est pas dans le schéma 'users'
        Timestamp dateCreationSql = rs.getTimestamp("date_created");
        if (dateCreationSql != null) {
            utilisateur.setDateCreationCompte(dateCreationSql.toLocalDateTime());
        }
        // derniereConnexion n'est pas dans le schéma 'users'
        utilisateur.setPhoto(rs.getBytes("photo"));
        utilisateur.setPhotoMimeType(rs.getString("photo_mimetype"));
        return utilisateur;
    }

    @Override
    public Utilisateur creer(Utilisateur utilisateurACreer) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_CREER_UTILISATEUR, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setString(1, utilisateurACreer.getIdentifiantConnexion());
            declarationPreparee.setString(2, utilisateurACreer.getMotDePasseHache());
            declarationPreparee.setString(3, utilisateurACreer.getNomComplet());
            if (utilisateurACreer.getRoleSysteme() == RoleUtilisateur.ADMINISTRATEUR) {
                declarationPreparee.setString(4, "ADMIN");
            } else { // PARTICIPANT et ORGANISATEUR mappent à 'USER' dans la DB
                declarationPreparee.setString(4, "USER");
            }
            declarationPreparee.setBytes(5, utilisateurACreer.getPhoto());
            declarationPreparee.setString(6, utilisateurACreer.getPhotoMimeType());
            // date_created est géré par DB DEFAULT CURRENT_TIMESTAMP

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        utilisateurACreer.setId(clesGenerees.getInt(1));
                        // Récupérer date_created si nécessaire, bien que la DB le gère
                        // Optionnel: Re-fetch l'utilisateur pour avoir la date_created par la DB
                    }
                }
            }
            journal.debug("Utilisateur créé avec ID : {}", utilisateurACreer.getId());
            return utilisateurACreer;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la création de l'utilisateur : {}", utilisateurACreer.getIdentifiantConnexion(), e);
            throw new ExceptionPersistance("Erreur lors de la création de l'utilisateur.", e);
        }
    }

    @Override
    public Optional<Utilisateur> trouverParId(int idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ID)) {

            declarationPreparee.setInt(1, idUtilisateur);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    Utilisateur utilisateur = hydraterUtilisateurDepuisResultSet(resultSet);
                    journal.debug("Utilisateur trouvé par ID : {}", idUtilisateur);
                    return Optional.of(utilisateur);
                }
            }
            journal.debug("Aucun utilisateur trouvé pour ID : {}", idUtilisateur);
            return Optional.empty();

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la recherche de l'utilisateur par ID : {}", idUtilisateur, e);
            throw new ExceptionPersistance("Erreur lors de la recherche de l'utilisateur par ID.", e);
        }
    }

    @Override
    public Optional<Utilisateur> trouverParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_IDENTIFIANT_CONNEXION)) {

            declarationPreparee.setString(1, identifiantConnexion);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    Utilisateur utilisateur = hydraterUtilisateurDepuisResultSet(resultSet);
                    journal.debug("Utilisateur trouvé par identifiant de connexion : {}", identifiantConnexion);
                    return Optional.of(utilisateur);
                }
            }
            journal.debug("Aucun utilisateur trouvé pour l'identifiant de connexion : {}", identifiantConnexion);
            return Optional.empty();

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la recherche de l'utilisateur par identifiant de connexion : {}", identifiantConnexion, e);
            throw new ExceptionPersistance("Erreur lors de la recherche de l'utilisateur par identifiant de connexion.", e);
        }
    }

    @Override
    public List<Utilisateur> trouverTous() throws ExceptionPersistance {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_TOUS);
             ResultSet resultSet = declarationPreparee.executeQuery()) {

            while (resultSet.next()) {
                utilisateurs.add(hydraterUtilisateurDepuisResultSet(resultSet));
            }
            journal.debug("{} utilisateurs trouvés au total.", utilisateurs.size());

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération de tous les utilisateurs.", e);
            throw new ExceptionPersistance("Erreur lors de la récupération de tous les utilisateurs.", e);
        }
        return utilisateurs;
    }

    @Override
    public boolean mettreAJour(Utilisateur utilisateurAMettreAJour) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_UTILISATEUR)) {

            declarationPreparee.setString(1, utilisateurAMettreAJour.getIdentifiantConnexion());
            declarationPreparee.setString(2, utilisateurAMettreAJour.getMotDePasseHache());
            declarationPreparee.setString(3, utilisateurAMettreAJour.getNomComplet());
            if (utilisateurAMettreAJour.getRoleSysteme() == RoleUtilisateur.ADMINISTRATEUR) {
                declarationPreparee.setString(4, "ADMIN");
            } else {
                declarationPreparee.setString(4, "USER");
            }
            declarationPreparee.setBytes(5, utilisateurAMettreAJour.getPhoto());
            declarationPreparee.setString(6, utilisateurAMettreAJour.getPhotoMimeType());
            declarationPreparee.setInt(7, utilisateurAMettreAJour.getId());
            // derniere_connexion et statut_compte ne sont pas dans le schéma et ne peuvent pas être mis à jour ici

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour de l'utilisateur ID : {}, lignes affectées : {}", utilisateurAMettreAJour.getId(), lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour de l'utilisateur ID : {}", utilisateurAMettreAJour.getId(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour de l'utilisateur.", e);
        }
    }

    @Override
    public boolean supprimer(int idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_UTILISATEUR)) {

            declarationPreparee.setInt(1, idUtilisateur);
            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Suppression de l'utilisateur ID : {}, lignes affectées : {}", idUtilisateur, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la suppression de l'utilisateur ID : {}", idUtilisateur, e);
            throw new ExceptionPersistance("Erreur lors de la suppression de l'utilisateur.", e);
        }
    }

    @Override
    public boolean existeParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_EXISTE_PAR_IDENTIFIANT_CONNEXION)) {

            declarationPreparee.setString(1, identifiantConnexion);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    boolean existe = resultSet.getInt("nombre") > 0;
                    journal.debug("Vérification existence pour identifiant de connexion '{}': {}", identifiantConnexion, existe);
                    return existe;
                }
            }
            return false;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la vérification d'existence par identifiant de connexion : {}", identifiantConnexion, e);
            throw new ExceptionPersistance("Erreur lors de la vérification d'existence par identifiant de connexion.", e);
        }
    }
}