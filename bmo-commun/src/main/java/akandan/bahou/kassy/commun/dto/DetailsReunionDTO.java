package akandan.bahou.kassy.commun.dto;

import java.util.List;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;

public class DetailsReunionDTO {

    private int id;
    private String titre;
    private String ordreDuJour;
    private String dateHeureDebut;
    private int dureeMinutes;
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private int organisateurId;
    private String nomOrganisateur;
    private String motDePasseOptionnel;
    private List<DonneesUtilisateurDTO> participants;
    private String dateCreationReunion;

    public DetailsReunionDTO() {
    }

    public DetailsReunionDTO(int id, String titre, String ordreDuJour, String dateHeureDebut, int dureeMinutes, TypeReunion typeReunion, StatutReunion statutReunion, int organisateurId, String nomOrganisateur, String motDePasseOptionnel, List<DonneesUtilisateurDTO> participants, String dateCreationReunion) {
        this.id = id;
        this.titre = titre;
        this.ordreDuJour = ordreDuJour;
        this.dateHeureDebut = dateHeureDebut;
        this.dureeMinutes = dureeMinutes;
        this.typeReunion = typeReunion;
        this.statutReunion = statutReunion;
        this.organisateurId = organisateurId;
        this.nomOrganisateur = nomOrganisateur;
        this.motDePasseOptionnel = motDePasseOptionnel;
        this.participants = participants;
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

    public String getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(String dateHeureDebut) {
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

    public String getNomOrganisateur() {
        return nomOrganisateur;
    }

    public void setNomOrganisateur(String nomOrganisateur) {
        this.nomOrganisateur = nomOrganisateur;
    }

    public String getMotDePasseOptionnel() {
        return motDePasseOptionnel;
    }

    public void setMotDePasseOptionnel(String motDePasseOptionnel) {
        this.motDePasseOptionnel = motDePasseOptionnel;
    }

    public List<DonneesUtilisateurDTO> getParticipants() {
        return participants;
    }

    public void setParticipants(List<DonneesUtilisateurDTO> participants) {
        this.participants = participants;
    }

    public String getDateCreationReunion() {
        return dateCreationReunion;
    }

    public void setDateCreationReunion(String dateCreationReunion) {
        this.dateCreationReunion = dateCreationReunion;
    }
}