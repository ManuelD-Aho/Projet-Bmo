package akandan.bahou.kassy.client.vues.controleurs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour VueAdministrationTableauDeBord
 */
public class ControleurVueAdministrationTableauDeBord implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ControleurVueAdministrationTableauDeBord.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initialisation du contrôleur ControleurVueAdministrationTableauDeBord");
        // Initialisation des composants
    }

    @FXML
    private void handleAction() {
        logger.debug("Action déclenchée dans ControleurVueAdministrationTableauDeBord");
        // Gestion des actions
    }
}
