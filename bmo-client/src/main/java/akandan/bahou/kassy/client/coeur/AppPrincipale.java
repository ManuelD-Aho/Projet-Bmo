package akandan.bahou.kassy.client.coeur;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader; // Bien que non utilisé directement ici, souvent nécessaire avec la navigation
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Locale;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import akandan.bahou.kassy.client.util.AlertesUtilisateur; // Pour les erreurs d'initialisation
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
    public void init() throws Exception {
        try {
            this.paquetRessourcesI18n = ResourceBundle.getBundle("akandan.bahou.kassy.client.vues.i18n.messages", new Locale("fr", "FR"));
            this.serviceSessionUtilisateur = new ServiceSessionUtilisateur();
            this.serviceCommunicationServeur = new ServiceCommunicationServeur(this.serviceSessionUtilisateur, this.paquetRessourcesI18n);

            boolean connecte = this.serviceCommunicationServeur.connecterAuServeur("localhost", 5000);
            if (!connecte) {
                journal.warn("La connexion initiale au serveur a échoué. L'utilisateur devra réessayer depuis la vue de connexion.");
                // Une alerte pourrait être affichée ici, mais la vue de connexion gérera aussi les échecs.
            }
            journal.info("Services client initialisés.");
        } catch (Exception e) {
            journal.error("Erreur critique lors de l'initialisation de l'application.", e);
            if (this.paquetRessourcesI18n == null) { // Si i18n n'a pas pu charger, utiliser des messages par défaut
                AlertesUtilisateur.afficherErreur("Erreur Critique", "Impossible d'initialiser l'application : " + e.getMessage());
            } else {
                AlertesUtilisateur.afficherErreur(
                        paquetRessourcesI18n.getString("error.app.init.title"),
                        paquetRessourcesI18n.getString("error.app.init.content") + ": " + e.getMessage()
                );
            }
            throw e; // Relaunche pour arrêter l'application si init échoue gravement
        }
    }

    @Override
    public void start(Stage stagePrincipal) throws IOException {
        try {
            this.stagePrincipal = stagePrincipal;
            this.stagePrincipal.setTitle(paquetRessourcesI18n.getString("app.title"));

            this.gestionnaireNavigation = new GestionnaireNavigation(this.stagePrincipal, this.serviceCommunicationServeur, this.serviceSessionUtilisateur, this.paquetRessourcesI18n);
            this.gestionnaireNavigation.afficherVueConnexion();

            this.stagePrincipal.setOnCloseRequest(event -> {
                try {
                    stop();
                } catch (Exception e) {
                    journal.error("Erreur lors de la fermeture de l'application.", e);
                }
            });

            this.stagePrincipal.show();
            journal.info("Application client BMO démarrée et vue de connexion affichée.");
        } catch (Exception e) {
            journal.error("Erreur lors du démarrage de l'interface graphique.", e);
            AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.app.start.title"),
                    paquetRessourcesI18n.getString("error.app.start.content") + ": " + e.getMessage()
            );
            Platform.exit(); // Quitter si start échoue
        }
    }

    @Override
    public void stop() throws Exception {
        if (this.serviceCommunicationServeur != null) {
            this.serviceCommunicationServeur.deconnecterDuServeur();
        }
        journal.info("Application client BMO arrêtée.");
    }
}