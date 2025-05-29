package akandan.bahou.kassy.client.modele;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;

public class ModeleObservableUtilisateur {

    private final StringProperty idUtilisateur;
    private final StringProperty nomComplet;
    private final StringProperty identifiant; // Corrigé: était identifiantConnexion
    private final ObjectProperty<RoleUtilisateur> role; // Corrigé: était roleUtilisateur
    private final ObjectProperty<StatutCompteUtilisateur> statutCompte;
    private final StringProperty dateCreationCompteAffichage;
    private final StringProperty dateDerniereConnexionAffichage;

    private DonneesUtilisateurDTO donneesDTOOriginal;

    private static final DateTimeFormatter FORMATEUR_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ModeleObservableUtilisateur(DonneesUtilisateurDTO dto) {
        this.idUtilisateur = new SimpleStringProperty(String.valueOf(dto.getIdUtilisateur()));
        this.nomComplet = new SimpleStringProperty(dto.getNomComplet());
        this.identifiant = new SimpleStringProperty(dto.getIdentifiant()); // Utilise getIdentifiant() du DTO
        this.role = new SimpleObjectProperty<>(dto.getRole()); // Utilise getRole() du DTO
        this.statutCompte = new SimpleObjectProperty<>(dto.getStatutCompte());
        this.dateCreationCompteAffichage = new SimpleStringProperty(dto.getDateCreationCompte() != null ? dto.getDateCreationCompte().format(FORMATEUR_DATE_HEURE) : "");
        this.dateDerniereConnexionAffichage = new SimpleStringProperty(dto.getDateDerniereConnexion() != null ? dto.getDateDerniereConnexion().format(FORMATEUR_DATE_HEURE) : "");
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

    public ObjectProperty<StatutCompteUtilisateur> statutCompteProperty() { return statutCompte; }
    public StatutCompteUtilisateur getStatutCompte() { return statutCompte.get(); }
    public void setStatutCompte(StatutCompteUtilisateur statutCompte) { this.statutCompte.set(statutCompte); }

    public StringProperty dateCreationCompteAffichageProperty() { return dateCreationCompteAffichage; }
    public String getDateCreationCompteAffichage() { return dateCreationCompteAffichage.get(); }
    // Pas de setter pour les affichages formatés, ils sont dérivés

    public StringProperty dateDerniereConnexionAffichageProperty() { return dateDerniereConnexionAffichage; }
    public String getDateDerniereConnexionAffichage() { return dateDerniereConnexionAffichage.get(); }
    // Pas de setter

    public DonneesUtilisateurDTO getDonneesDTOOriginal() {
        return donneesDTOOriginal;
    }

    public void mettreAJourAvecDTO(DonneesUtilisateurDTO dto) {
        this.idUtilisateur.set(String.valueOf(dto.getIdUtilisateur()));
        this.nomComplet.set(dto.getNomComplet());
        this.identifiant.set(dto.getIdentifiant());
        this.role.set(dto.getRole());
        this.statutCompte.set(dto.getStatutCompte());
        this.dateCreationCompteAffichage.set(dto.getDateCreationCompte() != null ? dto.getDateCreationCompte().format(FORMATEUR_DATE_HEURE) : "");
        this.dateDerniereConnexionAffichage.set(dto.getDateDerniereConnexion() != null ? dto.getDateDerniereConnexion().format(FORMATEUR_DATE_HEURE) : "");
        this.donneesDTOOriginal = dto;
    }
}