package akandan.bahou.kassy.client.coeur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service GestionnaireEtatClient
 */
@Service
public class GestionnaireEtatClient {
    private static final Logger logger = LoggerFactory.getLogger(GestionnaireEtatClient.class);

    public GestionnaireEtatClient() {
        logger.debug("Initialisation du service GestionnaireEtatClient");
    }

    public void executerAction() {
        logger.info("Exécution d'une action dans GestionnaireEtatClient");
        // Implémentation de la logique métier
    }
}
