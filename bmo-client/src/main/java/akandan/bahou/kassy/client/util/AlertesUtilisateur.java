package akandan.bahou.kassy.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class AlertesUtilisateur {

    private static void configurerEtAfficherAlerte(Alert alerte, String titre, String message) {
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stage = (Stage) alerte.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alerte.showAndWait();
    }

    private static void configurerAlerte(Alert alerte, String titre, String message) {
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Stage stage = (Stage) alerte.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
    }

    public static void afficherInformation(String titre, String message) {
        Alert alerte = new Alert(AlertType.INFORMATION);
        configurerEtAfficherAlerte(alerte, titre, message);
    }

    public static void afficherAvertissement(String titre, String message) {
        Alert alerte = new Alert(AlertType.WARNING);
        configurerEtAfficherAlerte(alerte, titre, message);
    }

    public static void afficherErreur(String titre, String message) {
        Alert alerte = new Alert(AlertType.ERROR);
        configurerEtAfficherAlerte(alerte, titre, message);
    }

    public static void afficherErreurAvecException(String titre, String message, Exception ex) {
        Alert alerte = new Alert(AlertType.ERROR);
        configurerAlerte(alerte, titre, message);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String traceException = sw.toString();

        TextArea textArea = new TextArea(traceException);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane contenuDetaille = new GridPane();
        contenuDetaille.setMaxWidth(Double.MAX_VALUE);
        contenuDetaille.add(new Label("La trace de l'exception était :"), 0, 0);
        contenuDetaille.add(textArea, 0, 1);

        alerte.getDialogPane().setExpandableContent(contenuDetaille);
        alerte.showAndWait();
    }

    public static boolean afficherConfirmation(String titre, String message) {
        Alert alerte = new Alert(AlertType.CONFIRMATION);
        // Utilisation des boutons par défaut (OK, Annuler) car leurs textes sont généralement gérés par le système d'exploitation/JavaFX selon la locale.
        // Si des textes spécifiques sont nécessaires (Oui/Non), il faudrait les définir explicitement :
        // ButtonType boutonOui = new ButtonType("Oui", ButtonBar.ButtonData.YES);
        // ButtonType boutonNon = new ButtonType("Non", ButtonBar.ButtonData.NO);
        // alerte.getButtonTypes().setAll(boutonOui, boutonNon);
        configurerAlerte(alerte, titre, message); // Ne pas appeler showAndWait ici

        Optional<ButtonType> resultat = alerte.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.OK; // Ou boutonOui si défini explicitement
    }
}