package akandan.bahou.kassy.client.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service ServiceSessionUtilisateur
 */
@Service
public class ServiceSessionUtilisateur {
    private static final Logger logger = LoggerFactory.getLogger(ServiceSessionUtilisateur.class);

    public ServiceSessionUtilisateur() {
        logger.debug("Initialisation du service ServiceSessionUtilisateur");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans ServiceSessionUtilisateur");
        // Implémentation de la logique métier
    }
}
