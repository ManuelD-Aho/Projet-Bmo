package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur; // Pour la validation client-side
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

public class ControleurFenetreConnexion implements ControleurAvecInitialisation {

    @FXML private VBox panneauConnexion;
    @FXML private TextField champIdentifiantConnexion;
    @FXML private PasswordField champMotDePasseConnexion;
    @FXML private Button boutonSeConnecter;
    @FXML private Hyperlink lienCreerCompte;
    @FXML private Label etiquetteMessageErreurConnexion;

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

        // Écouteur pour les messages d'erreur d'authentification/inscription
        serviceComm.dernierMessageErreurAuthProperty().addListener((obs, ancienMessage, nouveauMessage) -> {
            if (nouveauMessage != null && !nouveauMessage.isEmpty()) {
                Platform.runLater(() -> {
                    if (panneauConnexion.isVisible()) {
                        afficherErreurConnexion(nouveauMessage);
                        activerDesactiverControlesConnexion(false);
                    } else if (panneauInscription.isVisible()) {
                        afficherErreurInscription(nouveauMessage);
                        activerDesactiverControlesInscription(false);
                    }
                });
            }
        });

        // Écouteur pour la réussite de la connexion
        serviceSess.estUtilisateurConnecteProperty().addListener((obs, etaitConnecte, estConnecteMaintenant) -> {
            if (estConnecteMaintenant) {
                Platform.runLater(() -> {
                    viderChamps();
                    gestionnaireNavigation.afficherTableauDeBord();
                });
            }
        });

        // Écouteur pour l'état de la connexion au serveur
        serviceComm.etatConnexionServeurProperty().addListener((obs, etaitCoServeur, estCoServeurMaintenant) -> {
            Platform.runLater(() -> {
                boolean desactiverSaisie = !estCoServeurMaintenant;
                activerDesactiverControlesConnexion(desactiverSaisie);
                activerDesactiverControlesInscription(desactiverSaisie);

                if(desactiverSaisie && panneauConnexion.isVisible() && (etiquetteMessageErreurConnexion.getText() == null || etiquetteMessageErreurConnexion.getText().isEmpty())){
                    etiquetteMessageErreurConnexion.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
                } else if (!desactiverSaisie && panneauConnexion.isVisible()){
                    if (etiquetteMessageErreurConnexion.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
                        etiquetteMessageErreurConnexion.setText("");
                    }
                }
                if(desactiverSaisie && panneauInscription.isVisible() && (etiquetteMessageErreurInscription.getText() == null || etiquetteMessageErreurInscription.getText().isEmpty())){
                    etiquetteMessageErreurInscription.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
                } else if (!desactiverSaisie && panneauInscription.isVisible()){
                    if (etiquetteMessageErreurInscription.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
                        etiquetteMessageErreurInscription.setText("");
                    }
                }
            });
        });
        // État initial des contrôles basé sur la connexion serveur
        Platform.runLater(() -> {
            boolean desactiverSaisieInitiale = !serviceCommunicationServeur.estActuellementConnecte();
            activerDesactiverControlesConnexion(desactiverSaisieInitiale);
            if(desactiverSaisieInitiale){
                etiquetteMessageErreurConnexion.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
            }
        });
    }

    @FXML
    private void initialize() {
        basculerVersPanneauConnexion(); // Assurer l'état initial correct
    }

    @FXML
    private void actionSeConnecter(ActionEvent evenement) {
        String identifiant = champIdentifiantConnexion.getText();
        String motDePasse = champMotDePasseConnexion.getText();

        try {
            ValidateurEntreeUtilisateur.validerNonNulOuVide(identifiant, paquetRessourcesI18n.getString("login.email"));
            ValidateurEntreeUtilisateur.validerNonNulOuVide(motDePasse, paquetRessourcesI18n.getString("login.password"));
        } catch (ExceptionValidation e) {
            afficherErreurConnexion(e.getMessage());
            return;
        }

        etiquetteMessageErreurConnexion.setText("");
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

    @FXML
    private void actionValiderInscription(ActionEvent evenement) {
        String nomComplet = champNomCompletInscription.getText();
        String identifiant = champIdentifiantInscription.getText();
        String motDePasse = champMotDePasseInscription.getText();
        String confirmationMotDePasse = champConfirmationMotDePasseInscription.getText();

        try {
            ValidateurEntreeUtilisateur.validerNonNulOuVide(nomComplet, paquetRessourcesI18n.getString("signup.fullname"));
            ValidateurEntreeUtilisateur.validerIdentifiantConnexion(identifiant, paquetRessourcesI18n.getString("signup.identifier"));
            ValidateurEntreeUtilisateur.validerComplexiteMotDePasse(motDePasse, paquetRessourcesI18n.getString("signup.password"));
            if (!motDePasse.equals(confirmationMotDePasse)) {
                throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.motdepasse.confirmation"));
            }
        } catch (ExceptionValidation e) {
            afficherErreurInscription(e.getMessage());
            return;
        }

        etiquetteMessageErreurInscription.setText("");
        activerDesactiverControlesInscription(true);
        serviceCommunicationServeur.envoyerRequeteInscription(identifiant, motDePasse, nomComplet);
        journal.info("Tentative d'inscription pour l'utilisateur : {}", identifiant);
    }

    private void viderChamps() {
        champIdentifiantConnexion.clear();
        champMotDePasseConnexion.clear();
        etiquetteMessageErreurConnexion.setText("");
        champNomCompletInscription.clear();
        champIdentifiantInscription.clear();
        champMotDePasseInscription.clear();
        champConfirmationMotDePasseInscription.clear();
        etiquetteMessageErreurInscription.setText("");
    }

    private void basculerVersPanneauInscription() {
        viderChamps();
        panneauConnexion.setVisible(false);
        panneauConnexion.setManaged(false);
        panneauInscription.setVisible(true);
        panneauInscription.setManaged(true);
        boolean desactiverSaisie = !serviceCommunicationServeur.estActuellementConnecte();
        activerDesactiverControlesInscription(desactiverSaisie);
        if(desactiverSaisie){
            etiquetteMessageErreurInscription.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
        }
    }

    private void basculerVersPanneauConnexion() {
        viderChamps();
        panneauInscription.setVisible(false);
        panneauInscription.setManaged(false);
        panneauConnexion.setVisible(true);
        panneauConnexion.setManaged(true);
        boolean desactiverSaisie = !serviceCommunicationServeur.estActuellementConnecte();
        activerDesactiverControlesConnexion(desactiverSaisie);
        if(desactiverSaisie){
            etiquetteMessageErreurConnexion.setText(paquetRessourcesI18n.getString("error.server.unavailable"));
        }
    }

    private void afficherErreurConnexion(String message) {
        etiquetteMessageErreurConnexion.setText(message);
        etiquetteMessageErreurConnexion.setVisible(true);
    }

    private void afficherErreurInscription(String message) {
        etiquetteMessageErreurInscription.setText(message);
        etiquetteMessageErreurInscription.setVisible(true);
    }

    private void activerDesactiverControlesConnexion(boolean desactiver) {
        champIdentifiantConnexion.setDisable(desactiver);
        champMotDePasseConnexion.setDisable(desactiver);
        boutonSeConnecter.setDisable(desactiver);
        lienCreerCompte.setDisable(desactiver);
        if (!desactiver && etiquetteMessageErreurConnexion.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
            etiquetteMessageErreurConnexion.setText("");
        }
    }

    private void activerDesactiverControlesInscription(boolean desactiver) {
        champNomCompletInscription.setDisable(desactiver);
        champIdentifiantInscription.setDisable(desactiver);
        champMotDePasseInscription.setDisable(desactiver);
        champConfirmationMotDePasseInscription.setDisable(desactiver);
        boutonValiderInscription.setDisable(desactiver);
        boutonAnnulerInscription.setDisable(desactiver);
        if (!desactiver && etiquetteMessageErreurInscription.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
            etiquetteMessageErreurInscription.setText("");
        }
    }
}