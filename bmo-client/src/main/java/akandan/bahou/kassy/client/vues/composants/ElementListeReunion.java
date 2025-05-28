package akandan.bahou.kassy.client.vues.composants;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;

/**
 * Composant personnalisé ElementListeReunion
 */
public class ElementListeReunion extends ListCell<String> {

    private HBox content;
    private Label label;

    public ElementListeReunion() {
        super();
        label = new Label();
        content = new HBox(label);
        content.setSpacing(10);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            label.setText(item);
            setGraphic(content);
        }
    }
}
