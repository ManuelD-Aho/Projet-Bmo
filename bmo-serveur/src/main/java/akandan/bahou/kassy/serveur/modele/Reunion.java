package akandan.bahou.kassy.serveur.modele;

import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import java.time.LocalDateTime;

public class Reunion {

    private long id; // Corrigé en long
    private String titre;
    private String description; // Cohérence avec DetailsReunionDTO
    private LocalDateTime dateHeureDebut;
    private int dureeEstimeeMinutes; // Cohérence avec DetailsReunionDTO
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private long organisateurId; // Corrigé en long
    private String motDePasseOptionnel; // Le DTO a motDePasseOptionnelValeur
    private LocalDateTime dateCreationReunion;

    public Reunion() {
    }

    // Constructeur complet peut être utile pour les tests ou la création initiale par le service
    public Reunion(String titre, String description, LocalDateTime dateHeureDebut, int dureeEstimeeMinutes, TypeReunion typeReunion, StatutReunion statutReunion, long organisateurId, String motDePasseOptionnel, LocalDateTime dateCreationReunion) {
        this.titre = titre;
        this.description = description;
        this.dateHeureDebut = dateHeureDebut;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
        this.typeReunion = typeReunion;
        this.statutReunion = statutReunion;
        this.organisateurId = organisateurId;
        this.motDePasseOptionnel = motDePasseOptionnel;
        this.dateCreationReunion = dateCreationReunion;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(LocalDateTime dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public int getDureeEstimeeMinutes() {
        return dureeEstimeeMinutes;
    }

    public void setDureeEstimeeMinutes(int dureeEstimeeMinutes) {
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
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

    public long getOrganisateurId() {
        return organisateurId;
    }

    public void setOrganisateurId(long organisateurId) {
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