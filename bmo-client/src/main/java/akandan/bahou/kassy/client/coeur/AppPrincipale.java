package akandan.bahou.kassy.client.coeur;

import akandan.bahou.kassy.client.controleur.ControleurFenetrePrincipale;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class AppPrincipale extends Application {

    private Stage stagePrincipal;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private GestionnaireNavigation gestionnaireNavigation;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(AppPrincipale.class);
    private ResourceBundle paquetRessourcesI18n;
    private ControleurFenetrePrincipale controleurFenetrePrincipale;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        try {
            try {
                this.paquetRessourcesI18n = ResourceBundle.getBundle("akandan.bahou.kassy.client.vues.i18n.messages", new Locale("fr", "FR"));
            } catch (MissingResourceException mre) {
                journal.error("Fichier de ressources i18n introuvable.", mre);
                Platform.runLater(() -> AlertesUtilisateur.afficherErreur("Critical Error", "Localization files not found. Application will exit."));
                Platform.exit(); // Force l'arrêt si i18n est crucial
                return;
            }

            this.serviceSessionUtilisateur = new ServiceSessionUtilisateur();
            this.serviceCommunicationServeur = new ServiceCommunicationServeur(this.serviceSessionUtilisateur, this.paquetRessourcesI18n);

            boolean connecte = this.serviceCommunicationServeur.connecterAuServeur("localhost", 5000);
            if (!connecte) {
                journal.warn("La connexion initiale au serveur (localhost:5000) a échoué.");
            }
            journal.info("Services client initialisés.");

        } catch (Exception e) {
            journal.error("Erreur critique non gérée lors de l'initialisation de l'application.", e);
            String titreErreur = "Erreur d'Initialisation Critique";
            String contenuErreur = "Une erreur fatale est survenue: " + e.getMessage();
            if (this.paquetRessourcesI18n != null && this.paquetRessourcesI18n.containsKey("error.app.init.critical.title")) {
                titreErreur = this.paquetRessourcesI18n.getString("error.app.init.critical.title");
                contenuErreur = this.paquetRessourcesI18n.getString("error.app.init.critical.content") + "\n" + e.getMessage();
            }
            final String finalTitre = titreErreur;
            final String finalContenu = contenuErreur;
            Platform.runLater(() -> {
                AlertesUtilisateur.afficherErreur(finalTitre, finalContenu);
                Platform.exit();
            });
        }
    }

    @Override
    public void start(Stage stagePrincipal) {
        if (this.paquetRessourcesI18n == null) {
            journal.error("ResourceBundle non initialisé, arrêt de l'application.");
            Platform.exit();
            return;
        }
        if (this.serviceCommunicationServeur == null || this.serviceSessionUtilisateur == null) {
            journal.error("Services non initialisés correctement. Arrêt de l'application.");
            AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.app.start.title"), paquetRessourcesI18n.getString("error.app.start.services_not_ready"));
            Platform.exit();
            return;
        }

        this.stagePrincipal = stagePrincipal;
        this.stagePrincipal.setTitle(paquetRessourcesI18n.getString("app.title"));
        this.gestionnaireNavigation = new GestionnaireNavigation(this.stagePrincipal, this.serviceCommunicationServeur, this.serviceSessionUtilisateur, this.paquetRessourcesI18n);

        try {
            FXMLLoader chargeurFenetrePrincipale = new FXMLLoader(getClass().getResource(GestionnaireNavigation.CHEMIN_VUES_FXML + "FenetrePrincipale.fxml"), paquetRessourcesI18n);
            Parent racinePrincipale = chargeurFenetrePrincipale.load();
            this.controleurFenetrePrincipale = chargeurFenetrePrincipale.getController();

            if (this.controleurFenetrePrincipale != null) {
                this.controleurFenetrePrincipale.initialiserDonneesEtServices(this.gestionnaireNavigation, this.serviceCommunicationServeur, this.serviceSessionUtilisateur, this.paquetRessourcesI18n);
                this.gestionnaireNavigation.setControleurFenetrePrincipale(this.controleurFenetrePrincipale); // Donner une référence au gestionnaire
            } else {
                journal.error("Le contrôleur de la fenêtre principale n'a pas pu être chargé.");
                AlertesUtilisateur.afficherErreur("Erreur Critique", "Impossible de charger le cadre principal.");
                Platform.exit();
                return;
            }

            Scene scenePrincipale = new Scene(racinePrincipale);
            URL urlCss = getClass().getResource(GestionnaireNavigation.CHEMIN_CSS);
            if (urlCss != null) {
                scenePrincipale.getStylesheets().add(urlCss.toExternalForm());
            } else {
                journal.warn("Fichier CSS principal non trouvé : {}", GestionnaireNavigation.CHEMIN_CSS);
            }
            this.stagePrincipal.setScene(scenePrincipale);

            this.gestionnaireNavigation.afficherVueConnexion(); // Charge la vue de connexion dans le cadre principal

            this.stagePrincipal.setOnCloseRequest(event -> {
                journal.info("Demande de fermeture de l'application reçue.");
                stop();
            });

            this.stagePrincipal.show();
            journal.info("Application client BMO démarrée.");

        } catch (Exception e) {
            journal.error("Erreur critique lors du démarrage de l'interface graphique (start).", e);
            AlertesUtilisateur.afficherErreurAvecException(
                    paquetRessourcesI18n.getString("error.app.start.title"),
                    paquetRessourcesI18n.getString("error.app.start.content"),
                    e
            );
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        journal.info("Arrêt de l'application client BMO...");
        if (this.serviceCommunicationServeur != null) {
            this.serviceCommunicationServeur.deconnecterDuServeur();
        }
        journal.info("Application client BMO arrêtée proprement.");
    }
}