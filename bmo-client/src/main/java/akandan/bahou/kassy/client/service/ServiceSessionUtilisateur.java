package akandan.bahou.kassy.client.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ServiceSessionUtilisateur {

    private final ObjectProperty<DonneesUtilisateurDTO> utilisateurConnecte = new SimpleObjectProperty<>(null);
    private final BooleanProperty estUtilisateurConnecte = new SimpleBooleanProperty(false);
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ServiceSessionUtilisateur.class);

    public ServiceSessionUtilisateur() {
        utilisateurConnecte.addListener((obs, ancienUtilisateur, nouvelUtilisateur) -> estUtilisateurConnecte.set(nouvelUtilisateur != null));
    }

    public void definirSession(DonneesUtilisateurDTO donneesUtilisateur) {
        this.utilisateurConnecte.set(donneesUtilisateur);
        if (donneesUtilisateur != null) {
            journal.info("Session utilisateur définie pour : {} (ID: {})", donneesUtilisateur.getNomComplet(), donneesUtilisateur.getIdUtilisateur());
        } else {
            journal.info("Session utilisateur effacée (DTO utilisateur nul fourni).");
        }
    }

    public void viderSession() {
        this.utilisateurConnecte.set(null);
        journal.info("Session utilisateur vidée.");
    }

    public DonneesUtilisateurDTO getUtilisateurConnecte() {
        return utilisateurConnecte.get();
    }

    public ObjectProperty<DonneesUtilisateurDTO> utilisateurConnecteProperty() {
        return utilisateurConnecte;
    }

    public boolean estConnecte() {
        return estUtilisateurConnecte.get();
    }

    public BooleanProperty estUtilisateurConnecteProperty() {
        return estUtilisateurConnecte;
    }

    public boolean aRole(RoleUtilisateur roleRequis) {
        // Le DTO DonneesUtilisateurDTO devrait avoir getRole() et non getRoleUtilisateur()
        // S'il a getRoleUtilisateur(), alors il faut l'utiliser ici.
        // En supposant que le DTO a bien une méthode getRole() qui retourne RoleUtilisateur:
        return estConnecte() && getUtilisateurConnecte() != null && getUtilisateurConnecte().getRole() == roleRequis;
    }
}