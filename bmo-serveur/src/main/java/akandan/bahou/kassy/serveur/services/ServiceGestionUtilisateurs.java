package akandan.bahou.kassy.serveur.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service ServiceGestionUtilisateurs
 */
@Service
public class ServiceGestionUtilisateurs {
    private static final Logger logger = LoggerFactory.getLogger(ServiceGestionUtilisateurs.class);

    public ServiceGestionUtilisateurs() {
        logger.debug("Initialisation du service ServiceGestionUtilisateurs");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans ServiceGestionUtilisateurs");
        // Implémentation de la logique métier
    }
}
