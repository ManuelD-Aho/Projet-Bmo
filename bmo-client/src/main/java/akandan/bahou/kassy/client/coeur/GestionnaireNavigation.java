package akandan.bahou.kassy.client.coeur;

import akandan.bahou.kassy.client.controleur.ControleurAvecInitialisation;
import akandan.bahou.kassy.client.controleur.ControleurFenetrePrincipale;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class GestionnaireNavigation {

    private final Stage stagePrincipal;
    private final ServiceCommunicationServeur serviceCommunicationServeur;
    private final ServiceSessionUtilisateur serviceSessionUtilisateur;
    private final ResourceBundle paquetRessourcesI18n;
    private ControleurFenetrePrincipale controleurFenetrePrincipale; // Référence au contrôleur du cadre
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(GestionnaireNavigation.class);
    public static final String CHEMIN_VUES_FXML = "/akandan/bahou/kassy/client/vues/vue/"; // Rendu public pour AppPrincipale
    public static final String CHEMIN_CSS = "/akandan/bahou/kassy/client/vues/css/style-bmo.css";

    public GestionnaireNavigation(Stage stagePrincipal, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources) {
        this.stagePrincipal = stagePrincipal;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;
    }

    public void setControleurFenetrePrincipale(ControleurFenetrePrincipale controleur) {
        this.controleurFenetrePrincipale = controleur;
    }

    private void chargerVueDansConteneurCentral(String nomFichierFxml, String cleTitreOptionnel, Object... donneesOptionnellesControleur) {
        if (this.controleurFenetrePrincipale == null) {
            journal.error("Impossible de charger la vue '{}' car le contrôleur de la fenêtre principale n'est pas défini.", nomFichierFxml);
            AlertesUtilisateur.afficherErreur("Erreur de Navigation", "Le cadre principal de l'application n'est pas prêt.");
            return;
        }
        try {
            String cheminCompletFxml = CHEMIN_VUES_FXML + nomFichierFxml;
            URL urlFxml = getClass().getResource(cheminCompletFxml);
            if (urlFxml == null) {
                journal.error("Fichier FXML non trouvé : {}", cheminCompletFxml);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.fxml.load.title"), paquetRessourcesI18n.getString("error.fxml.notfound") + ": " + nomFichierFxml);
                return;
            }

            FXMLLoader chargeurFxml = new FXMLLoader(urlFxml, paquetRessourcesI18n);
            Parent racineVueInterne = chargeurFxml.load();

            Object controleur = chargeurFxml.getController();
            if (controleur instanceof ControleurAvecInitialisation) {
                ((ControleurAvecInitialisation) controleur).initialiserDonneesEtServices(this, serviceCommunicationServeur, serviceSessionUtilisateur, paquetRessourcesI18n, donneesOptionnellesControleur);
            }

            this.controleurFenetrePrincipale.definirContenuCentral(racineVueInterne);
            // Optionnellement, mettre à jour le titre du stage principal si la sous-vue doit le changer
            // if (cleTitreOptionnel != null) this.stagePrincipal.setTitle(paquetRessourcesI18n.getString(cleTitreOptionnel));

            journal.debug("Vue {} chargée dans le conteneur central.", nomFichierFxml);

        } catch (IOException e) {
            journal.error("Erreur d'E/S lors du chargement de la vue FXML interne : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreurAvecException(paquetRessourcesI18n.getString("error.fxml.load.title"), nomFichierFxml, e);
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de l'affichage de la vue interne : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreurAvecException(paquetRessourcesI18n.getString("error.view.display.title"), nomFichierFxml, e);
        }
    }

    // Cette méthode remplace toute la scène, utile si FenetrePrincipale n'est pas utilisée comme cadre permanent
    private void chargerEtRemplacerScene(String nomFichierFxml, String cleTitreFenetre) {
        try {
            String cheminCompletFxml = CHEMIN_VUES_FXML + nomFichierFxml;
            URL urlFxml = getClass().getResource(cheminCompletFxml);
            if (urlFxml == null) {
                journal.error("Fichier FXML non trouvé : {}", cheminCompletFxml);
                AlertesUtilisateur.afficherErreur( paquetRessourcesI18n.getString("error.fxml.load.title"), paquetRessourcesI18n.getString("error.fxml.notfound") + ": " + nomFichierFxml);
                return;
            }

            FXMLLoader chargeurFxml = new FXMLLoader(urlFxml, paquetRessourcesI18n);
            Parent racine = chargeurFxml.load();

            Object controleur = chargeurFxml.getController();
            if (controleur instanceof ControleurAvecInitialisation) {
                ((ControleurAvecInitialisation) controleur).initialiserDonneesEtServices(this, serviceCommunicationServeur, serviceSessionUtilisateur, paquetRessourcesI18n);
            }

            Scene nouvelleScene = new Scene(racine);
            URL urlCss = getClass().getResource(CHEMIN_CSS);
            if (urlCss != null) { nouvelleScene.getStylesheets().add(urlCss.toExternalForm());}
            else { journal.warn("Fichier CSS principal non trouvé : {}", CHEMIN_CSS); }

            this.stagePrincipal.setScene(nouvelleScene);
            try {
                this.stagePrincipal.setTitle(paquetRessourcesI18n.getString(cleTitreFenetre));
            } catch (Exception e) {
                journal.warn("Clé i18n non trouvée pour le titre : {}. Utilisation de la clé brute.", cleTitreFenetre);
                this.stagePrincipal.setTitle(cleTitreFenetre);
            }
            this.stagePrincipal.sizeToScene();
            this.stagePrincipal.centerOnScreen();
            journal.debug("Scène remplacée par {} avec le titre : {}", nomFichierFxml, this.stagePrincipal.getTitle());

        } catch (Exception e) {
            journal.error("Erreur lors du chargement et remplacement de scène pour {} : {}", nomFichierFxml, e.getMessage(), e);
            AlertesUtilisateur.afficherErreurAvecException(paquetRessourcesI18n.getString("error.scene.load.title"), nomFichierFxml, e);
        }
    }


    public void afficherVueConnexion() {
        // Si FenetrePrincipale est le cadre, charger la connexion dedans.
        // Sinon, FenetrePrincipale n'est pas encore chargée, donc on remplace la scène.
        if (controleurFenetrePrincipale != null) {
            chargerVueDansConteneurCentral("FenetreConnexion.fxml", "login.title");
        } else {
            // Ce cas ne devrait pas arriver si AppPrincipale charge FenetrePrincipale d'abord
            journal.warn("Tentative d'affichage de VueConnexion sans FenetrePrincipale chargée. Remplacement de la scène.");
            chargerEtRemplacerScene("FenetreConnexion.fxml", "login.title");
        }
    }

    public void afficherTableauDeBord() {
        chargerVueDansConteneurCentral("TableauDeBord.fxml", "dashboard.title");
    }

    public void afficherVuePlanificationReunion(DetailsReunionDTO reunionExistantePeutEtreNulle) {
        String cleTitre = (reunionExistantePeutEtreNulle == null) ? "meeting.planning.title.create" : "meeting.planning.title.edit";
        chargerVueDansConteneurCentral("PlanificationReunion.fxml", cleTitre, reunionExistantePeutEtreNulle);
    }

    public void afficherVueSalleReunion(DetailsReunionDTO detailsReunion) {
        String titreFenetre;
        if (detailsReunion != null && detailsReunion.getTitre() != null && !detailsReunion.getTitre().trim().isEmpty()) {
            titreFenetre = detailsReunion.getTitre();
            // Si le titre est direct, on ne le préfixe pas avec la clé i18n.
            // La méthode chargerVueDansConteneurCentral ne change pas le titre du Stage principal par défaut.
            chargerVueDansConteneurCentral("SalleReunion.fxml", null, detailsReunion); // null pour cleTitreOptionnel pour ne pas le chercher dans i18n
            stagePrincipal.setTitle(titreFenetre); // Mettre à jour le titre du stage directement
        } else {
            titreFenetre = paquetRessourcesI18n.getString("meeting.room.default.title");
            chargerVueDansConteneurCentral("SalleReunion.fxml", "meeting.room.default.title", detailsReunion);
        }
    }

    public void afficherVueAdministrationUtilisateurs() {
        chargerVueDansConteneurCentral("AdministrationUtilisateurs.fxml", "admin.users.title");
    }
}