package akandan.bahou.kassy.client.modeles;

import javafx.beans.property.*;

/**
 * Modèle ObservableListReunions pour l'interface utilisateur
 */
public class ObservableListReunions {

    private final StringProperty nom = new SimpleStringProperty();
    private final BooleanProperty actif = new SimpleBooleanProperty();

    public ObservableListReunions() {
        // Constructeur par défaut
    }

    // Propriétés JavaFX
    public StringProperty nomProperty() { return nom; }
    public String getNom() { return nom.get(); }
    public void setNom(String nom) { this.nom.set(nom); }

    public BooleanProperty actifProperty() { return actif; }
    public boolean isActif() { return actif.get(); }
    public void setActif(boolean actif) { this.actif.set(actif); }
}
