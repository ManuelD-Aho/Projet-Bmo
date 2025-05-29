package akandan.bahou.kassy.client.coeur;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.controleur.ControleurAvecInitialisation;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import org.slf4j.Logger;

public class GestionnaireNavigation {

    private final Stage stagePrincipal;
    private final ServiceCommunicationServeur serviceCommunicationServeur;
    private final ServiceSessionUtilisateur serviceSessionUtilisateur;
    private final ResourceBundle paquetRessourcesI18n;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(GestionnaireNavigation.class);
    private static final String CHEMIN_VUES_FXML = "/akandan/bahou/kassy/client/vues/vue/";
    private static final String CHEMIN_CSS = "/akandan/bahou/kassy/client/vues/css/style-bmo.css";


    public GestionnaireNavigation(Stage stagePrincipal, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources) {
        this.stagePrincipal = stagePrincipal;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;
    }

    public void afficherVueConnexion() {
        chargerEtAfficherVue("FenetreConnexion.fxml", "login.title");
    }

    public void afficherTableauDeBord() {
        chargerEtAfficherVue("TableauDeBord.fxml", "dashboard.title");
    }

    public void afficherVuePlanificationReunion(DetailsReunionDTO reunionExistantePeutEtreNulle) {
        chargerEtAfficherVue("PlanificationReunion.fxml", "meeting.planning.title", reunionExistantePeutEtreNulle);
    }

    public void afficherVueSalleReunion(DetailsReunionDTO detailsReunion) {
        String titreFenetre = detailsReunion != null && detailsReunion.getTitre() != null && !detailsReunion.getTitre().isEmpty()
                ? detailsReunion.getTitre()
                : paquetRessourcesI18n.getString("meeting.room.default.title");
        chargerEtAfficherVue("SalleReunion.fxml", titreFenetre, detailsReunion, true); // true pour indiquer que c'est une clé et non une chaîne directe
    }

    public void afficherVueAdministrationUtilisateurs() {
        chargerEtAfficherVue("AdministrationUtilisateurs.fxml", "admin.users.title");
    }

    private void chargerEtAfficherVue(String nomFichierFxml, String cleOuTitreFenetre, Object... donneesOptionnellesControleur) {
        chargerEtAfficherVue(nomFichierFxml, cleOuTitreFenetre, false, donneesOptionnellesControleur);
    }


    private void chargerEtAfficherVue(String nomFichierFxml, String cleOuTitreFenetre, boolean estTitreDirect, Object... donneesOptionnellesControleur) {
        try {
            String cheminCompletFxml = CHEMIN_VUES_FXML + nomFichierFxml;
            URL urlFxml = getClass().getResource(cheminCompletFxml);
            if (urlFxml == null) {
                journal.error("Fichier FXML non trouvé : {}", cheminCompletFxml);
                AlertesUtilisateur.afficherErreur(
                        paquetRessourcesI18n.getString("error.fxml.load.title"),
                        paquetRessourcesI18n.getString("error.fxml.notfound") + ": " + nomFichierFxml
                );
                return;
            }

            FXMLLoader chargeurFxml = new FXMLLoader(urlFxml, paquetRessourcesI18n);
            Parent racine = chargeurFxml.load();

            Object controleur = chargeurFxml.getController();
            if (controleur instanceof ControleurAvecInitialisation) {
                ((ControleurAvecInitialisation) controleur).initialiserDonneesEtServices(this, serviceCommunicationServeur, serviceSessionUtilisateur, donneesOptionnellesControleur);
            }

            Scene nouvelleScene = new Scene(racine); // Prévoir taille dynamique ou par défaut
            URL urlCss = getClass().getResource(CHEMIN_CSS);
            if (urlCss != null) {
                nouvelleScene.getStylesheets().add(urlCss.toExternalForm());
            } else {
                journal.warn("Fichier CSS non trouvé : {}", CHEMIN_CSS);
            }

            this.stagePrincipal.setScene(nouvelleScene);
            if (estTitreDirect) {
                this.stagePrincipal.setTitle(cleOuTitreFenetre);
            } else {
                this.stagePrincipal.setTitle(paquetRessourcesI18n.getString(cleOuTitreFenetre));
            }
            this.stagePrincipal.sizeToScene(); // Ajuste la taille du stage au contenu de la scène
            this.stagePrincipal.centerOnScreen(); // Centre la fenêtre
            journal.debug("Vue {} chargée et affichée.", nomFichierFxml);

        } catch (IOException e) {
            journal.error("Erreur lors du chargement de la vue FXML : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.fxml.load.title"),
                    paquetRessourcesI18n.getString("error.fxml.load.content") + ": " + nomFichierFxml + "\n" + e.getMessage()
            );
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de l'affichage de la vue : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.view.display.title"),
                    paquetRessourcesI18n.getString("error.view.display.content") + ": " + nomFichierFxml + "\n" + e.getMessage()
            );
        }
    }
}