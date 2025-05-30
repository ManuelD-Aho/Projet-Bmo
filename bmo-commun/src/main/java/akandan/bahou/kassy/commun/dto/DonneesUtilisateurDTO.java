package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

public class DonneesUtilisateurDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(DonneesUtilisateurDTO.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private long idUtilisateur;
    private String nomComplet;
    private String identifiant;
    private RoleUtilisateur role;
    private StatutCompteUtilisateur statutCompte;
    private LocalDateTime dateCreationCompte;
    private LocalDateTime dateDerniereConnexion;
    private String motDePasse; // Principalement pour la création/modification, ne devrait pas être sérialisé vers le client en général

    public DonneesUtilisateurDTO() {
    }

    public DonneesUtilisateurDTO(long idUtilisateur, String nomComplet, String identifiant, RoleUtilisateur role, StatutCompteUtilisateur statutCompte, LocalDateTime dateCreation, LocalDateTime dateDerniereConnexion) {
        this.idUtilisateur = idUtilisateur;
        this.nomComplet = nomComplet;
        this.identifiant = identifiant;
        this.role = role;
        this.statutCompte = statutCompte;
        this.dateCreationCompte = dateCreation;
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public long getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(long idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
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

    public LocalDateTime getDateDerniereConnexion() {
        return dateDerniereConnexion;
    }

    public void setDateDerniereConnexion(LocalDateTime dateDerniereConnexion) {
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String toJsonString() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("idUtilisateur", idUtilisateur);
        json.put("nomComplet", nomComplet);
        json.put("identifiant", identifiant);
        if (role != null) json.put("role", role.name());
        if (statutCompte != null) json.put("statutCompte", statutCompte.name());
        if (dateCreationCompte != null) json.put("dateCreationCompte", dateCreationCompte.format(FORMATTER));
        if (dateDerniereConnexion != null) json.put("dateDerniereConnexion", dateDerniereConnexion.format(FORMATTER));
        return json.toString();
    }

    public static DonneesUtilisateurDTO fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdUtilisateur(json.optLong("idUtilisateur"));
        dto.setNomComplet(json.optString("nomComplet", null));
        dto.setIdentifiant(json.optString("identifiant", null));
        if (json.has("role")) dto.setRole(RoleUtilisateur.valueOf(json.getString("role")));
        if (json.has("statutCompte")) dto.setStatutCompte(StatutCompteUtilisateur.valueOf(json.getString("statutCompte")));
        if (json.has("dateCreationCompte")) {
            try {
                dto.setDateCreationCompte(LocalDateTime.parse(json.getString("dateCreationCompte"), FORMATTER));
            } catch (DateTimeParseException e) {
                journal.warn("Format de dateCreationCompte invalide dans JSON: {}", json.getString("dateCreationCompte"));
            }
        }
        if (json.has("dateDerniereConnexion")) {
            try {
                dto.setDateDerniereConnexion(LocalDateTime.parse(json.getString("dateDerniereConnexion"), FORMATTER));
            } catch (DateTimeParseException e) {
                journal.warn("Format de dateDerniereConnexion invalide dans JSON: {}", json.getString("dateDerniereConnexion"));
            }
        }
        return dto;
    }
}