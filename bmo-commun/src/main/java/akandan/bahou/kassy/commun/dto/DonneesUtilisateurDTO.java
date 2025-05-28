package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;

public class DonneesUtilisateurDTO {

    private int idUtilisateur;
    private String identifiantConnexion;
    private String nomComplet;
    private RoleUtilisateur roleUtilisateur;
    private StatutCompteUtilisateur statutCompte;
    private String dateDerniereConnexion;

    public DonneesUtilisateurDTO() {
    }

    public DonneesUtilisateurDTO(int idUtilisateur, String identifiantConnexion, String nomComplet, RoleUtilisateur roleUtilisateur, StatutCompteUtilisateur statutCompte) {
        this.idUtilisateur = idUtilisateur;
        this.identifiantConnexion = identifiantConnexion;
        this.nomComplet = nomComplet;
        this.roleUtilisateur = roleUtilisateur;
        this.statutCompte = statutCompte;
    }

    public DonneesUtilisateurDTO(int idUtilisateur, String identifiantConnexion, String nomComplet, RoleUtilisateur roleUtilisateur, StatutCompteUtilisateur statutCompte, String dateDerniereConnexion) {
        this.idUtilisateur = idUtilisateur;
        this.identifiantConnexion = identifiantConnexion;
        this.nomComplet = nomComplet;
        this.roleUtilisateur = roleUtilisateur;
        this.statutCompte = statutCompte;
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public int getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getIdentifiantConnexion() {
        return identifiantConnexion;
    }

    public void setIdentifiantConnexion(String identifiantConnexion) {
        this.identifiantConnexion = identifiantConnexion;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public RoleUtilisateur getRoleUtilisateur() {
        return roleUtilisateur;
    }

    public void setRoleUtilisateur(RoleUtilisateur roleUtilisateur) {
        this.roleUtilisateur = roleUtilisateur;
    }

    public StatutCompteUtilisateur getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompteUtilisateur statutCompte) {
        this.statutCompte = statutCompte;
    }

    public String getDateDerniereConnexion() {
        return dateDerniereConnexion;
    }

    public void setDateDerniereConnexion(String dateDerniereConnexion) {
        this.dateDerniereConnexion = dateDerniereConnexion;
    }
}