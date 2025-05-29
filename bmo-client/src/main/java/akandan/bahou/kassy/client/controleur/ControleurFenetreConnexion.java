package akandan.bahou.kassy.client.controleur;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox; // Supposons que les sections sont dans des VBox
import javafx.event.ActionEvent;
import javafx.application.Platform;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurFenetreConnexion implements ControleurAvecInitialisation {

    @FXML private TextField champIdentifiantConnexion;
    @FXML private PasswordField champMotDePasseConnexion;
    @FXML private Button boutonSeConnecter;
    @FXML private Hyperlink lienCreerCompte;
    @FXML private Label etiquetteMessageErreurConnexion;

    @FXML private VBox panneauConnexion;
    @FXML private VBox panneauInscription;

    @FXML private TextField champNomCompletInscription;
    @FXML private TextField champIdentifiantInscription;
    @FXML private PasswordField champMotDePasseInscription;
    @FXML private PasswordField champConfirmationMotDePasseInscription;
    @FXML private Button boutonValiderInscription;
    @FXML private Button boutonAnnulerInscription;
    @FXML private Label etiquetteMessageErreurInscription;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurFenetreConnexion.class);

    public ControleurFenetreConnexion() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        serviceComm.dernierMessageErreurServeurProperty().addListener((obs, ancienMessage, nouveauMessage) -> {
            if (nouveauMessage != null && !nouveauMessage.isEmpty()) {
                Platform.runLater(() -> {
                    if (panneauConnexion.isVisible()) {
                        afficherErreurConnexion(nouveauMessage);
                    } else if (panneauInscription.isVisible()) {
                        afficherErreurInscription(nouveauMessage);
                    }
                });
            }
        });

        serviceSess.estUtilisateurConnecteProperty().addListener((obs, etaitConnecte, estConnecteMaintenant) -> {
            if (estConnecteMaintenant) {
                Platform.runLater(() -> gestionnaireNavigation.afficherTableauDeBord());
            }
        });

        serviceComm.etatConnexionServeurProperty().addListener((obs, etaitCoServeur, estCoServeurMaintenant) -> {
            Platform.runLater(() -> {
                boolean desactiverControles = !estCoServeurMaintenant;
                boutonSeConnecter.setDisable(desactiverControles);
                boutonValiderInscription.setDisable(desactiverControles);
                if(desactiverControles && etiquetteMessageErreurConnexion.getText().isEmpty() && panneauConnexion.isVisible()){
                    etiquetteMessageErreurConnexion.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
                } else if (!desactiverControles && panneauConnexion.isVisible()){
                    etiquetteMessageErreurConnexion.setText("");
                }
                if(desactiverControles && etiquetteMessageErreurInscription.getText().isEmpty() && panneauInscription.isVisible()){
                    etiquetteMessageErreurInscription.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
                } else if (!desactiverControles && panneauInscription.isVisible()){
                    etiquetteMessageErreurInscription.setText("");
                }
            });
        });
    }

    @FXML
    private void initialize() {
        etiquetteMessageErreurConnexion.setText("");
        etiquetteMessageErreurInscription.setText("");
        basculerVersPanneauConnexion();

        // Vérification initiale de la connexion serveur
        Platform.runLater(() -> {
            boolean desactiverControles = !serviceCommunicationServeur.estActuellementConnecte();
            boutonSeConnecter.setDisable(desactiverControles);
            boutonValiderInscription.setDisable(desactiverControles);
            if(desactiverControles){
                etiquetteMessageErreurConnexion.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
            }
        });
    }

    @FXML
    private void actionSeConnecter(ActionEvent evenement) {
        String identifiant = champIdentifiantConnexion.getText();
        String motDePasse = champMotDePasseConnexion.getText();

        if (identifiant == null || identifiant.trim().isEmpty()) {
            afficherErreurConnexion(paquetRessourcesI18n.getString("validation.identifiant.requis"));
            return;
        }
        if (motDePasse == null || motDePasse.isEmpty()) {
            afficherErreurConnexion(paquetRessourcesI18n.getString("validation.motdepasse.requis"));
            return;
        }

        etiquetteMessageErreurConnexion.setText(""); // Clear previous error
        activerDesactiverControlesConnexion(true);
        serviceCommunicationServeur.envoyerRequeteConnexion(identifiant, motDePasse);
        journal.info("Tentative de connexion pour l'utilisateur : {}", identifiant);
    }

    @FXML
    private void actionAfficherInscription(ActionEvent evenement) {
        basculerVersPanneauInscription();
    }

    @FXML
    private void actionAnnulerInscription(ActionEvent evenement) {
        basculerVersPanneauConnexion();
    }

    private void basculerVersPanneauInscription() {
        panneauConnexion.setVisible(false);
        panneauConnexion.setManaged(false);
        panneauInscription.setVisible(true);
        panneauInscription.setManaged(true);
        etiquetteMessageErreurConnexion.setText("");
        etiquetteMessageErreurInscription.setText("");
        champNomCompletInscription.clear();
        champIdentifiantInscription.clear();
        champMotDePasseInscription.clear();
        champConfirmationMotDePasseInscription.clear();
    }

    private void basculerVersPanneauConnexion() {
        panneauInscription.setVisible(false);
        panneauInscription.setManaged(false);
        panneauConnexion.setVisible(true);
        panneauConnexion.setManaged(true);
        etiquetteMessageErreurConnexion.setText("");
        etiquetteMessageErreurInscription.setText("");
        champIdentifiantConnexion.clear();
        champMotDePasseConnexion.clear();
    }


    @FXML
    private void actionValiderInscription(ActionEvent evenement) {
        String nomComplet = champNomCompletInscription.getText();
        String identifiant = champIdentifiantInscription.getText();
        String motDePasse = champMotDePasseInscription.getText();
        String confirmationMotDePasse = champConfirmationMotDePasseInscription.getText();

        if (!ValidateurEntreeUtilisateur.estNomCompletValide(nomComplet)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.nomcomplet.requis"));
            return;
        }
        if (!ValidateurEntreeUtilisateur.estIdentifiantValide(identifiant)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.identifiant.format"));
            return;
        }
        if (!ValidateurEntreeUtilisateur.estMotDePasseValide(motDePasse)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.motdepasse.complexite"));
            return;
        }
        if (!motDePasse.equals(confirmationMotDePasse)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.motdepasse.confirmation"));
            return;
        }

        etiquetteMessageErreurInscription.setText(""); // Clear previous error
        activerDesactiverControlesInscription(true);
        serviceCommunicationServeur.envoyerRequeteInscription(identifiant, motDePasse, nomComplet);
        journal.info("Tentative d'inscription pour l'utilisateur : {}", identifiant);
    }

    private void afficherErreurConnexion(String message) {
        Platform.runLater(() -> {
            etiquetteMessageErreurConnexion.setText(message);
            activerDesactiverControlesConnexion(false);
        });
    }

    private void afficherErreurInscription(String message) {
        Platform.runLater(() -> {
            etiquetteMessageErreurInscription.setText(message);
            activerDesactiverControlesInscription(false);
        });
    }

    private void activerDesactiverControlesConnexion(boolean desactiver) {
        champIdentifiantConnexion.setDisable(desactiver);
        champMotDePasseConnexion.setDisable(desactiver);
        boutonSeConnecter.setDisable(desactiver);
        lienCreerCompte.setDisable(desactiver);
    }

    private void activerDesactiverControlesInscription(boolean desactiver) {
        champNomCompletInscription.setDisable(desactiver);
        champIdentifiantInscription.setDisable(desactiver);
        champMotDePasseInscription.setDisable(desactiver);
        champConfirmationMotDePasseInscription.setDisable(desactiver);
        boutonValiderInscription.setDisable(desactiver);
        boutonAnnulerInscription.setDisable(desactiver);
    }
}