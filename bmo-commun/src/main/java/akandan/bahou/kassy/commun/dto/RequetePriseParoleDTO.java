package akandan.bahou.kassy.commun.dto;

public class RequetePriseParoleDTO {

    private int idReunion;
    private int idUtilisateurDemandeur;
    private String nomUtilisateurDemandeur;
    private String actionRequise;

    public RequetePriseParoleDTO() {
    }

    public RequetePriseParoleDTO(int idReunion, int idUtilisateurDemandeur, String nomUtilisateurDemandeur, String actionRequise) {
        this.idReunion = idReunion;
        this.idUtilisateurDemandeur = idUtilisateurDemandeur;
        this.nomUtilisateurDemandeur = nomUtilisateurDemandeur;
        this.actionRequise = actionRequise;
    }

    public int getIdReunion() {
        return idReunion;
    }

    public void setIdReunion(int idReunion) {
        this.idReunion = idReunion;
    }

    public int getIdUtilisateurDemandeur() {
        return idUtilisateurDemandeur;
    }

    public void setIdUtilisateurDemandeur(int idUtilisateurDemandeur) {
        this.idUtilisateurDemandeur = idUtilisateurDemandeur;
    }

    public String getNomUtilisateurDemandeur() {
        return nomUtilisateurDemandeur;
    }

    public void setNomUtilisateurDemandeur(String nomUtilisateurDemandeur) {
        this.nomUtilisateurDemandeur = nomUtilisateurDemandeur;
    }

    public String getActionRequise() {
        return actionRequise;
    }

    public void setActionRequise(String actionRequise) {
        this.actionRequise = actionRequise;
    }
}