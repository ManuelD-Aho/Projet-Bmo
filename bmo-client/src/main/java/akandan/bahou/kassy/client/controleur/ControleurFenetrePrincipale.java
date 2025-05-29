package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
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

    @FXML private BorderPane conteneurPrincipalVue;
    @FXML private MenuBar barreMenuPrincipale;
    @FXML private Menu menuFichier;
    @FXML private MenuItem menuItemDeconnexion;
    @FXML private MenuItem menuItemQuitter;
    @FXML private Menu menuAide;
    @FXML private MenuItem menuItemAPropos;
    @FXML private Label etiquetteStatutUtilisateurConnecte;
    @FXML private Label etiquetteStatutServeur;

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

        // Adapter GestionnaireNavigation pour qu'il utilise conteneurPrincipalVue
        // Pour l'instant, on suppose que GestionnaireNavigation a été adapté.
        // Si ce n'est pas le cas, la première vue (connexion) doit être chargée ici
        // ou par AppPrincipale dans la scène principale.

        if (serviceSessionUtilisateur != null) {
            serviceSessionUtilisateur.estUtilisateurConnecteProperty().addListener((obs, etaitCo, estCoMaintenant) -> mettreAJourElementsUIConnexion());
        }
        if (serviceCommunicationServeur != null) {
            serviceCommunicationServeur.etatConnexionServeurProperty().addListener((obs, etaitCoServ, estCoServMaintenant) -> mettreAJourStatutServeur());
        }

        mettreAJourElementsUIConnexion();
        mettreAJourStatutServeur();

        // Charge la vue de connexion initiale dans le conteneur si GestionnaireNavigation est adapté
        // Sinon, cette logique est dans AppPrincipale qui change toute la scène.
        // Pour ce modèle, on suppose que le GestionnaireNavigation est adapté.
        if (this.gestionnaireNavigation != null && this.conteneurPrincipalVue != null) {
            // Le GestionnaireNavigation doit être modifié pour avoir une méthode comme:
            // this.gestionnaireNavigation.chargerVueDansConteneur(this.conteneurPrincipalVue, "FenetreConnexion.fxml", "app.title.login");
            // Pour l'instant, on suppose que la vue de connexion est chargée par AppPrincipale changeant la Scene,
            // et que cette FenetrePrincipale est chargée APRÈS connexion réussie, contenant déjà le TableauDeBord.
            // OU que AppPrincipale charge FenetrePrincipale.fxml, et FenetrePrincipale charge Connexion.fxml dans son conteneur.
            // Pour simplifier, on va supposer que la première vue est chargée par AppPrincipale ou le GestionnaireNavigation au démarrage.
            // Si aucune session n'est active, le gestionnaire de navigation devrait afficher la vue de connexion.
            if (!serviceSessionUtilisateur.estConnecte()) {
                gestionnaireNavigation.afficherVueConnexion(conteneurPrincipalVue); // Méthode à ajouter à GestionnaireNavigation
            } else {
                gestionnaireNavigation.afficherTableauDeBord(conteneurPrincipalVue); // Méthode à ajouter
            }
        }
    }

    @FXML
    private void initialize() {
        if (paquetRessourcesI18n != null) {
            menuFichier.setText(paquetRessourcesI18n.getString("main.menu.file"));
            menuItemDeconnexion.setText(paquetRessourcesI18n.getString("main.menu.file.logout"));
            menuItemQuitter.setText(paquetRessourcesI18n.getString("main.menu.file.quit"));
            menuAide.setText(paquetRessourcesI18n.getString("main.menu.help"));
            menuItemAPropos.setText(paquetRessourcesI18n.getString("main.menu.help.about"));
        }
        menuItemDeconnexion.setVisible(false); // Initialement non visible
        menuItemDeconnexion.setManaged(false);
    }

    @FXML
    private void actionDeconnexion(ActionEvent evenement) {
        serviceCommunicationServeur.envoyerRequeteDeconnexion();
        serviceSessionUtilisateur.viderSession();
        // Le listener sur estUtilisateurConnecteProperty devrait gérer le changement de vue
        // ou on le force ici si GestionnaireNavigation est adapté pour charger dans le conteneur.
        gestionnaireNavigation.afficherVueConnexion(conteneurPrincipalVue);
    }

    @FXML
    private void actionQuitterApplication(ActionEvent evenement) {
        if (serviceCommunicationServeur != null) {
            serviceCommunicationServeur.deconnecterDuServeur();
        }
        Platform.exit();
        System.exit(0); // Force l'arrêt si Platform.exit() ne suffit pas (ex: threads non-daemon)
    }

    @FXML
    private void actionAfficherAPropos(ActionEvent evenement) {
        AlertesUtilisateur.afficherInformation(
                paquetRessourcesI18n.getString("about.title"),
                paquetRessourcesI18n.getString("about.content") + "\nVersion: 1.0.0-BMO\nUtilisateur: " + (serviceSessionUtilisateur.estConnecte() ? serviceSessionUtilisateur.getUtilisateurConnecte().getNomComplet() : "N/A")
        );
    }

    private void mettreAJourElementsUIConnexion() {
        Platform.runLater(() -> {
            boolean estConnecte = serviceSessionUtilisateur.estConnecte();
            menuItemDeconnexion.setVisible(estConnecte);
            menuItemDeconnexion.setManaged(estConnecte);
            if (estConnecte && serviceSessionUtilisateur.getUtilisateurConnecte() != null) {
                etiquetteStatutUtilisateurConnecte.setText(paquetRessourcesI18n.getString("main.status.connected.as") + " " + serviceSessionUtilisateur.getUtilisateurConnecte().getNomComplet());
            } else {
                etiquetteStatutUtilisateurConnecte.setText(paquetRessourcesI18n.getString("main.status.notConnected"));
            }
        });
    }

    private void mettreAJourStatutServeur() {
        Platform.runLater(() -> {
            boolean connecteAuServeur = serviceCommunicationServeur.estActuellementConnecte();
            if (connecteAuServeur) {
                etiquetteStatutServeur.setText(paquetRessourcesI18n.getString("main.status.server.connected"));
                etiquetteStatutServeur.setStyle("-fx-text-fill: green;");
            } else {
                etiquetteStatutServeur.setText(paquetRessourcesI18n.getString("main.status.server.disconnected"));
                etiquetteStatutServeur.setStyle("-fx-text-fill: red;");
            }
        });
    }
}