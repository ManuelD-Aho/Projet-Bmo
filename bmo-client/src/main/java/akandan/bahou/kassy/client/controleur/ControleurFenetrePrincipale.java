package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurFenetrePrincipale implements ControleurAvecInitialisation {

    @FXML private BorderPane conteneurPrincipal;
    @FXML private MenuBar barreMenuPrincipale;
    @FXML private MenuItem menuItemFichierQuitter;
    @FXML private MenuItem menuItemAideAPropos;
    @FXML private Label etiquetteStatutApplication;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurFenetrePrincipale.class);

    public ControleurFenetrePrincipale() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        if (serviceSess != null) {
            serviceSess.estUtilisateurConnecteProperty().addListener((obs, ancien, nouveau) -> mettreAJourStatutUtilisateur(nouveau));
            mettreAJourStatutUtilisateur(serviceSess.estConnecte()); // Etat initial
        }
        if(serviceComm != null){
            serviceComm.etatConnexionServeurProperty().addListener((obs, etaitCo, estCoMaintenant) -> {
                if(!estCoMaintenant && etiquetteStatutApplication != null){
                    Platform.runLater(() -> etiquetteStatutApplication.setText(paquetRessourcesI18n.getString("status.server.disconnected")));
                } else if (estCoMaintenant && serviceSess != null){
                    mettreAJourStatutUtilisateur(serviceSess.estConnecte());
                }
            });
        }
    }

    @FXML
    private void initialize() {
        if (menuItemFichierQuitter != null && paquetRessourcesI18n != null) {
            menuItemFichierQuitter.setText(paquetRessourcesI18n.getString("menu.file.quit"));
        }
        if (menuItemAideAPropos != null && paquetRessourcesI18n != null) {
            menuItemAideAPropos.setText(paquetRessourcesI18n.getString("menu.help.about"));
        }
        // Si ce contrôleur est utilisé, la vue initiale (par exemple, la connexion)
        // serait chargée par AppPrincipale directement ou via GestionnaireNavigation
        // modifiant la scène du stage principal.
        // Si FenetrePrincipale.fxml EST la scène principale et contient un espace pour d'autres vues,
        // GestionnaireNavigation aurait besoin d'une méthode pour charger des FXML dans conteneurPrincipal.
    }

    @FXML
    private void actionQuitterApplication(ActionEvent evenement) {
        if (serviceCommunicationServeur != null) {
            serviceCommunicationServeur.deconnecterDuServeur();
        }
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void actionAfficherAPropos(ActionEvent evenement) {
        if (paquetRessourcesI18n != null) {
            AlertesUtilisateur.afficherInformation(
                    paquetRessourcesI18n.getString("about.title"),
                    paquetRessourcesI18n.getString("about.content")
            );
        } else {
            AlertesUtilisateur.afficherInformation("À Propos", "Application BMO.");
        }
    }

    private void mettreAJourStatutUtilisateur(boolean estConnecte) {
        if (etiquetteStatutApplication != null && paquetRessourcesI18n != null) {
            Platform.runLater(() -> {
                if (estConnecte && serviceSessionUtilisateur.getUtilisateurConnecte() != null) {
                    etiquetteStatutApplication.setText(paquetRessourcesI18n.getString("status.connected.as") + " " + serviceSessionUtilisateur.getUtilisateurConnecte().getNomComplet());
                } else {
                    if(serviceCommunicationServeur != null && serviceCommunicationServeur.estActuellementConnecte()){
                        etiquetteStatutApplication.setText(paquetRessourcesI18n.getString("status.disconnected.user"));
                    } else {
                        etiquetteStatutApplication.setText(paquetRessourcesI18n.getString("status.disconnected"));
                    }
                }
            });
        }
    }
}