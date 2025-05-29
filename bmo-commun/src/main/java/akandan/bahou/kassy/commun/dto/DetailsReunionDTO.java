package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import java.util.List;

public class DetailsReunionDTO {

    private int idReunion;
    private String titre;
    private String ordreDuJour;
    private String dateHeureDebut;
    private int dureeMinutes;
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private int idOrganisateur;
    private String nomOrganisateur;
    private boolean motDePasseOptionnelPresent;
    private String motDePasseOptionnelValeur;


    private List<DonneesUtilisateurDTO> participants;

    public DetailsReunionDTO() {
    }

    public int getIdReunion() {
        return idReunion;
    }

    public void setIdReunion(int idReunion) {
        this.idReunion = idReunion;
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

    public int getIdOrganisateur() {
        return idOrganisateur;
    }

    public void setIdOrganisateur(int idOrganisateur) {
        this.idOrganisateur = idOrganisateur;
    }

    public String getNomOrganisateur() {
        return nomOrganisateur;
    }

    public void setNomOrganisateur(String nomOrganisateur) {
        this.nomOrganisateur = nomOrganisateur;
    }

    public boolean isMotDePasseOptionnelPresent() {
        return motDePasseOptionnelPresent;
    }

    public void setMotDePasseOptionnelPresent(boolean motDePasseOptionnelPresent) {
        this.motDePasseOptionnelPresent = motDePasseOptionnelPresent;
    }

    public String getMotDePasseOptionnelValeur() {
        return motDePasseOptionnelValeur;
    }

    public void setMotDePasseOptionnelValeur(String motDePasseOptionnelValeur) {
        this.motDePasseOptionnelValeur = motDePasseOptionnelValeur;
        this.motDePasseOptionnelPresent = (motDePasseOptionnelValeur != null && !motDePasseOptionnelValeur.isEmpty());
    }


    public List<DonneesUtilisateurDTO> getParticipants() {
        return participants;
    }

    public void setParticipants(List<DonneesUtilisateurDTO> participants) {
        this.participants = participants;
    }
}