package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import java.util.ResourceBundle;

public interface ControleurAvecInitialisation {
    void initialiserDonneesEtServices(
            akandan.bahou.kassy.client.coeur.GestionnaireNavigation gestionnaireNav,
            akandan.bahou.kassy.client.service.ServiceCommunicationServeur serviceComm,
            akandan.bahou.kassy.client.service.ServiceSessionUtilisateur serviceSess,
            java.util.ResourceBundle paquetRessources,
            Object... donneesOptionnelles
    );
}