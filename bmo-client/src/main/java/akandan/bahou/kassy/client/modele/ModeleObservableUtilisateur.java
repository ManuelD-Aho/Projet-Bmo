package akandan.bahou.kassy.client.modele;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;

public class ModeleObservableUtilisateur {

    private final StringProperty idUtilisateur;
    private final StringProperty nomComplet;
    private final StringProperty identifiant; // Renommé pour correspondre au DTO
    private final ObjectProperty<RoleUtilisateur> role;
    private final ObjectProperty<LocalDateTime> dateCreationCompte;
    private final ObjectProperty<LocalDateTime> dateDerniereConnexion;
    private final ObjectProperty<StatutCompteUtilisateur> statutCompte;

    private DonneesUtilisateurDTO donneesDTOOriginal;

    public ModeleObservableUtilisateur(DonneesUtilisateurDTO dto) {
        this.idUtilisateur = new SimpleStringProperty(String.valueOf(dto.getIdUtilisateur()));
        this.nomComplet = new SimpleStringProperty(dto.getNomComplet());
        this.identifiant = new SimpleStringProperty(dto.getIdentifiant());
        this.role = new SimpleObjectProperty<>(dto.getRole());
        this.dateCreationCompte = new SimpleObjectProperty<>(dto.getDateCreationCompte());
        this.dateDerniereConnexion = new SimpleObjectProperty<>(dto.getDateDerniereConnexion());
        this.statutCompte = new SimpleObjectProperty<>(dto.getStatutCompte());
        this.donneesDTOOriginal = dto;
    }

    public StringProperty idUtilisateurProperty() { return idUtilisateur; }
    public String getIdUtilisateur() { return idUtilisateur.get(); }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur.set(idUtilisateur); }

    public StringProperty nomCompletProperty() { return nomComplet; }
    public String getNomComplet() { return nomComplet.get(); }
    public void setNomComplet(String nomComplet) { this.nomComplet.set(nomComplet); }

    public StringProperty identifiantProperty() { return identifiant; }
    public String getIdentifiant() { return identifiant.get(); }
    public void setIdentifiant(String identifiant) { this.identifiant.set(identifiant); }

    public ObjectProperty<RoleUtilisateur> roleProperty() { return role; }
    public RoleUtilisateur getRole() { return role.get(); }
    public void setRole(RoleUtilisateur role) { this.role.set(role); }

    public ObjectProperty<LocalDateTime> dateCreationCompteProperty() { return dateCreationCompte; }
    public LocalDateTime getDateCreationCompte() { return dateCreationCompte.get(); }
    public void setDateCreationCompte(LocalDateTime dateCreationCompte) { this.dateCreationCompte.set(dateCreationCompte); }

    public ObjectProperty<LocalDateTime> dateDerniereConnexionProperty() { return dateDerniereConnexion; }
    public LocalDateTime getDateDerniereConnexion() { return dateDerniereConnexion.get(); }
    public void setDateDerniereConnexion(LocalDateTime dateDerniereConnexion) { this.dateDerniereConnexion.set(dateDerniereConnexion); }

    public ObjectProperty<StatutCompteUtilisateur> statutCompteProperty() { return statutCompte; }
    public StatutCompteUtilisateur getStatutCompte() { return statutCompte.get(); }
    public void setStatutCompte(StatutCompteUtilisateur statutCompte) { this.statutCompte.set(statutCompte); }

    public DonneesUtilisateurDTO getDonneesDTOOriginal() {
        return donneesDTOOriginal;
    }

    public void mettreAJourAvecDTO(DonneesUtilisateurDTO dto) {
        this.idUtilisateur.set(String.valueOf(dto.getIdUtilisateur()));
        this.nomComplet.set(dto.getNomComplet());
        this.identifiant.set(dto.getIdentifiant());
        this.role.set(dto.getRole());
        this.dateCreationCompte.set(dto.getDateCreationCompte());
        this.dateDerniereConnexion.set(dto.getDateDerniereConnexion());
        this.statutCompte.set(dto.getStatutCompte());
        this.donneesDTOOriginal = dto;
    }
}