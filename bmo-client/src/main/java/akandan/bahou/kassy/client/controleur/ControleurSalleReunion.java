package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;
import java.time.LocalDateTime;


public class ControleurSalleReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreReunionSalle;
    @FXML private Label etiquetteStatutSalle;
    @FXML private ListView<String> listViewParticipants;
    @FXML private TextArea textAreaChatHistorique;
    @FXML private TextField champSaisieMessageChat;
    @FXML private Button boutonEnvoyerMessageChat;
    @FXML private Button boutonQuitterReunion;
    @FXML private Button boutonLeverMain;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private DetailsReunionDTO detailsReunionActuelleDansSalle;
    private final ObservableList<String> listeObservableAffichageParticipants = FXCollections.observableArrayList();
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurSalleReunion.class);
    private final DateTimeFormatter formateurHorodatageChat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private boolean mainLevee = false;


    public ControleurSalleReunion() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        if (donnees != null && donnees.length > 0 && donnees[0] instanceof DetailsReunionDTO) {
            this.detailsReunionActuelleDansSalle = (DetailsReunionDTO) donnees[0];
        } else if (serviceComm.detailsReunionActuelleProperty().get() != null) { // Au cas où la navigation est faite après que la propriété soit déjà mise à jour
            this.detailsReunionActuelleDansSalle = serviceComm.detailsReunionActuelleProperty().get();
        }

        if (this.detailsReunionActuelleDansSalle == null) {
            journal.error("Impossible d'entrer dans la salle de réunion sans détails de réunion valides.");
            AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.meeting.room.entry.title"),
                    paquetRessourcesI18n.getString("error.meeting.room.entry.content.nodetails")
            );
            Platform.runLater(gestionnaireNavigation::afficherTableauDeBord);
            return;
        }

        mettreAJourAffichageInitialSalle();

        serviceComm.dernierMessageChatRecuProperty().addListener((obs, ancienMsg, nouveauMsg) -> {
            if (nouveauMsg != null && detailsReunionActuelleDansSalle != null && nouveauMsg.getIdReunion() == detailsReunionActuelleDansSalle.getIdReunion()) {
                afficherNouveauMessageChat(nouveauMsg);
            }
        });
        serviceComm.historiqueMessagesReunionProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            if (detailsReunionActuelleDansSalle != null) { // S'assurer qu'on est bien dans une salle
                chargerHistoriqueChat(nouvelleListe);
            }
        });
        serviceComm.detailsReunionActuelleProperty().addListener((obs, ancienneValeur, nouvelleValeur) -> {
            if (nouvelleValeur != null && detailsReunionActuelleDansSalle != null && nouvelleValeur.getIdReunion() == detailsReunionActuelleDansSalle.getIdReunion()) {
                mettreAJourEtatSalleReunion(nouvelleValeur);
            }
        });
        serviceComm.notificationUtilisateurRejointProperty().addListener((obs, ancienUtil, nouveauUtil) -> {
            if (nouveauUtil != null && detailsReunionActuelleDansSalle != null) { // Supposer que c'est pour la réunion actuelle
                // Le DTO DetailsReunionDTO devrait être mis à jour par le serveur et actualiser la liste complète
                // On pourrait aussi ajouter l'utilisateur directement ici si le DTO de la réunion n'est pas renvoyé
                journal.info("Utilisateur {} a rejoint la réunion (notification reçue).", nouveauUtil.getNomComplet());
                // Rafraîchir les détails de la réunion pour obtenir la liste de participants à jour
                serviceCommunicationServeur.envoyerRequeteObtenirDetailsReunion(detailsReunionActuelleDansSalle.getIdReunion());
            }
        });
        serviceComm.notificationUtilisateurQuitteProperty().addListener((obs, ancienUtil, nouveauUtil) -> {
            if (nouveauUtil != null && detailsReunionActuelleDansSalle != null) {
                journal.info("Utilisateur {} a quitté la réunion (notification reçue).", nouveauUtil.getNomComplet());
                serviceCommunicationServeur.envoyerRequeteObtenirDetailsReunion(detailsReunionActuelleDansSalle.getIdReunion());
            }
        });
        serviceComm.notificationReunionClotureeIdProperty().addListener((obs, ancienId, nouveauId) -> {
            if (nouveauId != null && detailsReunionActuelleDansSalle != null && nouveauId.intValue() == detailsReunionActuelleDansSalle.getIdReunion()) {
                Platform.runLater(() -> {
                    AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.status.changed.title"), paquetRessourcesI18n.getString("meeting.status.closed_by_organizer"));
                    actionQuitterReunion(null);
                });
            }
        });

        listViewParticipants.setItems(listeObservableAffichageParticipants);
        textAreaChatHistorique.setEditable(false);
        textAreaChatHistorique.setWrapText(true);

        serviceCommunicationServeur.envoyerRequeteObtenirHistoriqueMessages(detailsReunionActuelleDansSalle.getIdReunion());
        // Les détails de la réunion (incluant les participants) sont déjà dans detailsReunionActuelleDansSalle
        // ou seront mis à jour via le listener sur detailsReunionActuelleProperty.
    }

    private void mettreAJourAffichageInitialSalle() {
        etiquetteTitreReunionSalle.setText(detailsReunionActuelleDansSalle.getTitre());
        mettreAJourEtatSalleReunion(detailsReunionActuelleDansSalle); // Pour le statut et les participants
    }


    @FXML
    private void initialize() {
        if (paquetRessourcesI18n != null) {
            boutonEnvoyerMessageChat.setText(paquetRessourcesI18n.getString("meeting.room.button.send"));
            boutonQuitterReunion.setText(paquetRessourcesI18n.getString("meeting.room.button.quit"));
            boutonLeverMain.setText(paquetRessourcesI18n.getString("meeting.room.button.raiseHand"));
            champSaisieMessageChat.setPromptText(paquetRessourcesI18n.getString("meeting.room.textfield.chatPrompt"));
        }
        champSaisieMessageChat.setOnAction(event -> actionEnvoyerMessageChat(null));
    }

    @FXML
    private void actionEnvoyerMessageChat(ActionEvent evenement) {
        String contenuMessage = champSaisieMessageChat.getText().trim();
        if (contenuMessage.isEmpty() || detailsReunionActuelleDansSalle == null) {
            return;
        }
        DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
        if (utilisateurActuel == null) {
            journal.warn("Tentative d'envoi de message chat sans utilisateur connecté.");
            return;
        }

        MessageChatDTO messageDTO = new MessageChatDTO(
                detailsReunionActuelleDansSalle.getIdReunion(),
                utilisateurActuel.getIdUtilisateur(),
                utilisateurActuel.getNomComplet(),
                contenuMessage,
                LocalDateTime.now() // Le serveur devrait utiliser son propre horodatage
        );
        serviceCommunicationServeur.envoyerRequeteMessageChat(messageDTO);
        champSaisieMessageChat.clear();
    }

    @FXML
    private void actionQuitterReunion(ActionEvent evenement) {
        if (detailsReunionActuelleDansSalle != null) {
            serviceCommunicationServeur.envoyerRequeteQuitterReunion(detailsReunionActuelleDansSalle.getIdReunion());
        }
        gestionnaireNavigation.afficherTableauDeBord();
    }

    @FXML
    private void actionLeverMain(ActionEvent evenement) {
        mainLevee = !mainLevee;
        if (mainLevee) {
            boutonLeverMain.setText(paquetRessourcesI18n.getString("meeting.room.button.lowerHand"));
            // Envoyer notification au serveur si nécessaire
            journal.info("Utilisateur a levé la main.");
        } else {
            boutonLeverMain.setText(paquetRessourcesI18n.getString("meeting.room.button.raiseHand"));
            journal.info("Utilisateur a baissé la main.");
        }
        // Exemple: serviceCommunicationServeur.envoyerRequeteEtatMain(detailsReunionActuelleDansSalle.getIdReunion(), mainLevee);
    }

    private void afficherNouveauMessageChat(MessageChatDTO message) {
        Platform.runLater(() -> {
            String messageFormatte = String.format("[%s] %s: %s\n",
                    message.getHorodatage() != null ? message.getHorodatage().format(formateurHorodatageChat) : "??:??:??",
                    message.getNomExpediteur() != null ? message.getNomExpediteur() : paquetRessourcesI18n.getString("unknown.user"),
                    message.getContenu()
            );
            textAreaChatHistorique.appendText(messageFormatte);
            textAreaChatHistorique.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void chargerHistoriqueChat(List<MessageChatDTO> historique) {
        Platform.runLater(() -> {
            textAreaChatHistorique.clear();
            if (historique != null) {
                historique.forEach(this::afficherNouveauMessageChat);
            }
        });
    }

    private void mettreAJourEtatSalleReunion(DetailsReunionDTO detailsMisAJour) {
        this.detailsReunionActuelleDansSalle = detailsMisAJour;
        Platform.runLater(() -> {
            etiquetteTitreReunionSalle.setText(detailsMisAJour.getTitre());
            etiquetteStatutSalle.setText(paquetRessourcesI18n.getString("meeting.status." + detailsMisAJour.getStatutReunion().name().toLowerCase()));

            mettreAJourListeAffichageParticipants(detailsMisAJour.getParticipantsDTO());

            boolean peutInteragir = detailsMisAJour.getStatutReunion() == StatutReunion.OUVERTE;
            champSaisieMessageChat.setDisable(!peutInteragir);
            boutonEnvoyerMessageChat.setDisable(!peutInteragir);
            boutonLeverMain.setDisable(!peutInteragir);

            if (detailsMisAJour.getStatutReunion() == StatutReunion.CLOTUREE || detailsMisAJour.getStatutReunion() == StatutReunion.ANNULEE) {
                AlertesUtilisateur.afficherInformation(
                        paquetRessourcesI18n.getString("meeting.status.changed.title"),
                        paquetRessourcesI18n.getString("meeting.status.closed_or_cancelled")
                );
                actionQuitterReunion(null);
            }
        });
    }

    private void mettreAJourListeAffichageParticipants(List<DonneesUtilisateurDTO> participants) {
        Platform.runLater(() -> {
            listeObservableAffichageParticipants.clear();
            if (participants != null) {
                participants.forEach(p -> {
                    // Pourrait inclure le rôle dans la réunion si cette info est disponible dans DonneesUtilisateurDTO
                    // ou un DTO spécifique pour les participants à une réunion.
                    String affichageParticipant = p.getNomComplet();
                    // Exemple: if (p.getRoleDansReunion() != null) affichageParticipant += " (" + p.getRoleDansReunion() + ")";
                    listeObservableAffichageParticipants.add(affichageParticipant);
                });
            }
        });
    }
}