package akandan.bahou.kassy.serveur.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleDansReunion;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
// import akandan.bahou.kassy.serveur.modele.ParticipationReunion; // Entité non définie dans le prompt, non utilisée directement.
import akandan.bahou.kassy.commun.util.ExceptionPersistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticipationReunionDAOImpl implements InterfaceParticipationReunionDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(ParticipationReunionDAOImpl.class);

    private static final String REQUETE_AJOUTER_PARTICIPANT = "INSERT INTO participants (meeting_id, user_id, role, status, join_time) VALUES (?, ?, ?, ?, ?)";
    private static final String REQUETE_METTRE_A_JOUR_STATUT = "UPDATE participants SET status = ?, join_time = CASE WHEN ? = 'REJOINT' THEN NOW() ELSE join_time END, leave_time = CASE WHEN ? = 'PARTI' THEN NOW() ELSE leave_time END WHERE meeting_id = ? AND user_id = ?";
    private static final String REQUETE_METTRE_A_JOUR_ROLE = "UPDATE participants SET role = ? WHERE meeting_id = ? AND user_id = ?";
    private static final String REQUETE_SUPPRIMER_PARTICIPANT = "DELETE FROM participants WHERE meeting_id = ? AND user_id = ?";
    private static final String REQUETE_RECUPERER_IDS_PARTICIPANTS = "SELECT user_id FROM participants WHERE meeting_id = ?";
    private static final String REQUETE_RECUPERER_IDS_PARTICIPANTS_ACTIFS = "SELECT user_id FROM participants WHERE meeting_id = ? AND status = 'joined'"; // 'joined' as per schema
    private static final String REQUETE_RECUPERER_STATUT_PARTICIPATION = "SELECT status FROM participants WHERE meeting_id = ? AND user_id = ?";
    private static final String REQUETE_RECUPERER_ROLE_DANS_REUNION = "SELECT role FROM participants WHERE meeting_id = ? AND user_id = ?";
    private static final String REQUETE_RECUPERER_PARTICIPANTS_DETAILS = "SELECT u.id AS utilisateur_id, u.login AS utilisateur_login, u.name AS utilisateur_nom, u.role AS utilisateur_role_systeme, u.photo AS utilisateur_photo, u.photo_mimetype AS utilisateur_photo_mimetype, u.date_created AS utilisateur_date_creation, pr.role AS role_dans_reunion, pr.status AS statut_participation FROM users u JOIN participants pr ON u.id = pr.user_id WHERE pr.meeting_id = ?";


    public ParticipationReunionDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    @Override
    public void ajouterParticipant(int idReunion, int idUtilisateur, RoleDansReunion role, StatutParticipationReunion statutInitial) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_AJOUTER_PARTICIPANT)) {

            declarationPreparee.setInt(1, idReunion);
            declarationPreparee.setInt(2, idUtilisateur);
            declarationPreparee.setString(3, role.name().toLowerCase()); // Schema uses lowercase
            declarationPreparee.setString(4, statutInitial.name().toLowerCase()); // Schema uses lowercase
            declarationPreparee.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            declarationPreparee.executeUpdate();
            journal.debug("Participant {} ajouté à la réunion {} avec le rôle {} et statut {}", idUtilisateur, idReunion, role, statutInitial);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de l'ajout du participant {} à la réunion {}", idUtilisateur, idReunion, e);
            throw new ExceptionPersistance("Erreur lors de l'ajout du participant.", e);
        }
    }

    @Override
    public boolean mettreAJourStatutParticipation(int idReunion, int idUtilisateur, StatutParticipationReunion nouveauStatut) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_STATUT)) {
            String statutDb = nouveauStatut.name().toLowerCase();
            declarationPreparee.setString(1, statutDb);
            declarationPreparee.setString(2, nouveauStatut.name()); // For CASE statement comparison, needs to match Java enum name
            declarationPreparee.setString(3, nouveauStatut.name()); // For CASE statement comparison
            declarationPreparee.setInt(4, idReunion);
            declarationPreparee.setInt(5, idUtilisateur);

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour du statut de participation pour utilisateur {} dans réunion {}, nouveau statut {}, lignes affectées : {}", idUtilisateur, idReunion, nouveauStatut, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour du statut de participation pour utilisateur {} dans réunion {}", idUtilisateur, idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour du statut de participation.", e);
        }
    }

    @Override
    public boolean mettreAJourRoleDansReunion(int idReunion, int idUtilisateur, RoleDansReunion nouveauRole) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_ROLE)) {

            declarationPreparee.setString(1, nouveauRole.name().toLowerCase()); // Schema uses lowercase
            declarationPreparee.setInt(2, idReunion);
            declarationPreparee.setInt(3, idUtilisateur);

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour du rôle pour utilisateur {} dans réunion {}, nouveau rôle {}, lignes affectées : {}", idUtilisateur, idReunion, nouveauRole, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour du rôle pour utilisateur {} dans réunion {}", idUtilisateur, idReunion, e);
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
            journal.error("Erreur SQL lors de la suppression du participant {} de la réunion {}", idUtilisateur, idReunion, e);
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
                    idsParticipants.add(resultSet.getInt("user_id"));
                }
            }
            journal.debug("{} IDs de participants récupérés pour la réunion {}", idsParticipants.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des IDs de participants pour la réunion {}", idReunion, e);
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
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    idsParticipantsActifs.add(resultSet.getInt("user_id"));
                }
            }
            journal.debug("{} IDs de participants actifs récupérés pour la réunion {}", idsParticipantsActifs.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des IDs de participants actifs pour la réunion {}", idReunion, e);
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
                    String statutDb = resultSet.getString("status");
                    try {
                        return StatutParticipationReunion.valueOf(statutDb.toUpperCase()); // DB uses lowercase
                    } catch (IllegalArgumentException iae) {
                        journal.warn("Statut de participation inconnu '{}' pour utilisateur {} dans réunion {}", statutDb, idUtilisateur, idReunion);
                        return null;
                    }
                }
            }
            journal.debug("Aucun statut de participation trouvé pour utilisateur {} dans réunion {}", idUtilisateur, idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération du statut de participation pour utilisateur {} dans réunion {}", idUtilisateur, idReunion, e);
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
                    String roleDb = resultSet.getString("role");
                    try {
                        return RoleDansReunion.valueOf(roleDb.toUpperCase()); // DB uses lowercase
                    } catch (IllegalArgumentException iae) {
                        journal.warn("Rôle dans réunion inconnu '{}' pour utilisateur {} dans réunion {}", roleDb, idUtilisateur, idReunion);
                        return null;
                    }
                }
            }
            journal.debug("Aucun rôle dans réunion trouvé pour utilisateur {} dans réunion {}", idUtilisateur, idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération du rôle dans réunion pour utilisateur {} dans réunion {}", idUtilisateur, idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la récupération du rôle dans la réunion.", e);
        }
        return null;
    }

    private Utilisateur hydraterUtilisateurEntiteDepuisResultSet(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(rs.getInt("utilisateur_id"));
        utilisateur.setIdentifiantConnexion(rs.getString("utilisateur_login"));
        utilisateur.setNomComplet(rs.getString("utilisateur_nom"));
        // Le motDePasseHache et selMotDePasse ne sont pas récupérés ici.
        String roleSystemeDb = rs.getString("utilisateur_role_systeme");
        if (roleSystemeDb != null) {
            if ("ADMIN".equalsIgnoreCase(roleSystemeDb)) {
                utilisateur.setRoleSysteme(RoleUtilisateur.ADMINISTRATEUR);
            } else if ("USER".equalsIgnoreCase(roleSystemeDb)) {
                utilisateur.setRoleSysteme(RoleUtilisateur.PARTICIPANT); // ou ORGANISATEUR selon logique métier externe
            }
        }
        // statutCompte n'est pas dans la table users du schema
        Timestamp dateCreationSql = rs.getTimestamp("utilisateur_date_creation");
        if (dateCreationSql != null) {
            utilisateur.setDateCreationCompte(dateCreationSql.toLocalDateTime());
        }
        // dateDerniereConnexion n'est pas dans la table users du schema
        utilisateur.setPhoto(rs.getBytes("utilisateur_photo"));
        utilisateur.setPhotoMimeType(rs.getString("utilisateur_photo_mimetype"));
        return utilisateur;
    }

    private DonneesUtilisateurDTO convertirUtilisateurEntiteVersDTO(Utilisateur entite) {
        if (entite == null) {
            return null;
        }
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdUtilisateur(entite.getId());
        dto.setIdentifiantConnexion(entite.getIdentifiantConnexion());
        dto.setNomComplet(entite.getNomComplet());
        dto.setRoleUtilisateur(entite.getRoleSysteme()); // Assumes Utilisateur entity has RoleUtilisateur
        // dto.setStatutCompte(entite.getStatutCompte()); // Assumes Utilisateur entity has StatutCompteUtilisateur
        // if (entite.getDateDerniereConnexion() != null) {
        //    dto.setDateDerniereConnexion(entite.getDateDerniereConnexion().toString());
        // }
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
                    Utilisateur utilisateurEntite = hydraterUtilisateurEntiteDepuisResultSet(resultSet);
                    DonneesUtilisateurDTO dto = convertirUtilisateurEntiteVersDTO(utilisateurEntite);

                    // Ici, on pourrait enrichir le DTO avec les informations de participation si le DTO le permettait
                    // String roleDansReunionDb = resultSet.getString("role_dans_reunion");
                    // String statutParticipationDb = resultSet.getString("statut_participation");
                    // Si DonneesUtilisateurDTO avait des champs pour cela, on les setterait ici.
                    // Par exemple: dto.setRoleSpecificAReunion(RoleDansReunion.valueOf(roleDansReunionDb.toUpperCase()));

                    participantsDetails.add(dto);
                }
            }
            journal.debug("{} détails de participants récupérés pour la réunion {}", participantsDetails.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des détails de participants pour la réunion {}", idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la récupération des détails de participants.", e);
        }
        return participantsDetails;
    }
}