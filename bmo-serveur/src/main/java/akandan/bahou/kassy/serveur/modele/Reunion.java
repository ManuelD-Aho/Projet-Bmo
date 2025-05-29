package akandan.bahou.kassy.serveur.modele;

import java.time.LocalDateTime;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;

public class Reunion {

    private int id;
    private String titre;
    private String ordreDuJour;
    private LocalDateTime dateHeureDebut;
    private int dureeMinutes;
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private int organisateurId;
    private String motDePasseOptionnel;
    private LocalDateTime dateCreationReunion;

    public Reunion() {
    }

    public Reunion(String titre, String ordreDuJour, LocalDateTime dateHeureDebut, int dureeMinutes, TypeReunion typeReunion, StatutReunion statutReunion, int organisateurId, String motDePasseOptionnel, LocalDateTime dateCreationReunion) {
        this.titre = titre;
        this.ordreDuJour = ordreDuJour;
        this.dateHeureDebut = dateHeureDebut;
        this.dureeMinutes = dureeMinutes;
        this.typeReunion = typeReunion;
        this.statutReunion = statutReunion;
        this.organisateurId = organisateurId;
        this.motDePasseOptionnel = motDePasseOptionnel;
        this.dateCreationReunion = dateCreationReunion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getOrdreDuJour() {
        return ordreDuJour;
    }

    public void setOrdreDuJour(String ordreDuJour) {
        this.ordreDuJour = ordreDuJour;
    }

    public LocalDateTime getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(LocalDateTime dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(int dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public TypeReunion getTypeReunion() {
        return typeReunion;
    }

    public void setTypeReunion(TypeReunion typeReunion) {
        this.typeReunion = typeReunion;
    }

    public StatutReunion getStatutReunion() {
        return statutReunion;
    }

    public void setStatutReunion(StatutReunion statutReunion) {
        this.statutReunion = statutReunion;
    }

    public int getOrganisateurId() {
        return organisateurId;
    }

    public void setOrganisateurId(int organisateurId) {
        this.organisateurId = organisateurId;
    }

    public String getMotDePasseOptionnel() {
        return motDePasseOptionnel;
    }

    public void setMotDePasseOptionnel(String motDePasseOptionnel) {
        this.motDePasseOptionnel = motDePasseOptionnel;
    }

    public LocalDateTime getDateCreationReunion() {
        return dateCreationReunion;
    }

    public void setDateCreationReunion(LocalDateTime dateCreationReunion) {
        this.dateCreationReunion = dateCreationReunion;
    }
}