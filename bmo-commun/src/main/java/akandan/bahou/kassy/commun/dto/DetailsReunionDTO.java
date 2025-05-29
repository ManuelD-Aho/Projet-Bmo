package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class DetailsReunionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long idReunion;
    private String titre;
    private String description;
    private LocalDateTime dateHeureDebut;
    private int dureeEstimeeMinutes;
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private long idOrganisateur;
    private String nomOrganisateur;
    private List<Long> idsParticipants;
    private int nombreParticipants;

    public DetailsReunionDTO() {
        this.idsParticipants = new ArrayList<>();
    }

    public DetailsReunionDTO(long idReunion, String titre, String description, LocalDateTime dateHeureDebut, int dureeEstimeeMinutes, TypeReunion typeReunion, StatutReunion statutReunion, long idOrganisateur, String nomOrganisateur, List<Long> idsParticipants) {
        this.idReunion = idReunion;
        this.titre = titre;
        this.description = description;
        this.dateHeureDebut = dateHeureDebut;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
        this.typeReunion = typeReunion;
        this.statutReunion = statutReunion;
        this.idOrganisateur = idOrganisateur;
        this.nomOrganisateur = nomOrganisateur;
        this.idsParticipants = idsParticipants != null ? idsParticipants : new ArrayList<>();
        this.nombreParticipants = this.idsParticipants.size();
    }


    public long getIdReunion() {
        return idReunion;
    }

    public void setIdReunion(long idReunion) {
        this.idReunion = idReunion;
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

    public long getIdOrganisateur() {
        return idOrganisateur;
    }

    public void setIdOrganisateur(long idOrganisateur) {
        this.idOrganisateur = idOrganisateur;
    }

    public String getNomOrganisateur() {
        return nomOrganisateur;
    }

    public void setNomOrganisateur(String nomOrganisateur) {
        this.nomOrganisateur = nomOrganisateur;
    }

    public List<Long> getIdsParticipants() {
        return idsParticipants;
    }

    public void setIdsParticipants(List<Long> idsParticipants) {
        this.idsParticipants = idsParticipants != null ? idsParticipants : new ArrayList<>();
        this.nombreParticipants = this.idsParticipants.size();
    }

    public int getNombreParticipants() {
        // Recalculer au cas où la liste a été modifiée directement (bien que non recommandé)
        if (this.idsParticipants != null) {
            return this.idsParticipants.size();
        }
        return nombreParticipants; // Retourner la valeur stockée si la liste est nulle
    }

    public void setNombreParticipants(int nombreParticipants) {
        // Ce setter est moins utile si nombreParticipants est toujours dérivé de idsParticipants.size()
        // Mais peut être utilisé si le nombre est fourni explicitement par le serveur.
        this.nombreParticipants = nombreParticipants;
    }


    public String toJsonString() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("idReunion", this.idReunion);
        jsonObject.put("titre", this.titre);
        jsonObject.put("description", this.description);
        if (this.dateHeureDebut != null) {
            jsonObject.put("dateHeureDebut", this.dateHeureDebut.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        jsonObject.put("dureeEstimeeMinutes", this.dureeEstimeeMinutes);
        if (this.typeReunion != null) {
            jsonObject.put("typeReunion", this.typeReunion.name());
        }
        if (this.statutReunion != null) {
            jsonObject.put("statutReunion", this.statutReunion.name());
        }
        jsonObject.put("idOrganisateur", this.idOrganisateur);
        jsonObject.put("nomOrganisateur", this.nomOrganisateur);

        if (this.idsParticipants != null) {
            JSONArray participantsArray = new JSONArray();
            for (Long id : this.idsParticipants) {
                participantsArray.put(id);
            }
            jsonObject.put("idsParticipants", participantsArray);
        }
        jsonObject.put("nombreParticipants", getNombreParticipants()); // Utiliser le getter pour la cohérence
        return jsonObject.toString();
    }

    public static DetailsReunionDTO fromJson(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(jsonString);
        DetailsReunionDTO dto = new DetailsReunionDTO();

        dto.setIdReunion(jsonObject.optLong("idReunion"));
        dto.setTitre(jsonObject.optString("titre", null));
        dto.setDescription(jsonObject.optString("description", null));

        if (jsonObject.has("dateHeureDebut") && !jsonObject.isNull("dateHeureDebut")) {
            try {
                dto.setDateHeureDebut(LocalDateTime.parse(jsonObject.getString("dateHeureDebut"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                // Log l'erreur
            }
        }
        dto.setDureeEstimeeMinutes(jsonObject.optInt("dureeEstimeeMinutes"));

        if (jsonObject.has("typeReunion") && !jsonObject.isNull("typeReunion")) {
            try {
                dto.setTypeReunion(TypeReunion.valueOf(jsonObject.getString("typeReunion")));
            } catch (IllegalArgumentException e) {
                // Log l'erreur
            }
        }
        if (jsonObject.has("statutReunion") && !jsonObject.isNull("statutReunion")) {
            try {
                dto.setStatutReunion(StatutReunion.valueOf(jsonObject.getString("statutReunion")));
            } catch (IllegalArgumentException e) {
                // Log l'erreur
            }
        }
        dto.setIdOrganisateur(jsonObject.optLong("idOrganisateur"));
        dto.setNomOrganisateur(jsonObject.optString("nomOrganisateur", null));

        if (jsonObject.has("idsParticipants") && !jsonObject.isNull("idsParticipants")) {
            JSONArray participantsArray = jsonObject.getJSONArray("idsParticipants");
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < participantsArray.length(); i++) {
                ids.add(participantsArray.getLong(i));
            }
            dto.setIdsParticipants(ids);
        }
        // Le nombre de participants est recalculé par setIdsParticipants,
        // mais on peut aussi le prendre du JSON s'il est explicitement envoyé.
        if (jsonObject.has("nombreParticipants")) {
            dto.setNombreParticipants(jsonObject.optInt("nombreParticipants", dto.getNombreParticipants()));
        }


        return dto;
    }

    @Override
    public String toString() {
        return "DetailsReunionDTO{" +
                "idReunion=" + idReunion +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateHeureDebut=" + dateHeureDebut +
                ", dureeEstimeeMinutes=" + dureeEstimeeMinutes +
                ", typeReunion=" + typeReunion +
                ", statutReunion=" + statutReunion +
                ", idOrganisateur=" + idOrganisateur +
                ", nomOrganisateur='" + nomOrganisateur + '\'' +
                ", idsParticipants=" + idsParticipants +
                ", nombreParticipants=" + getNombreParticipants() +
                '}';
    }
}