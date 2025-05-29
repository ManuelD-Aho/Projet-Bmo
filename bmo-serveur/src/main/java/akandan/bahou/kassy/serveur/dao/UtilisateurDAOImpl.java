package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilisateurDAOImpl implements InterfaceUtilisateurDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(UtilisateurDAOImpl.class);

    private static final String TABLE_UTILISATEURS = "utilisateurs";
    private static final String COL_ID = "id";
    private static final String COL_IDENTIFIANT = "identifiant_connexion";
    private static final String COL_MOT_DE_PASSE_HACHE = "mot_de_passe_hache";
    private static final String COL_SEL_MOT_DE_PASSE = "sel_mot_de_passe";
    private static final String COL_NOM_COMPLET = "nom_complet";
    private static final String COL_ROLE_SYSTEME = "role_systeme";
    private static final String COL_STATUT_COMPTE = "statut_compte";
    private static final String COL_DATE_CREATION = "date_creation_compte";
    private static final String COL_DERNIERE_CONNEXION = "derniere_connexion";

    private static final String REQUETE_CREER_UTILISATEUR = "INSERT INTO " + TABLE_UTILISATEURS +
            " (" + COL_IDENTIFIANT + ", " + COL_MOT_DE_PASSE_HACHE + ", " + COL_SEL_MOT_DE_PASSE + ", " +
            COL_NOM_COMPLET + ", " + COL_ROLE_SYSTEME + ", " + COL_STATUT_COMPTE + ", " + COL_DATE_CREATION + ", " + COL_DERNIERE_CONNEXION +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REQUETE_TROUVER_PAR_ID = "SELECT * FROM " + TABLE_UTILISATEURS + " WHERE " + COL_ID + " = ?";
    private static final String REQUETE_TROUVER_PAR_IDENTIFIANT_CONNEXION = "SELECT * FROM " + TABLE_UTILISATEURS + " WHERE " + COL_IDENTIFIANT + " = ?";
    private static final String REQUETE_TROUVER_TOUS = "SELECT * FROM " + TABLE_UTILISATEURS + " ORDER BY " + COL_NOM_COMPLET + " ASC";
    private static final String REQUETE_METTRE_A_JOUR_UTILISATEUR = "UPDATE " + TABLE_UTILISATEURS + " SET " +
            COL_IDENTIFIANT + "=?, " + COL_MOT_DE_PASSE_HACHE + "=?, " + COL_SEL_MOT_DE_PASSE + "=?, " +
            COL_NOM_COMPLET + "=?, " + COL_ROLE_SYSTEME + "=?, " + COL_STATUT_COMPTE + "=?, " + COL_DERNIERE_CONNEXION + "=? " +
            "WHERE " + COL_ID + " = ?";
    private static final String REQUETE_SUPPRIMER_UTILISATEUR = "DELETE FROM " + TABLE_UTILISATEURS + " WHERE " + COL_ID + " = ?";
    private static final String REQUETE_EXISTE_PAR_IDENTIFIANT_CONNEXION = "SELECT COUNT(*) AS nombre FROM " + TABLE_UTILISATEURS + " WHERE " + COL_IDENTIFIANT + " = ?";

    public UtilisateurDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private Utilisateur hydraterUtilisateurDepuisResultSet(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(rs.getLong(COL_ID));
        utilisateur.setIdentifiant(rs.getString(COL_IDENTIFIANT));
        utilisateur.setMotDePasseHache(rs.getString(COL_MOT_DE_PASSE_HACHE));
        utilisateur.setSelMotDePasse(rs.getString(COL_SEL_MOT_DE_PASSE));
        utilisateur.setNomComplet(rs.getString(COL_NOM_COMPLET));
        utilisateur.setRole(RoleUtilisateur.valueOf(rs.getString(COL_ROLE_SYSTEME)));
        utilisateur.setStatutCompte(StatutCompteUtilisateur.valueOf(rs.getString(COL_STATUT_COMPTE)));
        Timestamp dateCreationSql = rs.getTimestamp(COL_DATE_CREATION);
        if (dateCreationSql != null) {
            utilisateur.setDateCreationCompte(dateCreationSql.toLocalDateTime());
        }
        Timestamp dateDerniereConnexionSql = rs.getTimestamp(COL_DERNIERE_CONNEXION);
        if (dateDerniereConnexionSql != null) {
            utilisateur.setDerniereConnexion(dateDerniereConnexionSql.toLocalDateTime());
        }
        return utilisateur;
    }

    @Override
    public Utilisateur creer(Utilisateur utilisateurACreer) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_CREER_UTILISATEUR, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setString(1, utilisateurACreer.getIdentifiant());
            declarationPreparee.setString(2, utilisateurACreer.getMotDePasseHache());
            declarationPreparee.setString(3, utilisateurACreer.getSelMotDePasse());
            declarationPreparee.setString(4, utilisateurACreer.getNomComplet());
            declarationPreparee.setString(5, utilisateurACreer.getRole().name());
            declarationPreparee.setString(6, utilisateurACreer.getStatutCompte().name());
            declarationPreparee.setTimestamp(7, Timestamp.valueOf(utilisateurACreer.getDateCreationCompte()));
            if (utilisateurACreer.getDerniereConnexion() != null) {
                declarationPreparee.setTimestamp(8, Timestamp.valueOf(utilisateurACreer.getDerniereConnexion()));
            } else {
                declarationPreparee.setNull(8, Types.TIMESTAMP);
            }

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        utilisateurACreer.setId(clesGenerees.getLong(1));
                    }
                }
            }
            journal.debug("Utilisateur créé avec ID : {}", utilisateurACreer.getId());
            return utilisateurACreer;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la création de l'utilisateur : {}", utilisateurACreer.getIdentifiant(), e);
            throw new ExceptionPersistance("Erreur lors de la création de l'utilisateur.", e);
        }
    }

    @Override
    public Optional<Utilisateur> trouverParId(long idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ID)) {

            declarationPreparee.setLong(1, idUtilisateur);
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

            declarationPreparee.setString(1, utilisateurAMettreAJour.getIdentifiant());
            declarationPreparee.setString(2, utilisateurAMettreAJour.getMotDePasseHache());
            declarationPreparee.setString(3, utilisateurAMettreAJour.getSelMotDePasse());
            declarationPreparee.setString(4, utilisateurAMettreAJour.getNomComplet());
            declarationPreparee.setString(5, utilisateurAMettreAJour.getRole().name());
            declarationPreparee.setString(6, utilisateurAMettreAJour.getStatutCompte().name());
            if (utilisateurAMettreAJour.getDerniereConnexion() != null) {
                declarationPreparee.setTimestamp(7, Timestamp.valueOf(utilisateurAMettreAJour.getDerniereConnexion()));
            } else {
                declarationPreparee.setNull(7, Types.TIMESTAMP);
            }
            declarationPreparee.setLong(8, utilisateurAMettreAJour.getId());

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour de l'utilisateur ID : {}, lignes affectées : {}", utilisateurAMettreAJour.getId(), lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour de l'utilisateur ID : {}", utilisateurAMettreAJour.getId(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour de l'utilisateur.", e);
        }
    }

    @Override
    public boolean supprimer(long idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_UTILISATEUR)) {

            declarationPreparee.setLong(1, idUtilisateur);
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