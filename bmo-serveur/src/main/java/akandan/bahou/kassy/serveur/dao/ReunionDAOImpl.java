package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.Reunion;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReunionDAOImpl implements InterfaceReunionDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(ReunionDAOImpl.class);

    // Noms de colonnes basés sur le schéma SQL (généralement en minuscules_avec_tirets)
    private static final String TABLE_REUNIONS = "reunions";
    private static final String COL_ID = "id";
    private static final String COL_TITRE = "titre";
    private static final String COL_DESCRIPTION = "description"; // ou ordre_du_jour si c'est le nom exact de la colonne
    private static final String COL_DATE_HEURE_DEBUT = "date_heure_debut";
    private static final String COL_DUREE_MINUTES = "duree_minutes"; // ou duree_estimee_minutes
    private static final String COL_TYPE_REUNION = "type_reunion";
    private static final String COL_STATUT_REUNION = "statut_reunion";
    private static final String COL_ORGANISATEUR_ID = "organisateur_id";
    private static final String COL_MOT_DE_PASSE_REUNION = "mot_de_passe_reunion";
    private static final String COL_DATE_CREATION_REUNION = "date_creation_reunion";


    private static final String REQUETE_CREER_REUNION = "INSERT INTO " + TABLE_REUNIONS +
            " (" + COL_TITRE + ", " + COL_DESCRIPTION + ", " + COL_DATE_HEURE_DEBUT + ", " + COL_DUREE_MINUTES + ", " +
            COL_TYPE_REUNION + ", " + COL_STATUT_REUNION + ", " + COL_ORGANISATEUR_ID + ", " + COL_MOT_DE_PASSE_REUNION + ", " + COL_DATE_CREATION_REUNION +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REQUETE_TROUVER_PAR_ID = "SELECT * FROM " + TABLE_REUNIONS + " WHERE " + COL_ID + " = ?";
    private static final String REQUETE_TROUVER_TOUTES = "SELECT * FROM " + TABLE_REUNIONS + " ORDER BY " + COL_DATE_HEURE_DEBUT + " DESC";
    private static final String REQUETE_METTRE_A_JOUR_REUNION = "UPDATE " + TABLE_REUNIONS + " SET " +
            COL_TITRE + "=?, " + COL_DESCRIPTION + "=?, " + COL_DATE_HEURE_DEBUT + "=?, " + COL_DUREE_MINUTES + "=?, " +
            COL_TYPE_REUNION + "=?, " + COL_STATUT_REUNION + "=?, " + COL_MOT_DE_PASSE_REUNION + "=? " +
            "WHERE " + COL_ID + " = ?";
    private static final String REQUETE_SUPPRIMER_REUNION = "DELETE FROM " + TABLE_REUNIONS + " WHERE " + COL_ID + " = ?";
    private static final String REQUETE_TROUVER_PAR_ORGANISATEUR_ID = "SELECT * FROM " + TABLE_REUNIONS +
            " WHERE " + COL_ORGANISATEUR_ID + " = ? ORDER BY " + COL_DATE_HEURE_DEBUT + " DESC";
    private static final String REQUETE_TROUVER_PAR_STATUT = "SELECT * FROM " + TABLE_REUNIONS +
            " WHERE " + COL_STATUT_REUNION + " = ? ORDER BY " + COL_DATE_HEURE_DEBUT + " DESC";

    public ReunionDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private Reunion hydraterReunionDepuisResultSet(ResultSet rs) throws SQLException {
        Reunion reunion = new Reunion();
        reunion.setId(rs.getLong(COL_ID));
        reunion.setTitre(rs.getString(COL_TITRE));
        reunion.setDescription(rs.getString(COL_DESCRIPTION));
        Timestamp dateHeureDebutSql = rs.getTimestamp(COL_DATE_HEURE_DEBUT);
        if (dateHeureDebutSql != null) {
            reunion.setDateHeureDebut(dateHeureDebutSql.toLocalDateTime());
        }
        reunion.setDureeEstimeeMinutes(rs.getInt(COL_DUREE_MINUTES));
        reunion.setTypeReunion(TypeReunion.valueOf(rs.getString(COL_TYPE_REUNION)));
        reunion.setStatutReunion(StatutReunion.valueOf(rs.getString(COL_STATUT_REUNION)));
        reunion.setOrganisateurId(rs.getLong(COL_ORGANISATEUR_ID));
        reunion.setMotDePasseOptionnel(rs.getString(COL_MOT_DE_PASSE_REUNION));
        Timestamp dateCreationSql = rs.getTimestamp(COL_DATE_CREATION_REUNION);
        if (dateCreationSql != null) {
            reunion.setDateCreationReunion(dateCreationSql.toLocalDateTime());
        }
        return reunion;
    }

    @Override
    public Reunion creer(Reunion reunionACreer) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_CREER_REUNION, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setString(1, reunionACreer.getTitre());
            declarationPreparee.setString(2, reunionACreer.getDescription());
            declarationPreparee.setTimestamp(3, Timestamp.valueOf(reunionACreer.getDateHeureDebut()));
            declarationPreparee.setInt(4, reunionACreer.getDureeEstimeeMinutes());
            declarationPreparee.setString(5, reunionACreer.getTypeReunion().name());
            declarationPreparee.setString(6, reunionACreer.getStatutReunion().name());
            declarationPreparee.setLong(7, reunionACreer.getOrganisateurId());
            if (reunionACreer.getMotDePasseOptionnel() != null) {
                declarationPreparee.setString(8, reunionACreer.getMotDePasseOptionnel());
            } else {
                declarationPreparee.setNull(8, Types.VARCHAR);
            }
            declarationPreparee.setTimestamp(9, Timestamp.valueOf(reunionACreer.getDateCreationReunion()));

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        reunionACreer.setId(clesGenerees.getLong(1));
                    }
                }
            }
            journal.debug("Réunion créée avec ID : {}", reunionACreer.getId());
            return reunionACreer;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la création de la réunion '{}': {}", reunionACreer.getTitre(), e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la création de la réunion.", e);
        }
    }

    @Override
    public Reunion trouverParId(int idReunion) throws ExceptionPersistance { // Interface utilise int
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ID)) {

            declarationPreparee.setLong(1, idReunion); // cohérence avec l'ID en long
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    Reunion reunion = hydraterReunionDepuisResultSet(resultSet);
                    journal.debug("Réunion trouvée par ID : {}", idReunion);
                    return reunion;
                }
            }
            journal.debug("Aucune réunion trouvée pour ID : {}", idReunion);
            return null;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la recherche de la réunion par ID : {}", idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la recherche de la réunion par ID.", e);
        }
    }

    @Override
    public List<Reunion> trouverToutes() throws ExceptionPersistance {
        List<Reunion> reunions = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_TOUTES);
             ResultSet resultSet = declarationPreparee.executeQuery()) {

            while (resultSet.next()) {
                reunions.add(hydraterReunionDepuisResultSet(resultSet));
            }
            journal.debug("{} réunions trouvées au total.", reunions.size());

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération de toutes les réunions.", e);
            throw new ExceptionPersistance("Erreur lors de la récupération de toutes les réunions.", e);
        }
        return reunions;
    }

    @Override
    public boolean mettreAJour(Reunion reunionAMettreAJour) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_REUNION)) {

            declarationPreparee.setString(1, reunionAMettreAJour.getTitre());
            declarationPreparee.setString(2, reunionAMettreAJour.getDescription());
            declarationPreparee.setTimestamp(3, Timestamp.valueOf(reunionAMettreAJour.getDateHeureDebut()));
            declarationPreparee.setInt(4, reunionAMettreAJour.getDureeEstimeeMinutes());
            declarationPreparee.setString(5, reunionAMettreAJour.getTypeReunion().name());
            declarationPreparee.setString(6, reunionAMettreAJour.getStatutReunion().name());
            if (reunionAMettreAJour.getMotDePasseOptionnel() != null) {
                declarationPreparee.setString(7, reunionAMettreAJour.getMotDePasseOptionnel());
            } else {
                declarationPreparee.setNull(7, Types.VARCHAR);
            }
            declarationPreparee.setLong(8, reunionAMettreAJour.getId());

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour de la réunion ID : {}, lignes affectées : {}", reunionAMettreAJour.getId(), lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour de la réunion ID : {}", reunionAMettreAJour.getId(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour de la réunion.", e);
        }
    }

    @Override
    public boolean supprimer(int idReunion) throws ExceptionPersistance { // Interface utilise int
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_REUNION)) {

            declarationPreparee.setLong(1, idReunion); // cohérence avec l'ID en long
            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Suppression de la réunion ID : {}, lignes affectées : {}", idReunion, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la suppression de la réunion ID : {}", idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la suppression de la réunion.", e);
        }
    }

    @Override
    public List<Reunion> trouverParOrganisateurId(int idOrganisateur) throws ExceptionPersistance { // Interface utilise int
        List<Reunion> reunions = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ORGANISATEUR_ID)) {

            declarationPreparee.setLong(1, idOrganisateur); // cohérence
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    reunions.add(hydraterReunionDepuisResultSet(resultSet));
                }
            }
            journal.debug("{} réunions trouvées pour l'organisateur ID : {}", reunions.size(), idOrganisateur);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la recherche des réunions par organisateur ID : {}", idOrganisateur, e);
            throw new ExceptionPersistance("Erreur lors de la recherche des réunions par organisateur.", e);
        }
        return reunions;
    }

    @Override
    public List<Reunion> trouverParStatut(StatutReunion statut) throws ExceptionPersistance {
        List<Reunion> reunions = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_STATUT)) {

            declarationPreparee.setString(1, statut.name());
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    reunions.add(hydraterReunionDepuisResultSet(resultSet));
                }
            }
            journal.debug("{} réunions trouvées pour le statut : {}", reunions.size(), statut);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la recherche des réunions par statut : {}", statut, e);
            throw new ExceptionPersistance("Erreur lors de la recherche des réunions par statut.", e);
        }
        return reunions;
    }
}