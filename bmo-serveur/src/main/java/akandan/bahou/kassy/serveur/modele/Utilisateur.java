package akandan.bahou.kassy.serveur.modele;

import java.time.LocalDateTime;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;

public class Utilisateur {

    private int id;
    private String identifiantConnexion;
    private String motDePasseHache;
    private String selMotDePasse;
    private String nomComplet;
    private RoleUtilisateur roleSysteme;
    private StatutCompteUtilisateur statutCompte;
    private LocalDateTime dateCreationCompte;
    private LocalDateTime derniereConnexion;
    private byte[] photo;
    private String photoMimeType;


    public Utilisateur() {
    }

    public Utilisateur(String identifiantConnexion, String motDePasseHache, String selMotDePasse, String nomComplet, RoleUtilisateur roleSysteme, StatutCompteUtilisateur statutCompte, LocalDateTime dateCreationCompte) {
        this.identifiantConnexion = identifiantConnexion;
        this.motDePasseHache = motDePasseHache;
        this.selMotDePasse = selMotDePasse;
        this.nomComplet = nomComplet;
        this.roleSysteme = roleSysteme;
        this.statutCompte = statutCompte;
        this.dateCreationCompte = dateCreationCompte;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifiantConnexion() {
        return identifiantConnexion;
    }

    public void setIdentifiantConnexion(String identifiantConnexion) {
        this.identifiantConnexion = identifiantConnexion;
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

    public RoleUtilisateur getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(RoleUtilisateur roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public StatutCompteUtilisateur getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompteUtilisateur statutCompte) {
        this.statutCompte = statutCompte;
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