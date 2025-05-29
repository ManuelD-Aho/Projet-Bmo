package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.serveur.dao.InterfaceMessageChatDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceParticipationReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceCommunicationReunion {

    private final InterfaceMessageChatDAO messageChatDAO;
    private final InterfaceParticipationReunionDAO participationReunionDAO;
    private final InterfaceUtilisateurDAO utilisateurDAO;
    private final Map<Integer, List<ThreadClientDedie>> participantsConnectesParReunion;
    private static final Logger journal = LoggerFactory.getLogger(ServiceCommunicationReunion.class);

    public ServiceCommunicationReunion(InterfaceMessageChatDAO messageChatDAO, InterfaceParticipationReunionDAO participationReunionDAO, InterfaceUtilisateurDAO utilisateurDAO) {
        this.messageChatDAO = messageChatDAO;
        this.participationReunionDAO = participationReunionDAO;
        this.utilisateurDAO = utilisateurDAO;
        this.participantsConnectesParReunion = new ConcurrentHashMap<>();
    }

    public void traiterNouveauMessageChat(int idReunion, int idUtilisateurEmetteur, String contenuMessage) {
        journal.debug("Traitement du nouveau message de {} pour la réunion {}", idUtilisateurEmetteur, idReunion);
        try {
            Optional<Utilisateur> emetteurOpt = this.utilisateurDAO.trouverParId(idUtilisateurEmetteur);
            String nomEmetteur = emetteurOpt.map(Utilisateur::getNomComplet).orElse("Utilisateur Inconnu");

            MessageChatDTO messageDTO = new MessageChatDTO();
            messageDTO.setIdReunion(idReunion);
            messageDTO.setIdUtilisateurEmetteur(idUtilisateurEmetteur);
            messageDTO.setNomUtilisateurEmetteur(nomEmetteur);
            messageDTO.setContenuMessage(contenuMessage);
            messageDTO.setHorodatage(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            this.messageChatDAO.sauvegarderMessage(messageDTO);

            String messageProtocole = String.format("%s|%d|%d|%s|%s|%s",
                    TypeReponseServeur.NOUVEAU_MESSAGE_CHAT.getValeurProtocole(),
                    idReunion,
                    idUtilisateurEmetteur,
                    nomEmetteur,
                    messageDTO.getHorodatage(),
                    contenuMessage);

            diffuserMessageAuxParticipants(idReunion, TypeReponseServeur.NOUVEAU_MESSAGE_CHAT, messageProtocole);
            journal.info("Message de {} pour réunion {} persisté et diffusé.", nomEmetteur, idReunion);

        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors du traitement du nouveau message chat pour réunion {}: {}", idReunion, e.getMessage(), e);
        }
    }

    public List<MessageChatDTO> recupererHistoriqueMessages(int idReunion) throws ExceptionPersistance {
        journal.debug("Récupération de l'historique des messages pour la réunion {}", idReunion);
        List<MessageChatDTO> messages = this.messageChatDAO.recupererMessagesParIdReunion(idReunion);
        for (MessageChatDTO msg : messages) {
            if (msg.getNomUtilisateurEmetteur() == null || msg.getNomUtilisateurEmetteur().isEmpty() || "Utilisateur Inconnu".equals(msg.getNomUtilisateurEmetteur())) {
                try {
                    Optional<Utilisateur> emetteurOpt = this.utilisateurDAO.trouverParId(msg.getIdUtilisateurEmetteur());
                    msg.setNomUtilisateurEmetteur(emetteurOpt.map(Utilisateur::getNomComplet).orElse("Utilisateur Inconnu"));
                } catch (ExceptionPersistance e) {
                    journal.warn("Impossible de récupérer le nom de l'émetteur pour le message ID {} : {}", msg.getIdMessage(), e.getMessage());
                }
            }
        }
        return messages;
    }

    public void enregistrerParticipantConnecte(int idReunion, ThreadClientDedie clientThread) {
        this.participantsConnectesParReunion.computeIfAbsent(idReunion, k -> new CopyOnWriteArrayList<>()).add(clientThread);
        journal.info("Client {} enregistré comme participant actif pour la réunion {}.", clientThread.getSocketClientCommunication().getRemoteSocketAddress(), idReunion);

        try {
            if (clientThread.obtenirIdUtilisateurAuthentifie() != null) {
                this.participationReunionDAO.mettreAJourStatutParticipation(idReunion, clientThread.obtenirIdUtilisateurAuthentifie(), StatutParticipationReunion.REJOINT);
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur lors de la mise à jour du statut REJOINT pour l'utilisateur {} dans la réunion {}: {}", clientThread.obtenirIdUtilisateurAuthentifie(), idReunion, e.getMessage());
        }


        String nomUtilisateur = clientThread.obtenirNomUtilisateurAuthentifie() != null ? clientThread.obtenirNomUtilisateurAuthentifie() : "Un utilisateur";
        Integer idUtilisateur = clientThread.obtenirIdUtilisateurAuthentifie();

        if (idUtilisateur != null) {
            String notification = String.format("%s|%d|%d|%s", TypeReponseServeur.UTILISATEUR_REJOINT_REUNION.getValeurProtocole(), idReunion, idUtilisateur, nomUtilisateur);
            diffuserMessageAuxParticipants(idReunion, TypeReponseServeur.UTILISATEUR_REJOINT_REUNION, notification, clientThread);
        }
    }

    public void retirerParticipantConnecte(int idReunion, ThreadClientDedie clientThread) {
        List<ThreadClientDedie> participants = this.participantsConnectesParReunion.get(idReunion);
        if (participants != null) {
            boolean removed = participants.remove(clientThread);
            if (removed) {
                journal.info("Client {} retiré des participants actifs pour la réunion {}.", clientThread.getSocketClientCommunication().getRemoteSocketAddress(), idReunion);
                if (participants.isEmpty()) {
                    this.participantsConnectesParReunion.remove(idReunion);
                    journal.info("Plus aucun participant actif dans la réunion {}, liste nettoyée.", idReunion);
                }

                try {
                    if (clientThread.obtenirIdUtilisateurAuthentifie() != null) {
                        this.participationReunionDAO.mettreAJourStatutParticipation(idReunion, clientThread.obtenirIdUtilisateurAuthentifie(), StatutParticipationReunion.PARTI);
                    }
                } catch (ExceptionPersistance e) {
                    journal.error("Erreur lors de la mise à jour du statut PARTI pour l'utilisateur {} dans la réunion {}: {}", clientThread.obtenirIdUtilisateurAuthentifie(), idReunion, e.getMessage());
                }


                String nomUtilisateur = clientThread.obtenirNomUtilisateurAuthentifie() != null ? clientThread.obtenirNomUtilisateurAuthentifie() : "Un utilisateur";
                Integer idUtilisateur = clientThread.obtenirIdUtilisateurAuthentifie();

                if (idUtilisateur != null) {
                    String notification = String.format("%s|%d|%d|%s", TypeReponseServeur.UTILISATEUR_QUITTE_REUNION.getValeurProtocole(), idReunion, idUtilisateur, nomUtilisateur);
                    diffuserMessageAuxParticipants(idReunion, TypeReponseServeur.UTILISATEUR_QUITTE_REUNION, notification);
                }
            }
        }
    }

    public void notifierFermetureReunion(int idReunion) {
        journal.info("Notification de fermeture de la réunion ID {} à tous les participants connectés.", idReunion);
        String messageNotification = String.format("%s|%d",
                TypeReponseServeur.REUNION_TERMINEE.getValeurProtocole(),
                idReunion);
        diffuserMessageAuxParticipants(idReunion, TypeReponseServeur.REUNION_TERMINEE, messageNotification);
        this.participantsConnectesParReunion.remove(idReunion);
        journal.info("Liste des participants pour la réunion ID {} vidée après notification de fermeture.", idReunion);
    }


    private void diffuserMessageAuxParticipants(int idReunion, TypeReponseServeur typeReponse, String messageProtocole) {
        diffuserMessageAuxParticipants(idReunion, typeReponse, messageProtocole, null);
    }

    private void diffuserMessageAuxParticipants(int idReunion, TypeReponseServeur typeReponse, String messageProtocole, ThreadClientDedie clientExclu) {
        List<ThreadClientDedie> participants = this.participantsConnectesParReunion.get(idReunion);
        if (participants != null && !participants.isEmpty()) {
            journal.debug("Diffusion du message de type {} à {} participant(s) pour la réunion {}: {}", typeReponse.name(), participants.size(), idReunion, messageProtocole);
            for (ThreadClientDedie client : participants) {
                if (client != clientExclu && client.getSocketClientCommunication().isConnected() && !client.getSocketClientCommunication().isClosed()) {
                    client.envoyerReponseAuClient(messageProtocole);
                }
            }
        } else {
            journal.debug("Aucun participant connecté à qui diffuser le message pour la réunion {}.", idReunion);
        }
    }
}