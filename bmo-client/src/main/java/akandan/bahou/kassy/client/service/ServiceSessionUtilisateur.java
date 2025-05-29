package akandan.bahou.kassy.client.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;
import java.time.LocalDateTime;

public class ServiceSessionUtilisateur {

    private final ObjectProperty<DonneesUtilisateurDTO> utilisateurConnecte = new SimpleObjectProperty<>(null);
    private final BooleanProperty estUtilisateurConnecte = new SimpleBooleanProperty(false);
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ServiceSessionUtilisateur.class);

    public ServiceSessionUtilisateur() {
        utilisateurConnecte.addListener((obs, ancienUtilisateur, nouvelUtilisateur) -> estUtilisateurConnecte.set(nouvelUtilisateur != null));
    }

    public void definirSession(long idUtilisateur, String nomUtilisateur, RoleUtilisateur role, String identifiantConnexion, LocalDateTime dateDerniereConnexion, StatutCompteUtilisateur statutCompte) {
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdUtilisateur(idUtilisateur);
        dto.setNomComplet(nomUtilisateur);
        dto.setRole(role);
        dto.setIdentifiant(identifiantConnexion);
        dto.setDateDerniereConnexion(dateDerniereConnexion);
        dto.setStatutCompte(statutCompte);
        this.utilisateurConnecte.set(dto);
        journal.info("Session utilisateur définie pour : {} (ID: {})", nomUtilisateur, idUtilisateur);
    }

    public void definirSession(DonneesUtilisateurDTO donneesUtilisateur) {
        this.utilisateurConnecte.set(donneesUtilisateur);
        if (donneesUtilisateur != null) {
            journal.info("Session utilisateur définie pour : {} (ID: {})", donneesUtilisateur.getNomComplet(), donneesUtilisateur.getIdUtilisateur());
        } else {
            journal.info("Session utilisateur effacée car le DTO fourni est nul.");
            effacerSession(); // Assure la cohérence de l'état
        }
    }

    public void effacerSession() {
        this.utilisateurConnecte.set(null);
        journal.info("Session utilisateur effacée.");
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
        return estConnecte() && utilisateurConnecte.get() != null && utilisateurConnecte.get().getRole() == roleRequis;
    }

    public void viderSession() {
        effacerSession();
    }
}