package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.JSONObject;
import org.json.JSONException;

public class DonneesUtilisateurDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long idUtilisateur;
    private String identifiant;
    private String nomComplet;
    private RoleUtilisateur role;
    private StatutCompteUtilisateur statutCompte;
    private LocalDateTime dateCreationCompte;
    private LocalDateTime dateDerniereConnexion;

    public DonneesUtilisateurDTO() {
    }

    public DonneesUtilisateurDTO(long idUtilisateur, String identifiant, String nomComplet, RoleUtilisateur role, StatutCompteUtilisateur statutCompte, LocalDateTime dateCreationCompte, LocalDateTime dateDerniereConnexion) {
        this.idUtilisateur = idUtilisateur;
        this.identifiant = identifiant;
        this.nomComplet = nomComplet;
        this.role = role;
        this.statutCompte = statutCompte;
        this.dateCreationCompte = dateCreationCompte;
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public long getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(long idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
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

    public LocalDateTime getDateDerniereConnexion() {
        return dateDerniereConnexion;
    }

    public void setDateDerniereConnexion(LocalDateTime dateDerniereConnexion) {
        this.dateDerniereConnexion = dateDerniereConnexion;
    }

    public String toJsonString() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("idUtilisateur", this.idUtilisateur);
        jsonObject.put("identifiant", this.identifiant);
        jsonObject.put("nomComplet", this.nomComplet);
        if (this.role != null) {
            jsonObject.put("role", this.role.name());
        }
        if (this.statutCompte != null) {
            jsonObject.put("statutCompte", this.statutCompte.name());
        }
        if (this.dateCreationCompte != null) {
            jsonObject.put("dateCreationCompte", this.dateCreationCompte.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (this.dateDerniereConnexion != null) {
            jsonObject.put("dateDerniereConnexion", this.dateDerniereConnexion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return jsonObject.toString();
    }

    public static DonneesUtilisateurDTO fromJson(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            // Consider logging a warning or throwing an IllegalArgumentException
            // For now, returning null as per one interpretation of "gérer"
            return null;
        }
        JSONObject jsonObject = new JSONObject(jsonString);
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();

        dto.setIdUtilisateur(jsonObject.optLong("idUtilisateur"));
        dto.setIdentifiant(jsonObject.optString("identifiant", null));
        dto.setNomComplet(jsonObject.optString("nomComplet", null));

        if (jsonObject.has("role") && !jsonObject.isNull("role")) {
            try {
                dto.setRole(RoleUtilisateur.valueOf(jsonObject.getString("role")));
            } catch (IllegalArgumentException e) {
                // Log l'erreur ou assigne une valeur par défaut si nécessaire
                // e.g., EnregistreurEvenementsBMO.getLogger(DonneesUtilisateurDTO.class).warn("Rôle invalide dans JSON : {}", jsonObject.getString("role"));
            }
        }
        if (jsonObject.has("statutCompte") && !jsonObject.isNull("statutCompte")) {
            try {
                dto.setStatutCompte(StatutCompteUtilisateur.valueOf(jsonObject.getString("statutCompte")));
            } catch (IllegalArgumentException e) {
                // Log l'erreur
            }
        }
        if (jsonObject.has("dateCreationCompte") && !jsonObject.isNull("dateCreationCompte")) {
            try {
                dto.setDateCreationCompte(LocalDateTime.parse(jsonObject.getString("dateCreationCompte"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                // Log l'erreur
            }
        }
        if (jsonObject.has("dateDerniereConnexion") && !jsonObject.isNull("dateDerniereConnexion")) {
            try {
                dto.setDateDerniereConnexion(LocalDateTime.parse(jsonObject.getString("dateDerniereConnexion"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                // Log l'erreur
            }
        }
        return dto;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JSONException e) {
            return "DonneesUtilisateurDTO{" +
                    "idUtilisateur=" + idUtilisateur +
                    ", identifiant='" + identifiant + '\'' +
                    ", nomComplet='" + nomComplet + '\'' +
                    ", role=" + role +
                    ", statutCompte=" + statutCompte +
                    ", dateCreationCompte=" + dateCreationCompte +
                    ", dateDerniereConnexion=" + dateDerniereConnexion +
                    ", (Erreur JSON: " + e.getMessage() + ")" +
                    '}';
        }
    }
}