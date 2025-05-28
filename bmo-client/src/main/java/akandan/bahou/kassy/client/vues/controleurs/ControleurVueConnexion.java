package akandan.bahou.kassy.client.vues.controleurs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour VueConnexion
 */
public class ControleurVueConnexion implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ControleurVueConnexion.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialisation du contrôleur ControleurVueConnexion");
        // Initialisation des composants
    }

    @FXML
    private void handleAction() {
        logger.debug("Action déclenchée dans ControleurVueConnexion");
        // Gestion des actions
    }
}
