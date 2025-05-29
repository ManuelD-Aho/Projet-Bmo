package akandan.bahou.kassy.client.coeur;

import akandan.bahou.kassy.client.controleur.ControleurAvecInitialisation;
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
        chargerEtAfficherVue("FenetreConnexion.fxml", "login.title", false);
    }

    public void afficherTableauDeBord() {
        chargerEtAfficherVue("TableauDeBord.fxml", "dashboard.title", false);
    }

    public void afficherFenetrePrincipale() {
        chargerEtAfficherVue("FenetrePrincipale.fxml", "app.title", false);
    }

    public void afficherVuePlanificationReunion(DetailsReunionDTO reunionExistantePeutEtreNulle) {
        String cleTitre = (reunionExistantePeutEtreNulle == null) ? "meeting.planning.title.create" : "meeting.planning.title.edit";
        chargerEtAfficherVue("PlanificationReunion.fxml", cleTitre, false, reunionExistantePeutEtreNulle);
    }

    public void afficherVueSalleReunion(DetailsReunionDTO detailsReunion) {
        String titreFenetre;
        boolean estTitreDirectPourSalle = false;
        if (detailsReunion != null && detailsReunion.getTitre() != null && !detailsReunion.getTitre().trim().isEmpty()) {
            titreFenetre = detailsReunion.getTitre();
            estTitreDirectPourSalle = true; // Le titre vient directement du DTO
        } else {
            titreFenetre = "meeting.room.default.title"; // C'est une clé i18n
            estTitreDirectPourSalle = false;
        }
        chargerEtAfficherVue("SalleReunion.fxml", titreFenetre, estTitreDirectPourSalle, detailsReunion);
    }

    public void afficherVueAdministrationUtilisateurs() {
        chargerEtAfficherVue("AdministrationUtilisateurs.fxml", "admin.users.title", false);
    }

    // Méthode principale de chargement de vue
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
                ((ControleurAvecInitialisation) controleur).initialiserDonneesEtServices(this, serviceCommunicationServeur, serviceSessionUtilisateur, paquetRessourcesI18n, donneesOptionnellesControleur);
            }

            Scene nouvelleScene = new Scene(racine);
            URL urlCss = getClass().getResource(CHEMIN_CSS);
            if (urlCss != null) {
                nouvelleScene.getStylesheets().add(urlCss.toExternalForm());
            } else {
                journal.warn("Fichier CSS principal non trouvé : {}", CHEMIN_CSS);
            }

            this.stagePrincipal.setScene(nouvelleScene);
            String titreFenetreFinal;
            if (estTitreDirect) {
                titreFenetreFinal = cleOuTitreFenetre;
            } else {
                try {
                    titreFenetreFinal = paquetRessourcesI18n.getString(cleOuTitreFenetre);
                } catch (Exception e) {
                    journal.warn("Clé i18n non trouvée pour le titre : {}. Utilisation de la clé brute.", cleOuTitreFenetre);
                    titreFenetreFinal = cleOuTitreFenetre;
                }
            }
            this.stagePrincipal.setTitle(titreFenetreFinal);
            this.stagePrincipal.sizeToScene();
            this.stagePrincipal.centerOnScreen();
            journal.debug("Vue {} chargée et affichée avec le titre : {}", nomFichierFxml, titreFenetreFinal);

        } catch (IOException e) {
            journal.error("Erreur d'E/S lors du chargement de la vue FXML : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreurAvecException(
                    paquetRessourcesI18n.getString("error.fxml.load.title"),
                    paquetRessourcesI18n.getString("error.fxml.load.content") + ": " + nomFichierFxml,
                    e
            );
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de l'affichage de la vue : " + nomFichierFxml, e);
            AlertesUtilisateur.afficherErreurAvecException(
                    paquetRessourcesI18n.getString("error.view.display.title"),
                    paquetRessourcesI18n.getString("error.view.display.content") + ": " + nomFichierFxml,
                    e
            );
        }
    }
}