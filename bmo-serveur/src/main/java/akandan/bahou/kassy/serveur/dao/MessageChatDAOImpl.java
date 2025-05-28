package akandan.bahou.kassy.serveur.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.serveur.modele.MessageChat;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageChatDAOImpl implements InterfaceMessageChatDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(MessageChatDAOImpl.class);

    private static final String REQUETE_SAUVEGARDER_MESSAGE = "INSERT INTO messages (meeting_id, user_id, content, timestamp) VALUES (?, ?, ?, ?)";
    private static final String REQUETE_RECUPERER_PAR_ID_REUNION = "SELECT id, meeting_id, user_id, content, timestamp FROM messages WHERE meeting_id = ? ORDER BY timestamp ASC";
    private static final String REQUETE_RECUPERER_PAR_ID_MESSAGE = "SELECT id, meeting_id, user_id, content, timestamp FROM messages WHERE id = ?";
    private static final String REQUETE_SUPPRIMER_MESSAGE = "DELETE FROM messages WHERE id = ?";
    private static final String REQUETE_METTRE_A_JOUR_MESSAGE = "UPDATE messages SET content = ? WHERE id = ?";

    public MessageChatDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private MessageChat hydraterMessageDepuisResultSet(ResultSet resultSet) throws SQLException {
        MessageChat entite = new MessageChat();
        entite.setIdMessage(resultSet.getLong("id"));
        entite.setIdReunion(resultSet.getInt("meeting_id"));
        entite.setIdUtilisateurEmetteur(resultSet.getInt("user_id"));
        entite.setContenuMessage(resultSet.getString("content"));
        Timestamp horodatageSql = resultSet.getTimestamp("timestamp");
        if (horodatageSql != null) {
            entite.setHorodatage(horodatageSql.toLocalDateTime());
        }
        return entite;
    }

    private MessageChatDTO convertirEntiteVersDTO(MessageChat entite) {
        if (entite == null) {
            return null;
        }
        MessageChatDTO dto = new MessageChatDTO();
        dto.setIdMessage(entite.getIdMessage());
        dto.setIdReunion(entite.getIdReunion());
        dto.setIdUtilisateurEmetteur(entite.getIdUtilisateurEmetteur());
        dto.setContenuMessage(entite.getContenuMessage());
        if (entite.getHorodatage() != null) {
            dto.setHorodatage(entite.getHorodatage().toString());
        }
        // nomUtilisateurEmetteur n'est pas dans l'entité MessageChat,
        // il sera peuplé par la couche service si nécessaire.
        return dto;
    }

    private MessageChat convertirDTOVersEntite(MessageChatDTO dto) {
        if (dto == null) {
            return null;
        }
        MessageChat entite = new MessageChat();
        entite.setIdMessage(dto.getIdMessage());
        entite.setIdReunion(dto.getIdReunion());
        entite.setIdUtilisateurEmetteur(dto.getIdUtilisateurEmetteur());
        entite.setContenuMessage(dto.getContenuMessage());
        if (dto.getHorodatage() != null && !dto.getHorodatage().isEmpty()) {
            try {
                entite.setHorodatage(LocalDateTime.parse(dto.getHorodatage()));
            } catch (java.time.format.DateTimeParseException e) {
                journal.warn("Format d'horodatage invalide pour le DTO lors de la conversion : {}", dto.getHorodatage(), e);
                // Gérer l'erreur ou laisser l'horodatage de l'entité à null
            }
        }
        return entite;
    }


    @Override
    public void sauvegarderMessage(MessageChatDTO messageDTO) throws ExceptionPersistance {
        MessageChat entite = convertirDTOVersEntite(messageDTO);
        if (entite.getHorodatage() == null) { // S'assurer que l'horodatage est défini
            entite.setHorodatage(LocalDateTime.now());
        }

        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SAUVEGARDER_MESSAGE, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setInt(1, entite.getIdReunion());
            declarationPreparee.setInt(2, entite.getIdUtilisateurEmetteur());
            declarationPreparee.setString(3, entite.getContenuMessage());
            declarationPreparee.setTimestamp(4, Timestamp.valueOf(entite.getHorodatage()));

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        messageDTO.setIdMessage(clesGenerees.getLong(1)); // Mettre à jour l'ID dans le DTO original
                    }
                }
            }
            journal.debug("Message sauvegardé pour la réunion ID : {}", entite.getIdReunion());

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la sauvegarde du message pour la réunion ID : {}", entite.getIdReunion(), e);
            throw new ExceptionPersistance("Erreur lors de la sauvegarde du message.", e);
        }
    }

    @Override
    public List<MessageChatDTO> recupererMessagesParIdReunion(int idReunion) throws ExceptionPersistance {
        List<MessageChatDTO> messagesDTO = new ArrayList<>();
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_PAR_ID_REUNION)) {

            declarationPreparee.setInt(1, idReunion);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                while (resultSet.next()) {
                    MessageChat entite = hydraterMessageDepuisResultSet(resultSet);
                    messagesDTO.add(convertirEntiteVersDTO(entite));
                }
            }
            journal.debug("{} messages récupérés pour la réunion ID : {}", messagesDTO.size(), idReunion);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération des messages pour la réunion ID : {}", idReunion, e);
            throw new ExceptionPersistance("Erreur lors de la récupération des messages.", e);
        }
        return messagesDTO;
    }

    @Override
    public MessageChatDTO recupererMessageParId(long idMessage) throws ExceptionPersistance {
        MessageChatDTO messageDTO = null;
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_RECUPERER_PAR_ID_MESSAGE)) {

            declarationPreparee.setLong(1, idMessage);
            try (ResultSet resultSet = declarationPreparee.executeQuery()) {
                if (resultSet.next()) {
                    MessageChat entite = hydraterMessageDepuisResultSet(resultSet);
                    messageDTO = convertirEntiteVersDTO(entite);
                }
            }
            journal.debug("Message récupéré pour l'ID : {}", idMessage);

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la récupération du message ID : {}", idMessage, e);
            throw new ExceptionPersistance("Erreur lors de la récupération du message.", e);
        }
        return messageDTO;
    }

    @Override
    public boolean supprimerMessage(long idMessage) throws ExceptionPersistance {
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SUPPRIMER_MESSAGE)) {

            declarationPreparee.setLong(1, idMessage);
            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Suppression du message ID : {}, lignes affectées : {}", idMessage, lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la suppression du message ID : {}", idMessage, e);
            throw new ExceptionPersistance("Erreur lors de la suppression du message.", e);
        }
    }

    @Override
    public boolean mettreAJourMessage(MessageChatDTO messageDTO) throws ExceptionPersistance {
        MessageChat entite = convertirDTOVersEntite(messageDTO);
        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_METTRE_A_JOUR_MESSAGE)) {

            declarationPreparee.setString(1, entite.getContenuMessage());
            declarationPreparee.setLong(2, entite.getIdMessage());

            int lignesAffectees = declarationPreparee.executeUpdate();
            journal.debug("Mise à jour du message ID : {}, lignes affectées : {}", entite.getIdMessage(), lignesAffectees);
            return lignesAffectees > 0;

        } catch (SQLException e) {
            journal.error("Erreur SQL lors de la mise à jour du message ID : {}", entite.getIdMessage(), e);
            throw new ExceptionPersistance("Erreur lors de la mise à jour du message.", e);
        }
    }
}