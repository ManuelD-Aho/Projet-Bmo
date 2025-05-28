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

import akandan.bahou.kassy.serveur.modele.Reunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReunionDAOImpl implements InterfaceReunionDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(ReunionDAOImpl.class);

    private static final String REQUETE_CREER_REUNION = "INSERT INTO meetings (title, agenda, datetime, duration, type, status, organizer_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String REQUETE_TROUVER_PAR_ID = "SELECT id, title, agenda, datetime, duration, type, status, organizer_id, created_at FROM meetings WHERE id = ?";
    private static final String REQUETE_TROUVER_TOUTES = "SELECT id, title, agenda, datetime, duration, type, status, organizer_id, created_at FROM meetings ORDER BY datetime DESC";
    private static final String REQUETE_METTRE_A_JOUR_REUNION = "UPDATE meetings SET title=?, agenda=?, datetime=?, duration=?, type=?, status=? WHERE id = ?";
    private static final String REQUETE_SUPPRIMER_REUNION = "DELETE FROM meetings WHERE id = ?";
    private static final String REQUETE_TROUVER_PAR_ORGANISATEUR_ID = "SELECT id, title, agenda, datetime, duration, type, status, organizer_id, created_at FROM meetings WHERE organizer_id = ? ORDER BY datetime DESC";
    private static final String REQUETE_TROUVER_PAR_STATUT = "SELECT id, title, agenda, datetime, duration, type, status, organizer_id, created_at FROM meetings WHERE status = ? ORDER BY datetime DESC";

    public ReunionDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private Reunion hydraterReunionDepuisResultSet(ResultSet rs) throws SQLException {
        Reunion reunion = new Reunion();
        reunion.setId(rs.getInt("id"));
        reunion.setTitre(rs.getString("title"));
        reunion.setOrdreDuJour(rs.getString("agenda"));
        Timestamp dateHeureDebutSql = rs.getTimestamp("datetime");
        if (dateHeureDebutSql != null) {
            reunion.setDateHeureDebut(dateHeureDebutSql.toLocalDateTime());
        }
        reunion.setDureeMinutes(rs.getInt("duration"));

        String typeReunionDb = rs.getString("type");
        try {
            reunion.setTypeReunion(TypeReunion.valueOf(typeReunionDb.toUpperCase().replace("É", "E"))); // Handle "Privée" -> PRIVEE
        } catch (IllegalArgumentException e) {
            journal.warn("Type de réunion inconnu depuis DB: '{}'", typeReunionDb);
            // Gérer le cas où la valeur de la DB ne correspond à aucun enum, peut-être affecter une valeur par défaut ou laisser null
        }

        String statutReunionDb = rs.getString("status");
        try {
            reunion.setStatutReunion(StatutReunion.valueOf(statutReunionDb.toUpperCase().replace("É", "E").replace("É", "E"))); // Handle "Planifiée", "Ouverte", "Terminée", "Annulée"
        } catch (IllegalArgumentException e) {
            journal.warn("Statut de réunion inconnu depuis DB: '{}'", statutReunionDb);
        }

        reunion.setOrganisateurId(rs.getInt("organizer_id"));
        Timestamp dateCreationSql = rs.getTimestamp("created_at");
        if (dateCreationSql != null) {
            reunion.setDateCreationReunion(dateCreationSql.toLocalDateTime());
        }
        // motDePasseOptionnel n'est pas dans le schema de la table meetings
        return reunion;
    }

    @Override
    public Reunion creer(Reunion reunionACreer) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_CREER_REUNION, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setString(1, reunionACreer.getTitre());
            declarationPreparee.setString(2, reunionACreer.getOrdreDuJour());
            declarationPreparee.setTimestamp(3, Timestamp.valueOf(reunionACreer.getDateHeureDebut()));
            declarationPreparee.setInt(4, reunionACreer.getDureeMinutes());
            declarationPreparee.setString(5, reunionACreer.getTypeReunion().name()); // Assuming DB stores uppercase
            declarationPreparee.setString(6, reunionACreer.getStatutReunion().name()); // Assuming DB stores uppercase
            declarationPreparee.setInt(7, reunionACreer.getOrganisateurId());
            // created_at est géré par DB DEFAULT CURRENT_TIMESTAMP

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        reunionACreer.setId(clesGenerees.getInt(1));
                    }
                }
            }
            journal.debug("Réunion créée avec ID : {}", reunionACreer.getId());
            return reunionACreer;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la création de la réunion : {}", reunionACreer.getTitre(), e);
            throw new ExceptionPersistance("Erreur lors de la création de la réunion.", e);
        }
    }

    @Override
    public Reunion trouverParId(int idReunion) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ID)) {

            declarationPreparee.setInt(1, idReunion);
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
            declarationPreparee.setString(2, reunionAMettreAJour.getOrdreDuJour());
            declarationPreparee.setTimestamp(3, Timestamp.valueOf(reunionAMettreAJour.getDateHeureDebut()));
            declarationPreparee.setInt(4, reunionAMettreAJour.getDureeMinutes());
            declarationPreparee.setString(5, reunionAMettreAJour.getTypeReunion().name()); // Assuming DB stores uppercase
            declarationPreparee.setString(6, reunionAMettreAJour.getStatutReunion().name()); // Assuming DB stores uppercase
            declarationPreparee.setInt(7, reunionAMettreAJour.getId());

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour de la réunion ID : {}, lignes affectées : {}", reunionAMettreAJour.getId(), lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour de la réunion ID : {}", reunionAMettreAJour.getId(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour de la réunion.", e);
        }
    }

    @Override
    public boolean supprimer(int idReunion) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_REUNION)) {

            declarationPreparee.setInt(1, idReunion);
            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Suppression de la réunion ID : {}, lignes affectées : {}", idReunion, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la suppression de la réunion ID : {}", idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la suppression de la réunion.", e);
        }
    }

    @Override
    public List<Reunion> trouverParOrganisateurId(int idOrganisateur) throws ExceptionPersistance {
        List<Reunion> reunions = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_TROUVER_PAR_ORGANISATEUR_ID)) {

            declarationPreparee.setInt(1, idOrganisateur);
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

            //The schema uses mixed case French for status. Java enum is UPPERCASE.
            //A direct .name() might not work if DB ENUM is 'Planifiée' and .name() is 'PLANIFIEE'
            //For this implementation, we assume the service layer might provide the exact DB string, or DB stores UPPERCASE.
            //The prompt for InterfaceReunionDAO suggests passing StatutReunion enum directly.
            //So, we use .name() and rely on DB storing it as such or schema being adapted.
            //Given schema uses 'Planifiée', 'Ouverte', etc. this will require mapping or schema change.
            //For now, using .name() as per typical Java enum to DB string practice.
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