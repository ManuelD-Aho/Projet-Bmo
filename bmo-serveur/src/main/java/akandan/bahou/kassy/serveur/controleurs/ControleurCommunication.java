package akandan.bahou.kassy.serveur.controleurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur ControleurCommunication
 */
@RestController
@RequestMapping("/api")
public class ControleurCommunication {
    private static final Logger logger = LoggerFactory.getLogger(ControleurCommunication.class);

    @GetMapping("/status")
    public String getStatus() {
        logger.info("Demande de statut reçue");
        return "OK";
    }
}
