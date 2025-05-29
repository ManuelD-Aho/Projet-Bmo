package akandan.bahou.kassy.client.controleur;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
// import akandan.bahou.kassy.client.util.AlertesUtilisateur; // Non utilisé directement ici, les erreurs sont dans les labels
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
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

        serviceComm.dernierMessageErreurAuthProperty().addListener((obs, ancienMessage, nouveauMessage) -> {
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
                Platform.runLater(() -> {
                    // Effacer les champs après une connexion réussie
                    if(champIdentifiantConnexion != null) champIdentifiantConnexion.clear();
                    if(champMotDePasseConnexion != null) champMotDePasseConnexion.clear();
                    if(etiquetteMessageErreurConnexion != null) etiquetteMessageErreurConnexion.setText("");
                    gestionnaireNavigation.afficherTableauDeBord();
                });
            }
        });

        serviceComm.etatConnexionServeurProperty().addListener((obs, etaitCoServeur, estCoServeurMaintenant) ->
                Platform.runLater(() -> mettreAJourEtatControlesSelonConnexionServeur(!estCoServeurMaintenant))
        );
    }

    @FXML
    private void initialize() {
        if (etiquetteMessageErreurConnexion != null) etiquetteMessageErreurConnexion.setText("");
        if (etiquetteMessageErreurInscription != null) etiquetteMessageErreurInscription.setText("");

        basculerVersPanneauConnexion();

        // L'état initial des contrôles est géré par le listener sur etatConnexionServeurProperty
        // mais on peut forcer une mise à jour initiale si le service est déjà initialisé
        if (serviceCommunicationServeur != null) {
            mettreAJourEtatControlesSelonConnexionServeur(!serviceCommunicationServeur.estActuellementConnecte());
        }
    }

    private void basculerVersPanneauConnexion() {
        if (panneauConnexion != null) {
            panneauConnexion.setVisible(true);
            panneauConnexion.setManaged(true);
        }
        if (panneauInscription != null) {
            panneauInscription.setVisible(false);
            panneauInscription.setManaged(false);
        }
        if (champIdentifiantConnexion != null) champIdentifiantConnexion.clear();
        if (champMotDePasseConnexion != null) champMotDePasseConnexion.clear();
        if (etiquetteMessageErreurConnexion != null) etiquetteMessageErreurConnexion.setText("");
        if (etiquetteMessageErreurInscription != null) etiquetteMessageErreurInscription.setText(""); // Effacer aussi l'erreur de l'autre panneau
    }

    private void basculerVersPanneauInscription() {
        if (panneauInscription != null) {
            panneauInscription.setVisible(true);
            panneauInscription.setManaged(true);
        }
        if (panneauConnexion != null) {
            panneauConnexion.setVisible(false);
            panneauConnexion.setManaged(false);
        }
        if (champNomCompletInscription != null) champNomCompletInscription.clear();
        if (champIdentifiantInscription != null) champIdentifiantInscription.clear();
        if (champMotDePasseInscription != null) champMotDePasseInscription.clear();
        if (champConfirmationMotDePasseInscription != null) champConfirmationMotDePasseInscription.clear();
        if (etiquetteMessageErreurInscription != null) etiquetteMessageErreurInscription.setText("");
        if (etiquetteMessageErreurConnexion != null) etiquetteMessageErreurConnexion.setText(""); // Effacer aussi l'erreur de l'autre panneau
    }

    @FXML
    private void actionSeConnecter(ActionEvent evenement) {
        String identifiant = champIdentifiantConnexion.getText();
        String motDePasse = champMotDePasseConnexion.getText();

        if (identifiant == null || identifiant.trim().isEmpty()) {
            afficherErreurConnexion(paquetRessourcesI18n.getString("validation.identifier.required"));
            return;
        }
        if (motDePasse == null || motDePasse.isEmpty()) {
            afficherErreurConnexion(paquetRessourcesI18n.getString("validation.password.required"));
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

        if (!ValidateurEntreeUtilisateur.estNomCompletValide(nomComplet)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.fullname.required"));
            return;
        }
        if (!ValidateurEntreeUtilisateur.estIdentifiantValide(identifiant)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.identifier.format"));
            return;
        }
        if (!ValidateurEntreeUtilisateur.estMotDePasseValide(motDePasse)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.password.complexity"));
            return;
        }
        if (!motDePasse.equals(confirmationMotDePasse)) {
            afficherErreurInscription(paquetRessourcesI18n.getString("validation.password.confirmation"));
            return;
        }

        etiquetteMessageErreurInscription.setText("");
        activerDesactiverControlesInscription(true);
        serviceCommunicationServeur.envoyerRequeteInscription(identifiant, motDePasse, nomComplet);
        journal.info("Tentative d'inscription pour l'utilisateur : {}", identifiant);
    }

    private void afficherErreurConnexion(String message) {
        if (etiquetteMessageErreurConnexion != null) {
            etiquetteMessageErreurConnexion.setText(message);
        }
        activerDesactiverControlesConnexion(false);
    }

    private void afficherErreurInscription(String message) {
        if (etiquetteMessageErreurInscription != null) {
            etiquetteMessageErreurInscription.setText(message);
        }
        activerDesactiverControlesInscription(false);
    }

    private void activerDesactiverControlesConnexion(boolean desactiver) {
        if (champIdentifiantConnexion != null) champIdentifiantConnexion.setDisable(desactiver);
        if (champMotDePasseConnexion != null) champMotDePasseConnexion.setDisable(desactiver);
        if (boutonSeConnecter != null) boutonSeConnecter.setDisable(desactiver);
        if (lienCreerCompte != null) lienCreerCompte.setDisable(desactiver);
    }

    private void activerDesactiverControlesInscription(boolean desactiver) {
        if (champNomCompletInscription != null) champNomCompletInscription.setDisable(desactiver);
        if (champIdentifiantInscription != null) champIdentifiantInscription.setDisable(desactiver);
        if (champMotDePasseInscription != null) champMotDePasseInscription.setDisable(desactiver);
        if (champConfirmationMotDePasseInscription != null) champConfirmationMotDePasseInscription.setDisable(desactiver);
        if (boutonValiderInscription != null) boutonValiderInscription.setDisable(desactiver);
        if (boutonAnnulerInscription != null) boutonAnnulerInscription.setDisable(desactiver);
    }

    private void mettreAJourEtatControlesSelonConnexionServeur(boolean serveurIndisponible) {
        activerDesactiverControlesConnexion(serveurIndisponible);
        activerDesactiverControlesInscription(serveurIndisponible);

        String messageErreurServeur = serveurIndisponible ? paquetRessourcesI18n.getString("error.server.unavailable") : "";

        if (panneauConnexion != null && panneauConnexion.isVisible() && etiquetteMessageErreurConnexion != null) {
            if (serveurIndisponible || etiquetteMessageErreurConnexion.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
                etiquetteMessageErreurConnexion.setText(messageErreurServeur);
            }
        }
        if (panneauInscription != null && panneauInscription.isVisible() && etiquetteMessageErreurInscription != null) {
            if (serveurIndisponible || etiquetteMessageErreurInscription.getText().equals(paquetRessourcesI18n.getString("error.server.unavailable"))) {
                etiquetteMessageErreurInscription.setText(messageErreurServeur);
            }
        }
    }
}