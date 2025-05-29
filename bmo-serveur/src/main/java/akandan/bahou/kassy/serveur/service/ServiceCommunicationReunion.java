package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.dao.InterfaceMessageChatDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceParticipationReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceCommunicationReunion {

    private final InterfaceMessageChatDAO messageChatDAO;
    private final InterfaceParticipationReunionDAO participationReunionDAO;
    private final InterfaceUtilisateurDAO utilisateurDAO;
    private final Map<Long, List<ThreadClientDedie>> participantsConnectesParReunion; // idReunion est un long
    private static final Logger journal = LoggerFactory.getLogger(ServiceCommunicationReunion.class);

    public ServiceCommunicationReunion(InterfaceMessageChatDAO messageChatDAO, InterfaceParticipationReunionDAO participationReunionDAO, InterfaceUtilisateurDAO utilisateurDAO) {
        this.messageChatDAO = messageChatDAO;
        this.participationReunionDAO = participationReunionDAO;
        this.utilisateurDAO = utilisateurDAO;
        this.participantsConnectesParReunion = new ConcurrentHashMap<>();
    }

    public void traiterNouveauMessageChat(long idReunion, long idUtilisateurEmetteur, String contenuMessage) {
        journal.debug("Traitement du nouveau message de l'utilisateur ID {} pour la réunion ID {}", idUtilisateurEmetteur, idReunion);
        try {
            Optional<Utilisateur> emetteurOpt = this.utilisateurDAO.trouverParId((int)idUtilisateurEmetteur); // DAO attend int
            String nomEmetteur = emetteurOpt.map(Utilisateur::getNomComplet).orElse("Utilisateur Inconnu");

            MessageChatDTO messageDTO = new MessageChatDTO(
                    idReunion,
                    idUtilisateurEmetteur,
                    nomEmetteur,
                    contenuMessage,
                    LocalDateTime.now()
            );

            this.messageChatDAO.sauvegarderMessage(messageDTO); // Le DAO mettra à jour l'ID du messageDTO

            String messageProtocole = TypeReponseServeur.NOUVEAU_MESSAGE_CHAT.getValeurProtocole() +
                    ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + messageDTO.toJsonString();

            diffuserMessageAuxParticipants(idReunion, messageProtocole);
            journal.info("Message de '{}' pour réunion ID {} persisté et diffusé.", nomEmetteur, idReunion);

        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors du traitement du nouveau message chat pour réunion {}: {}", idReunion, e.getMessage(), e);
        } catch (JSONException e) {
            journal.error("Erreur JSON lors de la préparation du message chat pour diffusion (réunion {}): {}", idReunion, e.getMessage(), e);
        }
    }

    public List<MessageChatDTO> recupererHistoriqueMessages(long idReunion) throws ExceptionPersistance {
        journal.debug("Récupération de l'historique des messages pour la réunion ID {}", idReunion);
        List<MessageChatDTO> messages = this.messageChatDAO.recupererMessagesParIdReunion((int)idReunion); // DAO attend int
        for (MessageChatDTO msg : messages) {
            if (msg.getNomExpediteur() == null || msg.getNomExpediteur().isEmpty() || "Utilisateur Inconnu".equals(msg.getNomExpediteur())) {
                try {
                    Optional<Utilisateur> emetteurOpt = this.utilisateurDAO.trouverParId((int)msg.getIdExpediteur());
                    msg.setNomExpediteur(emetteurOpt.map(Utilisateur::getNomComplet).orElse("Utilisateur Inconnu"));
                } catch (ExceptionPersistance e) {
                    journal.warn("Impossible de récupérer le nom de l'émetteur pour le message ID {} : {}", msg.getIdMessage(), e.getMessage());
                }
            }
        }
        return messages;
    }

    public void enregistrerParticipantConnecte(long idReunion, ThreadClientDedie clientThread) {
        this.participantsConnectesParReunion.computeIfAbsent(idReunion, k -> new CopyOnWriteArrayList<>()).add(clientThread);
        journal.info("Client {} enregistré comme participant actif pour la réunion ID {}.", clientThread.getSocketClientCommunication().getRemoteSocketAddress(), idReunion);

        Long idUtilisateurAuthentifie = Long.valueOf(clientThread.obtenirIdUtilisateurAuthentifie());
        if (idUtilisateurAuthentifie != null) {
            try {
                this.participationReunionDAO.mettreAJourStatutParticipation((int)idReunion, idUtilisateurAuthentifie.intValue(), StatutParticipationReunion.REJOINT);
            } catch (ExceptionPersistance e) {
                journal.error("Erreur MAJ statut REJOINT pour utilisateur {} dans réunion {}: {}", idUtilisateurAuthentifie, idReunion, e.getMessage());
            }

            Optional<Utilisateur> utilisateurOpt = Optional.empty();
            try {
                utilisateurOpt = utilisateurDAO.trouverParId(idUtilisateurAuthentifie.intValue());
            } catch (ExceptionPersistance e) {
                journal.error("Erreur récupération détails utilisateur {} pour notification: {}", idUtilisateurAuthentifie, e.getMessage());
            }

            if (utilisateurOpt.isPresent()) {
                DonneesUtilisateurDTO participantDTO = new DonneesUtilisateurDTO(
                        utilisateurOpt.get().getId(),
                        utilisateurOpt.get().getIdentifiant(),
                        utilisateurOpt.get().getNomComplet(),
                        utilisateurOpt.get().getRole(),
                        utilisateurOpt.get().getStatutCompte(),
                        utilisateurOpt.get().getDateCreationCompte(),
                        utilisateurOpt.get().getDerniereConnexion()
                );
                try {
                    String notification = TypeReponseServeur.UTILISATEUR_REJOINT_REUNION.getValeurProtocole() +
                            ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + // Ajout de l'ID réunion
                            ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + participantDTO.toJsonString();
                    diffuserMessageAuxParticipants(idReunion, notification, clientThread); // Exclure celui qui vient de rejoindre
                } catch (JSONException e) {
                    journal.error("Erreur JSON en notifiant UTILISATEUR_REJOINT_REUNION: {}", e.getMessage());
                }
            }
        }
    }

    public void retirerParticipantConnecte(long idReunion, ThreadClientDedie clientThread) {
        List<ThreadClientDedie> participants = this.participantsConnectesParReunion.get(idReunion);
        if (participants != null) {
            boolean estRetire = participants.remove(clientThread);
            if (estRetire) {
                journal.info("Client {} retiré des participants actifs pour la réunion ID {}.", clientThread.getSocketClientCommunication().getRemoteSocketAddress(), idReunion);
                if (participants.isEmpty()) {
                    this.participantsConnectesParReunion.remove(idReunion);
                    journal.info("Plus aucun participant actif dans la réunion ID {}, liste nettoyée.", idReunion);
                }

                Long idUtilisateurAuthentifie = Long.valueOf(clientThread.obtenirIdUtilisateurAuthentifie());
                if (idUtilisateurAuthentifie != null) {
                    try {
                        this.participationReunionDAO.mettreAJourStatutParticipation((int)idReunion, idUtilisateurAuthentifie.intValue(), StatutParticipationReunion.PARTI);
                    } catch (ExceptionPersistance e) {
                        journal.error("Erreur MAJ statut PARTI pour utilisateur {} dans réunion {}: {}", idUtilisateurAuthentifie, idReunion, e.getMessage());
                    }

                    Optional<Utilisateur> utilisateurOpt = Optional.empty();
                    try {
                        utilisateurOpt = utilisateurDAO.trouverParId(idUtilisateurAuthentifie.intValue());
                    } catch (ExceptionPersistance e) {
                        journal.error("Erreur récupération détails utilisateur {} pour notification depart: {}", idUtilisateurAuthentifie, e.getMessage());
                    }
                    if (utilisateurOpt.isPresent()) {
                        DonneesUtilisateurDTO participantDTO = new DonneesUtilisateurDTO(
                                utilisateurOpt.get().getId(),
                                utilisateurOpt.get().getIdentifiant(),
                                utilisateurOpt.get().getNomComplet(),
                                utilisateurOpt.get().getRole(),
                                utilisateurOpt.get().getStatutCompte(),
                                utilisateurOpt.get().getDateCreationCompte(),
                                utilisateurOpt.get().getDerniereConnexion()
                        );
                        try {
                            String notification = TypeReponseServeur.UTILISATEUR_QUITTE_REUNION.getValeurProtocole() +
                                    ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + // Ajout de l'ID réunion
                                    ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + participantDTO.toJsonString();
                            diffuserMessageAuxParticipants(idReunion, notification);
                        } catch (JSONException e) {
                            journal.error("Erreur JSON en notifiant UTILISATEUR_QUITTE_REUNION: {}", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void notifierFermetureReunion(long idReunion) {
        journal.info("Notification de fermeture de la réunion ID {} à tous les participants connectés.", idReunion);
        String messageNotification = TypeReponseServeur.NOTIFICATION_REUNION_CLOTUREE.getValeurProtocole() + //Utiliser TypeReponseServeur.REUNION_TERMINEE si c'est celui-ci
                ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion;
        diffuserMessageAuxParticipants(idReunion, messageNotification);
        this.participantsConnectesParReunion.remove(idReunion);
        journal.info("Liste des participants pour la réunion ID {} vidée après notification de fermeture.", idReunion);
    }

    private void diffuserMessageAuxParticipants(long idReunion, String messageProtocole) {
        diffuserMessageAuxParticipants(idReunion, messageProtocole, null);
    }

    private void diffuserMessageAuxParticipants(long idReunion, String messageProtocole, ThreadClientDedie clientExclu) {
        List<ThreadClientDedie> participants = this.participantsConnectesParReunion.get(idReunion);
        if (participants != null && !participants.isEmpty()) {
            journal.debug("Diffusion du message à {} participant(s) pour la réunion ID {}: {}", participants.size(), idReunion, messageProtocole);
            for (ThreadClientDedie client : participants) {
                if (client != clientExclu && client.getSocketClientCommunication().isConnected() && !client.getSocketClientCommunication().isClosed()) {
                    client.envoyerReponseAuClient(messageProtocole);
                } else if (client != clientExclu) {
                    journal.warn("Tentative de diffusion à un client non connecté ou fermé pour la réunion ID {}: {}", idReunion, client.getSocketClientCommunication().getRemoteSocketAddress());
                    // Envisager de retirer ce client de la liste s'il est détecté comme déconnecté
                }
            }
        } else {
            journal.debug("Aucun participant connecté à qui diffuser le message pour la réunion ID {}.", idReunion);
        }
    }
}