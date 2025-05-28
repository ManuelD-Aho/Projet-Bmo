package akandan.bahou.kassy.serveur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principale de démarrage du serveur BMO
 */
public class ServeurPrincipal {
    private static final Logger logger = LoggerFactory.getLogger(ServeurPrincipal.class);

    public static void main(String[] args) {
        logger.info("🚀 Démarrage du serveur BMO...");

        try {
            // Initialisation du serveur
            logger.info("✅ ServeurPrincipal démarré avec succès!");
        } catch (Exception e) {
            logger.error("❌ Erreur lors du démarrage du serveur", e);
            System.exit(1);
        }
    }
}
