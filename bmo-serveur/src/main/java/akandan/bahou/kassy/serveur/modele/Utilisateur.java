package akandan.bahou.kassy.serveur.modele;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import java.time.LocalDateTime;

public class Utilisateur {

    private long id; // Corrigé en long
    private String identifiant; // Corrigé depuis identifiantConnexion
    private String motDePasseHache;
    private String selMotDePasse;
    private String nomComplet;
    private RoleUtilisateur role; // Corrigé depuis roleSysteme
    private StatutCompteUtilisateur statutCompte;
    private LocalDateTime dateCreationCompte;
    private LocalDateTime derniereConnexion;
    private byte[] photo; // Conservé si utilisé, sinon peut être omis
    private String photoMimeType; // Conservé si utilisé

    public Utilisateur() {
    }

    public Utilisateur(String identifiant, String motDePasseHache, String selMotDePasse, String nomComplet, RoleUtilisateur role, StatutCompteUtilisateur statutCompte, LocalDateTime dateCreationCompte) {
        this.identifiant = identifiant;
        this.motDePasseHache = motDePasseHache;
        this.selMotDePasse = selMotDePasse;
        this.nomComplet = nomComplet;
        this.role = role;
        this.statutCompte = statutCompte;
        this.dateCreationCompte = dateCreationCompte;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getMotDePasseHache() {
        return motDePasseHache;
    }

    public void setMotDePasseHache(String motDePasseHache) {
        this.motDePasseHache = motDePasseHache;
    }

    public String getSelMotDePasse() {
        return selMotDePasse;
    }

    public void setSelMotDePasse(String selMotDePasse) {
        this.selMotDePasse = selMotDePasse;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    public void setRole(RoleUtilisateur role) {
        this.role = role;
    }

    public StatutCompteUtilisateur getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompteUtilisateur statutCompte) {
        this.statutCompte = statutCompte;
    }

    public LocalDateTime getDateCreationCompte() {
        return dateCreationCompte;
    }

    public void setDateCreationCompte(LocalDateTime dateCreationCompte) {
        this.dateCreationCompte = dateCreationCompte;
    }

    public LocalDateTime getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoMimeType() {
        return photoMimeType;
    }

    public void setPhotoMimeType(String photoMimeType) {
        this.photoMimeType = photoMimeType;
    }
}