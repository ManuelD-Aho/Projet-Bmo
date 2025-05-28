package akandan.bahou.kassy.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application principale JavaFX pour le client BMO
 */
public class AppPrincipale extends Application {
    private static final Logger logger = LoggerFactory.getLogger(AppPrincipale.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("🚀 Démarrage de l'application client BMO...");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/akandan/bahou/kassy/client/vues/fxml/VueConnexion.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("BMO - Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        logger.info("✅ Application client démarrée avec succès!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
