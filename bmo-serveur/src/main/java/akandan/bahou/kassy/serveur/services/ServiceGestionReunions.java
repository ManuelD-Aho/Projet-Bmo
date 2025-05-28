package akandan.bahou.kassy.serveur.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service ServiceGestionReunions
 */
@Service
public class ServiceGestionReunions {
    private static final Logger logger = LoggerFactory.getLogger(ServiceGestionReunions.class);

    public ServiceGestionReunions() {
        logger.debug("Initialisation du service ServiceGestionReunions");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans ServiceGestionReunions");
        // Implémentation de la logique métier
    }
}
