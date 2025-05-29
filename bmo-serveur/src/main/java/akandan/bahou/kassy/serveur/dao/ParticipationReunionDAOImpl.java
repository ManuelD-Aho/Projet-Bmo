package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleDansReunion;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticipationReunionDAOImpl implements InterfaceParticipationReunionDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(ParticipationReunionDAOImpl.class);

    // Noms de colonnes basés sur le schéma SQL de `docker-compose.yml` et `schema-initial.sql`
    private static final String TABLE_PARTICIPANTS = "participants_reunion"; // Nom de table corrigé
    private static final String COL_REUNION_ID = "reunion_id";
    private static final String COL_UTILISATEUR_ID = "utilisateur_id";
    private static final String COL_ROLE_DANS_REUNION = "role_dans_reunion";
    private static final String COL_STATUT_PARTICIPATION = "statut_participation";
    private static final String COL_HEURE_ENTREE = "heure_entree";
    private static final String COL_HEURE_SORTIE = "heure_sortie";

    private static final String TABLE_UTILISATEURS = "utilisateurs"; // Nom de table corrigé
    private static final String COL_UTIL_ID = "id";
    private static final String COL_UTIL_IDENTIFIANT = "identifiant_connexion"; // Corrigé
    private static final String COL_UTIL_NOM_COMPLET = "nom_complet"; // Corrigé
    private static final String COL_UTIL_ROLE_SYSTEME = "role_systeme";
    private static final String COL_UTIL_STATUT_COMPTE = "statut_compte";
    private static final String COL_UTIL_DATE_CREATION = "date_creation_compte";
    private static final String COL_UTIL_DERNIERE_CONNEXION = "derniere_connexion";
    // Les champs photo ne sont pas inclus dans DonneesUtilisateurDTO pour l'instant, donc omis de la jointure pour simplifier.

    private static final String REQUETE_AJOUTER_PARTICIPANT = "INSERT INTO " + TABLE_PARTICIPANTS +
            " (" + COL_REUNION_ID + ", " + COL_UTILISATEUR_ID + ", " + COL_ROLE_DANS_REUNION + ", " + COL_STATUT_PARTICIPATION + ", " + COL_HEURE_ENTREE + ") VALUES (?, ?, ?, ?, ?)";
    private static final String REQUETE_METTRE_A_JOUR_STATUT = "UPDATE " + TABLE_PARTICIPANTS + " SET " +
            COL_STATUT_PARTICIPATION + " = ?, " + COL_HEURE_ENTREE + " = CASE WHEN ? = ? THEN NOW() ELSE " + COL_HEURE_ENTREE + " END, " +
            COL_HEURE_SORTIE + " = CASE WHEN ? = ? THEN NOW() ELSE " + COL_HEURE_SORTIE + " END WHERE " +
            COL_REUNION_ID + " = ? AND " + COL_UTILISATEUR_ID + " = ?";
    private static final String REQUETE_METTRE_A_JOUR_ROLE = "UPDATE " + TABLE_PARTICIPANTS + " SET " +
            COL_ROLE_DANS_REUNION + " = ? WHERE " + COL_REUNION_ID + " = ? AND " + COL_UTILISATEUR_ID + " = ?";
    private static final String REQUETE_SUPPRIMER_PARTICIPANT = "DELETE FROM " + TABLE_PARTICIPANTS +
            " WHERE " + COL_REUNION_ID + " = ? AND " + COL_UTILISATEUR_ID + " = ?";
    private static final String REQUETE_RECUPERER_IDS_PARTICIPANTS = "SELECT " + COL_UTILISATEUR_ID + " FROM " + TABLE_PARTICIPANTS +
            " WHERE " + COL_REUNION_ID + " = ?";
    private static final String REQUETE_RECUPERER_IDS_PARTICIPANTS_ACTIFS = "SELECT " + COL_UTILISATEUR_ID + " FROM " + TABLE_PARTICIPANTS +
            " WHERE " + COL_REUNION_ID + " = ? AND " + COL_STATUT_PARTICIPATION + " = ?"; // Comparer avec la valeur ENUM exacte
    private static final String REQUETE_RECUPERER_STATUT_PARTICIPATION = "SELECT " + COL_STATUT_PARTICIPATION + " FROM " + TABLE_PARTICIPANTS +
            " WHERE " + COL_REUNION_ID + " = ? AND " + COL_UTILISATEUR_ID + " = ?";
    private static final String REQUETE_RECUPERER_ROLE_DANS_REUNION = "SELECT " + COL_ROLE_DANS_REUNION + " FROM " + TABLE_PARTICIPANTS +
            " WHERE " + COL_REUNION_ID + " = ? AND " + COL_UTILISATEUR_ID + " = ?";
    private static final String REQUETE_RECUPERER_PARTICIPANTS_DETAILS = "SELECT u." + COL_UTIL_ID + ", u." + COL_UTIL_IDENTIFIANT + ", u." + COL_UTIL_NOM_COMPLET +
            ", u." + COL_UTIL_ROLE_SYSTEME + ", u." + COL_UTIL_STATUT_COMPTE + ", u." + COL_UTIL_DATE_CREATION + ", u." + COL_UTIL_DERNIERE_CONNEXION +
            ", pr." + COL_ROLE_DANS_REUNION + ", pr." + COL_STATUT_PARTICIPATION +
            " FROM " + TABLE_UTILISATEURS + " u JOIN " + TABLE_PARTICIPANTS + " pr ON u." + COL_UTIL_ID + " = pr." + COL_UTILISATEUR_ID +
            " WHERE pr." + COL_REUNION_ID + " = ?";


    public ParticipationReunionDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    @Override
    public void ajouterParticipant(int idReunion, int idUtilisateur, RoleDansReunion role, StatutParticipationReunion statutInitial) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_AJOUTER_PARTICIPANT)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setInt(2, idUtilisateur);
            declarationPreparee.setString(3, role.name());
            declarationPreparee.setString(4, statutInitial.name());
            declarationPreparee.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            declarationPreparee.executeUpdate();
            journal.debug("Participant {} ajouté à la réunion {} avec le rôle {} et statut {}", idUtilisateur, idReunion, role, statutInitial);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de l'ajout du participant {} à la réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de l'ajout du participant.", e);
        }
    }

    @Override
    public boolean mettreAJourStatutParticipation(int idReunion, int idUtilisateur, StatutParticipationReunion nouveauStatut) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_STATUT)) {
            declarationPreparee.setString(1, nouveauStatut.name());
            declarationPreparee.setString(2, StatutParticipationReunion.REJOINT.name()); // Pour la condition CASE HEURE_ENTREE
            declarationPreparee.setString(3, StatutParticipationReunion.PARTI.name());   // Pour la condition CASE HEURE_SORTIE
            declarationPreparee.setInt(4, idReunion);
            declarationPreparee.setInt(5, idUtilisateur);

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour du statut de participation pour utilisateur {} dans réunion {}, nouveau statut {}, lignes affectées : {}", idUtilisateur, idReunion, nouveauStatut, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour du statut de participation pour utilisateur {} dans réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour du statut de participation.", e);
        }
    }

    @Override
    public boolean mettreAJourRoleDansReunion(int idReunion, int idUtilisateur, RoleDansReunion nouveauRole) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_ROLE)) {

            declarationPreparee.setString(1, nouveauRole.name());
            declarationPreparee.setInt(2, idReunion);
            declarationPreparee.setInt(3, idUtilisateur);

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour du rôle pour utilisateur {} dans réunion {}, nouveau rôle {}, lignes affectées : {}", idUtilisateur, idReunion, nouveauRole, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour du rôle pour utilisateur {} dans réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour du rôle dans la réunion.", e);
        }
    }

    @Override
    public void supprimerParticipant(int idReunion, int idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_PARTICIPANT)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setInt(2, idUtilisateur);

            declarationPreparee.executeUpdate();
            journal.debug("Participant {} supprimé de la réunion {}", idUtilisateur, idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la suppression du participant {} de la réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la suppression du participant.", e);
        }
    }

    @Override
    public List<Integer> recupererIdsParticipantsParIdReunion(int idReunion) throws ExceptionPersistance {
        List<Integer> idsParticipants = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_IDS_PARTICIPANTS)) {

            declarationPreparee.setInt(1, idReunion);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    idsParticipants.add(resultSet.getInt(COL_UTILISATEUR_ID));
                }
            }
            journal.debug("{} IDs de participants récupérés pour la réunion {}", idsParticipants.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des IDs de participants pour la réunion {}: {}", idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la récupération des IDs de participants.", e);
        }
        return idsParticipants;
    }

    @Override
    public List<Integer> recupererIdsParticipantsActifsParIdReunion(int idReunion) throws ExceptionPersistance {
        List<Integer> idsParticipantsActifs = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_IDS_PARTICIPANTS_ACTIFS)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setString(2, StatutParticipationReunion.REJOINT.name());
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    idsParticipantsActifs.add(resultSet.getInt(COL_UTILISATEUR_ID));
                }
            }
            journal.debug("{} IDs de participants actifs récupérés pour la réunion {}", idsParticipantsActifs.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des IDs de participants actifs pour la réunion {}: {}", idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la récupération des IDs de participants actifs.", e);
        }
        return idsParticipantsActifs;
    }

    @Override
    public StatutParticipationReunion recupererStatutParticipation(int idReunion, int idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_STATUT_PARTICIPATION)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setInt(2, idUtilisateur);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    String statutDb = resultSet.getString(COL_STATUT_PARTICIPATION);
                    try {
                        return StatutParticipationReunion.valueOf(statutDb);
                    } catch (IllegalArgumentException iae) {
                        journal.warn("Statut de participation inconnu '{}' pour utilisateur {} dans réunion {}", statutDb, idUtilisateur, idReunion);
                        return null;
                    }
                }
            }
            journal.debug("Aucun statut de participation trouvé pour utilisateur {} dans réunion {}", idUtilisateur, idReunion);
        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération du statut de participation pour utilisateur {} dans réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la récupération du statut de participation.", e);
        }
        return null;
    }

    @Override
    public RoleDansReunion recupererRoleDansReunion(int idReunion, int idUtilisateur) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_ROLE_DANS_REUNION)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setInt(2, idUtilisateur);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    String roleDb = resultSet.getString(COL_ROLE_DANS_REUNION);
                    try {
                        return RoleDansReunion.valueOf(roleDb);
                    } catch (IllegalArgumentException iae) {
                        journal.warn("Rôle dans réunion inconnu '{}' pour utilisateur {} dans réunion {}", roleDb, idUtilisateur, idReunion);
                        return null;
                    }
                }
            }
            journal.debug("Aucun rôle dans réunion trouvé pour utilisateur {} dans réunion {}", idUtilisateur, idReunion);
        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération du rôle dans réunion pour utilisateur {} dans réunion {}: {}", idUtilisateur, idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la récupération du rôle dans la réunion.", e);
        }
        return null;
    }

    private DonneesUtilisateurDTO hydraterDonneesUtilisateurDTO(ResultSet rs) throws SQLException {
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdUtilisateur(rs.getLong(COL_UTIL_ID));
        dto.setIdentifiant(rs.getString(COL_UTIL_IDENTIFIANT));
        dto.setNomComplet(rs.getString(COL_UTIL_NOM_COMPLET));
        String roleSystemeDb = rs.getString(COL_UTIL_ROLE_SYSTEME);
        if (roleSystemeDb != null) {
            try {
                dto.setRole(RoleUtilisateur.valueOf(roleSystemeDb));
            } catch (IllegalArgumentException e) {
                journal.warn("Rôle système DB inconnu: {}", roleSystemeDb);
            }
        }
        String statutCompteDb = rs.getString(COL_UTIL_STATUT_COMPTE);
        if (statutCompteDb != null) {
            try {
                dto.setStatutCompte(StatutCompteUtilisateur.valueOf(statutCompteDb));
            } catch (IllegalArgumentException e) {
                journal.warn("Statut compte DB inconnu: {}", statutCompteDb);
            }
        }
        Timestamp dateCreationSql = rs.getTimestamp(COL_UTIL_DATE_CREATION);
        if (dateCreationSql != null) {
            dto.setDateCreationCompte(dateCreationSql.toLocalDateTime());
        }
        Timestamp dateDerniereConnexionSql = rs.getTimestamp(COL_UTIL_DERNIERE_CONNEXION);
        if (dateDerniereConnexionSql != null) {
            dto.setDateDerniereConnexion(dateDerniereConnexionSql.toLocalDateTime());
        }
        // Pour enrichir avec le rôle et le statut dans la réunion, ces champs devraient être dans DonneesUtilisateurDTO
        // ou un DTO spécifique comme ParticipantDetailsDTO.
        // dto.setRoleDansReunion(RoleDansReunion.valueOf(rs.getString(COL_ROLE_DANS_REUNION)));
        // dto.setStatutParticipation(StatutParticipationReunion.valueOf(rs.getString(COL_STATUT_PARTICIPATION)));
        return dto;
    }

    @Override
    public List<DonneesUtilisateurDTO> recupererParticipantsDetailsParIdReunion(int idReunion) throws ExceptionPersistance {
        List<DonneesUtilisateurDTO> participantsDetails = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_PARTICIPANTS_DETAILS)) {

            declarationPreparee.setInt(1, idReunion);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    participantsDetails.add(hydraterDonneesUtilisateurDTO(resultSet));
                }
            }
            journal.debug("{} détails de participants récupérés pour la réunion {}", participantsDetails.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des détails de participants pour la réunion {}: {}", idReunion, e.getMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la récupération des détails de participants.", e);
        }
        return participantsDetails;
    }
}