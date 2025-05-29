package akandan.bahou.kassy.client.coeur;

import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.io.IOException; // Gardé car FXMLLoader peut le lever, même si géré par GestionnaireNavigation
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class AppPrincipale extends Application {

    private Stage stagePrincipal;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private GestionnaireNavigation gestionnaireNavigation;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(AppPrincipale.class);
    private ResourceBundle paquetRessourcesI18n;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() { // Retiré throws Exception, gestion interne des exceptions critiques
        try {
            // 1. Charger le ResourceBundle en premier pour les messages d'erreur potentiels
            try {
                this.paquetRessourcesI18n = ResourceBundle.getBundle("akandan.bahou.kassy.client.vues.i18n.messages", new Locale("fr", "FR"));
            } catch (MissingResourceException mre) {
                journal.error("Fichier de ressources i18n introuvable. L'application ne peut pas démarrer correctement.", mre);
                // Afficher une alerte en anglais si i18n échoue
                Platform.runLater(() -> AlertesUtilisateur.afficherErreur("Critical Error", "Localization files not found. Application cannot start."));
                // Impossible de continuer sans i18n pour les messages d'erreur localisés.
                // On pourrait lancer une RuntimeException pour forcer l'arrêt si JavaFX le permet ici.
                // Pour l'instant, on laisse start() tenter et échouer si paquetRessourcesI18n est null.
                return; // Arrêter l'initialisation ici.
            }

            // 2. Initialiser les services
            this.serviceSessionUtilisateur = new ServiceSessionUtilisateur();
            this.serviceCommunicationServeur = new ServiceCommunicationServeur(this.serviceSessionUtilisateur, this.paquetRessourcesI18n);

            // 3. Tenter la connexion au serveur (non bloquant pour init)
            boolean connecte = this.serviceCommunicationServeur.connecterAuServeur("localhost", 5000); // TODO: Externaliser host/port
            if (!connecte) {
                journal.warn("La connexion initiale au serveur (localhost:5000) a échoué. L'utilisateur devra réessayer depuis la vue de connexion.");
                // Une alerte pourrait être affichée ici via Platform.runLater,
                // mais la vue de connexion gérera l'affichage de l'état de la connexion.
            }
            journal.info("Services client initialisés.");

        } catch (Exception e) { // Capture large pour toute autre erreur non prévue durant init
            journal.error("Erreur critique non gérée lors de l'initialisation de l'application.", e);
            // Tenter d'afficher une alerte, même si i18n a pu échouer
            String titreErreur = "Erreur d'Initialisation Critique";
            String contenuErreur = "Une erreur fatale est survenue lors de l'initialisation de l'application: " + e.getMessage();
            if (this.paquetRessourcesI18n != null && this.paquetRessourcesI18n.containsKey("error.app.init.critical.title")) {
                titreErreur = this.paquetRessourcesI18n.getString("error.app.init.critical.title");
                contenuErreur = this.paquetRessourcesI18n.getString("error.app.init.critical.content") + "\n" + e.getMessage();
            }
            final String finalTitre = titreErreur;
            final String finalContenu = contenuErreur;
            if (Platform.isFxApplicationThread()) {
                AlertesUtilisateur.afficherErreur(finalTitre, finalContenu);
            } else {
                Platform.runLater(() -> AlertesUtilisateur.afficherErreur(finalTitre, finalContenu));
            }
            // Il est difficile de garantir que l'application s'arrêtera proprement si init() lève une exception
            // JavaFX pourrait continuer vers start() ou s'arrêter. Relauncher peut aider dans certains cas.
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Échec de l'initialisation de l'application", e);
        }
    }

    @Override
    public void start(Stage stagePrincipal) { // Retiré throws IOException
        if (this.paquetRessourcesI18n == null) {
            // init() a échoué à charger i18n, on ne peut pas continuer
            journal.error("ResourceBundle non initialisé, impossible de démarrer l'interface graphique.");
            AlertesUtilisateur.afficherErreur("Erreur Fatale", "Les fichiers de localisation sont manquants. L'application va se fermer.");
            Platform.exit();
            return;
        }

        try {
            this.stagePrincipal = stagePrincipal;
            this.stagePrincipal.setTitle(paquetRessourcesI18n.getString("app.title"));

            // S'assurer que les services sont initialisés (normalement fait dans init)
            if (this.serviceCommunicationServeur == null || this.serviceSessionUtilisateur == null) {
                journal.error("Les services n'ont pas été initialisés correctement dans init().");
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.app.start.title"), paquetRessourcesI18n.getString("error.app.start.services_not_ready"));
                Platform.exit();
                return;
            }

            this.gestionnaireNavigation = new GestionnaireNavigation(this.stagePrincipal, this.serviceCommunicationServeur, this.serviceSessionUtilisateur, this.paquetRessourcesI18n);

            // Afficher la Fenêtre Principale qui servira de cadre
            this.gestionnaireNavigation.afficherFenetrePrincipale();
            // La FenetrePrincipale devrait ensuite charger la vue de connexion en son centre,
            // ou GestionnaireNavigation.afficherVueConnexion() devrait remplacer toute la scène.
            // Si FenetrePrincipale est le cadre, la navigation vers connexion se fait via son contrôleur
            // ou le gestionnaire modifie le centre de FenetrePrincipale.
            // Pour un flux simple : GestionnaireNavigation charge directement FenetreConnexion.
            // this.gestionnaireNavigation.afficherVueConnexion(); // Déplacé vers le contrôleur de FenetrePrincipale ou appelé ici si FenetrePrincipale n'est pas utilisée comme cadre.

            this.stagePrincipal.setOnCloseRequest(event -> {
                journal.info("Demande de fermeture de l'application reçue.");
                try {
                    stop(); // Appeler notre méthode stop() pour nettoyage
                } catch (Exception e) {
                    journal.error("Erreur lors de l'appel à stop() sur la fermeture de la fenêtre.", e);
                }
                // Ne pas consommer l'événement pour permettre à JavaFX de fermer la fenêtre
            });

            this.stagePrincipal.show();
            journal.info("Application client BMO démarrée.");

        } catch (Exception e) { // Capture large pour les erreurs FXML ou autres pdt start
            journal.error("Erreur critique lors du démarrage de l'interface graphique (start).", e);
            AlertesUtilisateur.afficherErreurAvecException( // Afficher la trace si possible
                    paquetRessourcesI18n.getString("error.app.start.title"),
                    paquetRessourcesI18n.getString("error.app.start.content"),
                    e
            );
            Platform.exit();
        }
    }

    @Override
    public void stop() { // Retiré throws Exception
        journal.info("Arrêt de l'application client BMO...");
        if (this.serviceCommunicationServeur != null) {
            this.serviceCommunicationServeur.deconnecterDuServeur();
        }
        journal.info("Application client BMO arrêtée proprement.");
        // Platform.exit() est appelé par JavaFX, pas besoin de System.exit(0) en général.
    }
}