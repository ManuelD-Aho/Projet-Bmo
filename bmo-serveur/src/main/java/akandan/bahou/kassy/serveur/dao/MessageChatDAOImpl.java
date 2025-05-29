package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.MessageChat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageChatDAOImpl implements InterfaceMessageChatDAO {

    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions;
    private static final Logger journal = LoggerFactory.getLogger(MessageChatDAOImpl.class);

    private static final String REQUETE_SAUVEGARDER_MESSAGE = "INSERT INTO messages_chat (reunion_id, utilisateur_id, contenu_message, horodatage) VALUES (?, ?, ?, ?)";
    private static final String REQUETE_RECUPERER_PAR_ID_REUNION = "SELECT id, reunion_id, utilisateur_id, contenu_message, horodatage FROM messages_chat WHERE reunion_id = ? ORDER BY horodatage ASC";
    private static final String REQUETE_RECUPERER_PAR_ID_MESSAGE = "SELECT id, reunion_id, utilisateur_id, contenu_message, horodatage FROM messages_chat WHERE id = ?";
    private static final String REQUETE_SUPPRIMER_MESSAGE = "DELETE FROM messages_chat WHERE id = ?";
    private static final String REQUETE_METTRE_A_JOUR_MESSAGE = "UPDATE messages_chat SET contenu_message = ? WHERE id = ?";

    public MessageChatDAOImpl(GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexions) {
        this.gestionnaireDeConnexions = gestionnaireDeConnexions;
    }

    private MessageChat hydraterMessageDepuisResultSet(ResultSet resultSet) throws SQLException {
        MessageChat entite = new MessageChat();
        entite.setIdMessage(resultSet.getLong("id"));
        entite.setIdReunion(resultSet.getLong("reunion_id"));
        entite.setIdUtilisateurEmetteur(resultSet.getLong("utilisateur_id"));
        entite.setContenuMessage(resultSet.getString("contenu_message"));
        Timestamp horodatageSql = resultSet.getTimestamp("horodatage");
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
        dto.setIdExpediteur(entite.getIdUtilisateurEmetteur()); // Corrigé: DTO utilise idExpediteur
        // Le nom de l'expéditeur sera ajouté par la couche service si nécessaire.
        dto.setContenu(entite.getContenuMessage()); // Corrigé: DTO utilise contenu
        dto.setHorodatage(entite.getHorodatage());
        return dto;
    }

    private MessageChat convertirDTOVersEntite(MessageChatDTO dto) {
        if (dto == null) {
            return null;
        }
        MessageChat entite = new MessageChat();
        entite.setIdMessage(dto.getIdMessage());
        entite.setIdReunion(dto.getIdReunion());
        entite.setIdUtilisateurEmetteur(dto.getIdExpediteur()); // Corrigé: DTO utilise idExpediteur
        entite.setContenuMessage(dto.getContenu()); // Corrigé: DTO utilise contenu
        entite.setHorodatage(dto.getHorodatage());
        return entite;
    }

    @Override
    public void sauvegarderMessage(MessageChatDTO messageDTO) throws ExceptionPersistance {
        MessageChat entite = convertirDTOVersEntite(messageDTO);
        if (entite.getHorodatage() == null) {
            entite.setHorodatage(LocalDateTime.now());
        }

        try (Connection connexion = gestionnaireDeConnexions.etablirNouvelleConnexion();
             PreparedStatement declarationPreparee = connexion.prepareStatement(REQUETE_SAUVEGARDER_MESSAGE, Statement.RETURN_GENERATED_KEYS)) {

            declarationPreparee.setLong(1, entite.getIdReunion());
            declarationPreparee.setLong(2, entite.getIdUtilisateurEmetteur());
            declarationPreparee.setString(3, entite.getContenuMessage());
            declarationPreparee.setTimestamp(4, Timestamp.valueOf(entite.getHorodatage()));

            int lignesAffectees = declarationPreparee.executeUpdate();
            if (lignesAffectees > 0) {
                try (ResultSet clesGenerees = declarationPreparee.getGeneratedKeys()) {
                    if (clesGenerees.next()) {
                        messageDTO.setIdMessage(clesGenerees.getLong(1));
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

            declarationPreparee.setInt(1, idReunion); // La colonne BD est reunion_id (INT)
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