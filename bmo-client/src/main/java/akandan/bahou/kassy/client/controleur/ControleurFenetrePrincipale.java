package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;

public class ControleurFenetrePrincipale implements ControleurAvecInitialisation {

    @FXML private BorderPane conteneurPrincipalRacine;
    @FXML private MenuBar barreMenuPrincipale;
    @FXML private MenuItem menuItemFichierQuitter;
    @FXML private MenuItem menuItemNavigationTableauDeBord;
    @FXML private MenuItem menuItemNavigationAdminUtilisateurs;
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

        mettreAJourTextesEtVisibiliteMenus();

        if (serviceSess != null) {
            serviceSess.estUtilisateurConnecteProperty().addListener((obs, ancien, nouveau) -> {
                mettreAJourStatutApplication(nouveau);
                mettreAJourTextesEtVisibiliteMenus();
            });
            // État initial au cas où l'écouteur n'est pas déclenché immédiatement
            mettreAJourStatutApplication(serviceSess.estConnecte());
        }
        if (serviceComm != null) {
            serviceComm.etatConnexionServeurProperty().addListener((obs, etaitCo, estCoMaintenant) -> {
                Platform.runLater(() -> mettreAJourStatutApplication(serviceSess != null && serviceSess.estConnecte()));
            });
        }
    }

    @FXML
    private void initialize() {
        mettreAJourTextesEtVisibiliteMenus();
    }

    private void mettreAJourTextesEtVisibiliteMenus() {
        if (paquetRessourcesI18n != null) {
            if (menuItemFichierQuitter != null) menuItemFichierQuitter.setText(paquetRessourcesI18n.getString("menu.file.quit"));
            if (menuItemAideAPropos != null) menuItemAideAPropos.setText(paquetRessourcesI18n.getString("menu.help.about"));
            if (menuItemNavigationTableauDeBord != null) menuItemNavigationTableauDeBord.setText(paquetRessourcesI18n.getString("menu.navigation.dashboard"));
            if (menuItemNavigationAdminUtilisateurs != null) menuItemNavigationAdminUtilisateurs.setText(paquetRessourcesI18n.getString("menu.navigation.adminUsers"));
        }

        boolean estConnecte = serviceSessionUtilisateur != null && serviceSessionUtilisateur.estConnecte();
        boolean estAdmin = serviceSessionUtilisateur != null && serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR);

        if (menuItemNavigationTableauDeBord != null) {
            menuItemNavigationTableauDeBord.setVisible(estConnecte);
            // MenuItem n'a pas setManaged, la visibilité suffit.
        }
        if (menuItemNavigationAdminUtilisateurs != null) {
            menuItemNavigationAdminUtilisateurs.setVisible(estConnecte && estAdmin);
        }
    }

    public void definirContenuCentral(Node contenu) {
        if (conteneurPrincipalRacine != null) {
            conteneurPrincipalRacine.setCenter(contenu);
        } else {
            journal.warn("conteneurPrincipalRacine est nul, impossible de définir le contenu central.");
        }
    }

    @FXML
    private void actionQuitterApplication(ActionEvent evenement) {
        if (serviceCommunicationServeur != null) {
            serviceCommunicationServeur.envoyerRequeteDeconnexion(); // Notifier le serveur
            serviceCommunicationServeur.deconnecterDuServeur();
        }
        Platform.exit();
    }

    @FXML
    private void actionAfficherAPropos(ActionEvent evenement) {
        String titre = (paquetRessourcesI18n != null && paquetRessourcesI18n.containsKey("about.title")) ? paquetRessourcesI18n.getString("about.title") : "À Propos";
        String contenu = (paquetRessourcesI18n != null && paquetRessourcesI18n.containsKey("about.content")) ? paquetRessourcesI18n.getString("about.content") : "Application BMO - Bureau de Réunions Ouvertes.";
        AlertesUtilisateur.afficherInformation(titre, contenu);
    }

    @FXML
    private void actionNaviguerVersTableauDeBord(ActionEvent evenement) {
        if (gestionnaireNavigation != null && serviceSessionUtilisateur != null && serviceSessionUtilisateur.estConnecte()) {
            gestionnaireNavigation.afficherTableauDeBord();
        } else if (gestionnaireNavigation != null) { // Si pas connecté, retour à la connexion
            gestionnaireNavigation.afficherVueConnexion();
        }
    }

    @FXML
    private void actionNaviguerVersAdminUtilisateurs(ActionEvent evenement) {
        if (gestionnaireNavigation != null && serviceSessionUtilisateur != null && serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR)) {
            gestionnaireNavigation.afficherVueAdministrationUtilisateurs();
        } else {
            AlertesUtilisateur.afficherErreur(
                    (paquetRessourcesI18n != null && paquetRessourcesI18n.containsKey("error.access.denied.title")) ? paquetRessourcesI18n.getString("error.access.denied.title") : "Accès Refusé",
                    (paquetRessourcesI18n != null && paquetRessourcesI18n.containsKey("error.admin.access.required")) ? paquetRessourcesI18n.getString("error.admin.access.required") : "Droits administrateur requis."
            );
        }
    }

    private void mettreAJourStatutApplication(boolean estUtilisateurConnecteLocal) {
        if (etiquetteStatutApplication == null || paquetRessourcesI18n == null) return;

        Platform.runLater(() -> {
            String statutFinal;
            if (serviceCommunicationServeur != null && !serviceCommunicationServeur.estActuellementConnecte()) {
                statutFinal = paquetRessourcesI18n.getString("status.server.disconnected");
            } else if (estUtilisateurConnecteLocal && serviceSessionUtilisateur != null && serviceSessionUtilisateur.getUtilisateurConnecte() != null) {
                statutFinal = String.format(paquetRessourcesI18n.getString("status.connected.as"), serviceSessionUtilisateur.getUtilisateurConnecte().getNomComplet());
            } else {
                statutFinal = paquetRessourcesI18n.getString("status.user.disconnected");
            }
            etiquetteStatutApplication.setText(statutFinal);
        });
    }
}