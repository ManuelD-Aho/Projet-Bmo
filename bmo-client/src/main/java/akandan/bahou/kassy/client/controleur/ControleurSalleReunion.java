package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
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
// javafx.scene.web.WebView; // Si vous l'utilisez pour le partage d'écran/vidéo
import org.slf4j.Logger;

public class ControleurSalleReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreReunionSalle;
    @FXML private Label etiquetteDescriptionReunionSalle; // Optionnel, si existe dans FXML
    @FXML private ListView<String> listViewParticipants;
    @FXML private TextArea textAreaChatHistorique;
    @FXML private TextField champSaisieMessageChat;
    @FXML private Button boutonEnvoyerMessageChat;
    @FXML private Button boutonQuitterReunion;
    @FXML private Button boutonLeverMain;
    // @FXML private Button boutonPartageEcran; // Conserver si FXML l'a, mais logique non implémentée
    // @FXML private WebView webViewContenuPrincipal; // Idem

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private DetailsReunionDTO detailsReunionActuelle;
    private final ObservableList<String> listeObservableNomsParticipants = FXCollections.observableArrayList();
    private final ObservableList<DonneesUtilisateurDTO> listeCompleteParticipantsDTO = FXCollections.observableArrayList();

    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurSalleReunion.class);
    private final DateTimeFormatter formateurHeureChat = DateTimeFormatter.ofPattern("HH:mm:ss");
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
            this.detailsReunionActuelle = (DetailsReunionDTO) donnees[0];
        } else {
            journal.error("Impossible d'entrer dans la salle de réunion sans détails de réunion valides.");
            AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.meeting.room.entry.title"),
                    paquetRessourcesI18n.getString("error.meeting.room.entry.content.nodetails")
            );
            Platform.runLater(gestionnaireNavigation::afficherTableauDeBord);
            return;
        }

        configurerBindingsEtListeners();
        mettreAJourInformationsReunion(this.detailsReunionActuelle);
        chargerDonneesInitialesSalle();
    }

    private void configurerBindingsEtListeners() {
        etiquetteTitreReunionSalle.setText(detailsReunionActuelle.getTitre());
        if (etiquetteDescriptionReunionSalle != null) {
            etiquetteDescriptionReunionSalle.setText(detailsReunionActuelle.getDescription());
        }

        serviceCommunicationServeur.dernierMessageChatRecuProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null && detailsReunionActuelle != null && nouv.getIdReunion() == detailsReunionActuelle.getIdReunion()) {
                afficherNouveauMessageChat(nouv);
            }
        });

        serviceCommunicationServeur.historiqueMessagesReunionProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null) {
                Platform.runLater(() -> {
                    textAreaChatHistorique.clear();
                    nouv.forEach(this::afficherNouveauMessageChat);
                });
            }
        });

        serviceCommunicationServeur.notificationUtilisateurRejointProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null && detailsReunionActuelle != null /*&& nouv.getIdReunion() == detailsReunionActuelle.getIdReunion() - si le DTO de notif contient idReunion*/) {
                ajouterParticipantVisuellement(nouv);
                afficherMessageSysteme(String.format(paquetRessourcesI18n.getString("meeting.user.joined"), nouv.getNomComplet()));
            }
        });

        serviceCommunicationServeur.notificationUtilisateurQuitteProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null && detailsReunionActuelle != null /*&& nouv.getIdReunion() == detailsReunionActuelle.getIdReunion()*/) {
                retirerParticipantVisuellement(nouv);
                afficherMessageSysteme(String.format(paquetRessourcesI18n.getString("meeting.user.left"), nouv.getNomComplet()));
            }
        });

        serviceCommunicationServeur.notificationReunionClotureeIdProperty().addListener((obs, ancienId, nouveauId) -> {
            if (nouveauId != null && detailsReunionActuelle != null && nouveauId.longValue() == detailsReunionActuelle.getIdReunion()) {
                Platform.runLater(() -> {
                    AlertesUtilisateur.afficherInformation(
                            paquetRessourcesI18n.getString("meeting.status.changed.title"),
                            String.format(paquetRessourcesI18n.getString("meeting.status.now.closed"), detailsReunionActuelle.getTitre())
                    );
                    gestionnaireNavigation.afficherTableauDeBord();
                });
            }
        });


        textAreaChatHistorique.setEditable(false);
        textAreaChatHistorique.setWrapText(true);
        listViewParticipants.setItems(listeObservableNomsParticipants);

        boutonLeverMain.setText(paquetRessourcesI18n.getString("button.raise.hand"));
        // if(boutonPartageEcran != null) { boutonPartageEcran.setDisable(true); boutonPartageEcran.setVisible(false); }
        // if(webViewContenuPrincipal != null) { webViewContenuPrincipal.setVisible(false); }
    }

    private void ajouterParticipantVisuellement(DonneesUtilisateurDTO utilisateur) {
        Platform.runLater(() -> {
            boolean existeDeja = listeCompleteParticipantsDTO.stream().anyMatch(p -> p.getIdUtilisateur() == utilisateur.getIdUtilisateur());
            if (!existeDeja) {
                listeCompleteParticipantsDTO.add(utilisateur);
            } else { // Mettre à jour si déjà présent (ex: reconnexion)
                listeCompleteParticipantsDTO.removeIf(p -> p.getIdUtilisateur() == utilisateur.getIdUtilisateur());
                listeCompleteParticipantsDTO.add(utilisateur);
            }
            mettreAJourListeNomsParticipants();
        });
    }

    private void retirerParticipantVisuellement(DonneesUtilisateurDTO utilisateur) {
        Platform.runLater(() -> {
            listeCompleteParticipantsDTO.removeIf(p -> p.getIdUtilisateur() == utilisateur.getIdUtilisateur());
            mettreAJourListeNomsParticipants();
        });
    }

    private void mettreAJourListeNomsParticipants() {
        listeObservableNomsParticipants.setAll(
                listeCompleteParticipantsDTO.stream()
                        .map(DonneesUtilisateurDTO::getNomComplet)
                        .collect(Collectors.toList())
        );
    }


    private void chargerDonneesInitialesSalle() {
        if (detailsReunionActuelle != null) {
            // La liste des participants est maintenant dans detailsReunionActuelle.getParticipantsDTO()
            if (detailsReunionActuelle.getParticipantsDTO() != null) {
                listeCompleteParticipantsDTO.setAll(detailsReunionActuelle.getParticipantsDTO());
                mettreAJourListeNomsParticipants();
            }
            serviceCommunicationServeur.envoyerRequeteObtenirHistoriqueMessages(detailsReunionActuelle.getIdReunion());
        }
    }

    private void mettreAJourInformationsReunion(DetailsReunionDTO nouveauxDetails) {
        this.detailsReunionActuelle = nouveauxDetails; // Mettre à jour la référence locale
        Platform.runLater(() -> {
            etiquetteTitreReunionSalle.setText(nouveauxDetails.getTitre());
            if (etiquetteDescriptionReunionSalle != null) {
                etiquetteDescriptionReunionSalle.setText(nouveauxDetails.getDescription());
            }
            if (nouveauxDetails.getParticipantsDTO() != null) {
                listeCompleteParticipantsDTO.setAll(nouveauxDetails.getParticipantsDTO());
                mettreAJourListeNomsParticipants();
            }

            if (nouveauxDetails.getStatutReunion() == StatutReunion.CLOTUREE || nouveauxDetails.getStatutReunion() == StatutReunion.ANNULEE) {
                AlertesUtilisateur.afficherInformation(
                        paquetRessourcesI18n.getString("meeting.status.changed.title"),
                        paquetRessourcesI18n.getString("meeting.status.closed_or_cancelled")
                );
                actionQuitterReunion(null);
            }
        });
    }


    @FXML
    private void initialize() {
        champSaisieMessageChat.setOnAction(this::actionEnvoyerMessageChat);
    }

    @FXML
    private void actionEnvoyerMessageChat(ActionEvent evenement) {
        String contenuMessage = champSaisieMessageChat.getText().trim();
        if (!contenuMessage.isEmpty() && detailsReunionActuelle != null) {
            DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
            if (utilisateurActuel != null) {
                MessageChatDTO messageDTO = new MessageChatDTO(
                        detailsReunionActuelle.getIdReunion(),
                        utilisateurActuel.getIdUtilisateur(),
                        utilisateurActuel.getNomComplet(), // Le nom est déjà connu côté client
                        contenuMessage,
                        LocalDateTime.now() // Horodatage client, le serveur devrait utiliser le sien
                );
                serviceCommunicationServeur.envoyerMessageChat(messageDTO);
                champSaisieMessageChat.clear();
            } else {
                journal.warn("Tentative d'envoi de message chat sans utilisateur connecté.");
                AlertesUtilisateur.afficherAvertissement(paquetRessourcesI18n.getString("error.generic.title"), paquetRessourcesI18n.getString("error.not.logged.in.for.action"));
            }
        }
    }

    @FXML
    private void actionQuitterReunion(ActionEvent evenement) {
        if (detailsReunionActuelle != null) {
            serviceCommunicationServeur.envoyerRequeteQuitterReunion(detailsReunionActuelle.getIdReunion());
        }
        gestionnaireNavigation.afficherTableauDeBord();
    }

    @FXML
    private void actionLeverMain(ActionEvent evenement) {
        if (detailsReunionActuelle != null && serviceSessionUtilisateur.estConnecte()) {
            mainLevee = !mainLevee; // Basculer l'état
            // La logique d'envoyer une requête au serveur pour "lever la main" / "baisser la main" irait ici.
            // serviceCommunicationServeur.envoyerRequeteLeverMain(detailsReunionActuelle.getIdReunion(), mainLevee);
            journal.info("Action 'Lever/Baisser la main' cliquée. État : {}", mainLevee ? "levée" : "baissée");
            boutonLeverMain.setText(mainLevee ? paquetRessourcesI18n.getString("button.lower.hand") : paquetRessourcesI18n.getString("button.raise.hand"));
            // Une notification visuelle pourrait être ajoutée à côté du nom du participant.
        }
    }

    private void afficherNouveauMessageChat(MessageChatDTO message) {
        Platform.runLater(() -> {
            String horodatageFormatte = message.getHorodatage() != null ? message.getHorodatage().format(formateurHeureChat) : "??:??:??";
            String nomAuteur = message.getNomExpediteur() != null ? message.getNomExpediteur() : "Inconnu";
            String messageFormatte = String.format("[%s] %s: %s\n",
                    horodatageFormatte,
                    nomAuteur,
                    message.getContenu()
            );
            textAreaChatHistorique.appendText(messageFormatte);
            textAreaChatHistorique.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void afficherMessageSysteme(String message) {
        Platform.runLater(() -> {
            String messageFormatte = String.format("*** %s ***\n", message);
            textAreaChatHistorique.appendText(messageFormatte);
            textAreaChatHistorique.setScrollTop(Double.MAX_VALUE);
        });
    }
}