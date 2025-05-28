package akandan.bahou.kassy.serveur.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service ServiceNotification
 */
@Service
public class ServiceNotification {
    private static final Logger logger = LoggerFactory.getLogger(ServiceNotification.class);

    public ServiceNotification() {
        logger.debug("Initialisation du service ServiceNotification");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans ServiceNotification");
        // Implémentation de la logique métier
    }
}
