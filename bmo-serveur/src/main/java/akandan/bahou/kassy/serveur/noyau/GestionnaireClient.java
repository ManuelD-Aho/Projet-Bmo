package akandan.bahou.kassy.serveur.noyau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service GestionnaireClient
 */
@Service
public class GestionnaireClient {
    private static final Logger logger = LoggerFactory.getLogger(GestionnaireClient.class);

    public GestionnaireClient() {
        logger.debug("Initialisation du service GestionnaireClient");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans GestionnaireClient");
        // Implémentation de la logique métier
    }
}
