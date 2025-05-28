package akandan.bahou.kassy.serveur.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service ServiceAuthentification
 */
@Service
public class ServiceAuthentification {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAuthentification.class);

    public ServiceAuthentification() {
        logger.debug("Initialisation du service ServiceAuthentification");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans ServiceAuthentification");
        // Implémentation de la logique métier
    }
}
