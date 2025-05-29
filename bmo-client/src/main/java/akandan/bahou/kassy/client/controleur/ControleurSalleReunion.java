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
import javafx.scene.web.WebView; // Si utilisé

import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.List; // Pour la liste des participants

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO; // Pour les participants
import akandan.bahou.kassy.commun.modele.StatutReunion; // Pour vérifier le statut
// import akandan.bahou.kassy.commun.modele.RoleDansReunion; // Si DTO participant a ce rôle spécifique
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurSalleReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreReunionSalle;
    @FXML private Label etiquetteDescriptionReunionSalle;
    @FXML private ListView<String> listViewParticipants;
    @FXML private TextArea textAreaChatHistorique;
    @FXML private TextField champSaisieMessageChat;
    @FXML private Button boutonEnvoyerMessageChat;
    @FXML private Button boutonQuitterReunion;
    @FXML private Button boutonLeverMain;
    @FXML private Button boutonPartageEcran; // Non implémenté dans cette version
    @FXML private WebView webViewContenuPrincipal; // Non implémenté dans cette version

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private DetailsReunionDTO detailsReunionActuelle;
    private final ObservableList<String> listeObservableParticipants = FXCollections.observableArrayList();
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurSalleReunion.class);
    private final DateTimeFormatter formateurHeureChat = DateTimeFormatter.ofPattern("HH:mm:ss");

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

        etiquetteTitreReunionSalle.setText(detailsReunionActuelle.getTitre());
        if(etiquetteDescriptionReunionSalle != null) { // Peut être optionnel dans le FXML
            etiquetteDescriptionReunionSalle.setText(detailsReunionActuelle.getDescription());
        }

        serviceComm.dernierMessageChatRecuProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null && detailsReunionActuelle != null && nouv.getIdReunion() == detailsReunionActuelle.getIdReunion()) {
                afficherNouveauMessageChat(nouv);
            }
        });

        // Supposons que ServiceCommunicationServeur ait une propriété pour la liste des participants
        // Pour l'instant, nous allons simuler une mise à jour via une méthode directe si la propriété n'existe pas.
        // serviceComm.listeParticipantsMiseAJourProperty().addListener((obs, anc, nouv) -> mettreAJourListeParticipants(nouv));

        serviceComm.detailsReunionMiseAJourProperty().addListener((obs, anc, nouv) -> {
            if (nouv != null && detailsReunionActuelle != null && nouv.getIdReunion() == detailsReunionActuelle.getIdReunion()) {
                mettreAJourDetailsReunion(nouv);
            }
        });

        textAreaChatHistorique.setEditable(false);
        textAreaChatHistorique.setWrapText(true);
        listViewParticipants.setItems(listeObservableParticipants);

        // Simuler la réception de l'historique et des participants après avoir rejoint
        // Dans une vraie appli, ces requêtes seraient faites ici, et les réponses mettraient à jour l'UI
        if (detailsReunionActuelle != null) {
            serviceCommunicationServeur.envoyerRequeteObtenirHistoriqueMessages(detailsReunionActuelle.getIdReunion());
            // serviceCommunicationServeur.envoyerRequeteObtenirParticipantsReunion(detailsReunionActuelle.getIdReunion());
            // Pour l'instant, on ajoute l'utilisateur actuel à la liste des participants
            DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
            if(utilisateurActuel != null) {
                // La vraie liste viendrait du serveur. Ceci est un placeholder.
                // mettreAJourListeParticipants(List.of(utilisateurActuel));
            }
        }

        boutonLeverMain.setText(paquetRessourcesI18n.getString("button.raise.hand"));
        if(boutonPartageEcran != null) {
            boutonPartageEcran.setDisable(true); // Fonctionnalité non implémentée
            boutonPartageEcran.setVisible(false);
        }
        if(webViewContenuPrincipal != null) {
            webViewContenuPrincipal.setVisible(false); // Non utilisé pour l'instant
        }

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
                MessageChatDTO messageDTO = new MessageChatDTO();
                messageDTO.setIdReunion(detailsReunionActuelle.getIdReunion());
                messageDTO.setIdExpediteur(utilisateurActuel.getIdUtilisateur());
                // Le nomExpediteur et l'horodatage pourraient être définis par le serveur ou ici.
                // Par cohérence, le serveur devrait probablement les gérer pour éviter la manipulation client.
                // Pour l'affichage immédiat, on peut les pré-remplir.
                messageDTO.setNomExpediteur(utilisateurActuel.getNomComplet());
                messageDTO.setContenu(contenuMessage);
                messageDTO.setHorodatage(LocalDateTime.now()); // Le serveur devrait utiliser son propre horodatage

                serviceCommunicationServeur.envoyerMessageChat(messageDTO);
                // Afficher son propre message immédiatement (optionnel, si le serveur ne renvoie pas les messages de l'émetteur)
                // afficherNouveauMessageChat(messageDTO);
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
            journal.info("Utilisateur {} a quitté la réunion : {}", serviceSessionUtilisateur.getUtilisateurConnecte() != null ? serviceSessionUtilisateur.getUtilisateurConnecte().getNomComplet() : "Inconnu", detailsReunionActuelle.getTitre());
        }
        gestionnaireNavigation.afficherTableauDeBord();
    }

    @FXML
    private void actionLeverMain(ActionEvent evenement) {
        if (detailsReunionActuelle != null && serviceSessionUtilisateur.estConnecte()) {
            // La logique de "lever la main" (état on/off) doit être gérée.
            // Pour l'instant, on envoie juste une demande.
            // serviceCommunicationServeur.envoyerRequetePriseParole(detailsReunionActuelle.getIdReunion(), serviceSessionUtilisateur.getUtilisateurConnecte().getIdUtilisateur(), true);
            journal.info("Action 'Lever la main' cliquée.");
            AlertesUtilisateur.afficherInformation("Fonctionnalité", "Lever la main : demande envoyée (simulation).");
            // Changer le texte du bouton ou son apparence pour indiquer l'état
            if (boutonLeverMain.getText().equals(paquetRessourcesI18n.getString("button.raise.hand"))) {
                boutonLeverMain.setText(paquetRessourcesI18n.getString("button.lower.hand"));
            } else {
                boutonLeverMain.setText(paquetRessourcesI18n.getString("button.raise.hand"));
            }
        }
    }

    private void afficherNouveauMessageChat(MessageChatDTO message) {
        Platform.runLater(() -> {
            String messageFormatte = String.format("[%s] %s: %s\n",
                    message.getHorodatage() != null ? message.getHorodatage().format(formateurHeureChat) : "??:??:??",
                    message.getNomExpediteur() != null ? message.getNomExpediteur() : "Inconnu",
                    message.getContenu()
            );
            textAreaChatHistorique.appendText(messageFormatte);
            textAreaChatHistorique.setScrollTop(Double.MAX_VALUE); // Auto-scroll
        });
    }

    // Cette méthode serait appelée par un listener sur une propriété de ServiceCommunicationServeur
    // qui contiendrait la liste des participants mise à jour par le serveur.
    public void mettreAJourListeParticipants(List<DonneesUtilisateurDTO> participants) {
        Platform.runLater(() -> {
            listeObservableParticipants.clear();
            if (participants != null) {
                participants.forEach(p -> {
                    String roleDansReunionStr = ""; // Le DTO devrait avoir cette info si elle est spécifique à la réunion
                    // if (p.getRoleDansReunion() != null) { // Supposant que DonneesUtilisateurDTO a getRoleDansReunion()
                    //    roleDansReunionStr = " (" + paquetRessourcesI18n.getString("role.in.meeting." + p.getRoleDansReunion().name().toLowerCase()) + ")";
                    // }
                    listeObservableParticipants.add(p.getNomComplet() + roleDansReunionStr);
                });
            }
        });
    }

    private void mettreAJourDetailsReunion(DetailsReunionDTO nouveauxDetails) {
        this.detailsReunionActuelle = nouveauxDetails;
        Platform.runLater(() -> {
            etiquetteTitreReunionSalle.setText(detailsReunionActuelle.getTitre());
            if (etiquetteDescriptionReunionSalle != null) {
                etiquetteDescriptionReunionSalle.setText(detailsReunionActuelle.getDescription());
            }

            if (detailsReunionActuelle.getStatutReunion() == StatutReunion.CLOTUREE || detailsReunionActuelle.getStatutReunion() == StatutReunion.ANNULEE ) {
                AlertesUtilisateur.afficherInformation(
                        paquetRessourcesI18n.getString("meeting.status.changed.title"),
                        paquetRessourcesI18n.getString("meeting.status.closed_or_cancelled")
                );
                actionQuitterReunion(null); // Forcer la sortie
            }
        });
    }
}