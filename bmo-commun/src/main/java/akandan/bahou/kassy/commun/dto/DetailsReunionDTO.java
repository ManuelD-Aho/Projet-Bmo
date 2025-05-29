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
    private String motDePasseOptionnelValeur;
    private List<DonneesUtilisateurDTO> participantsDTO;
    private int nombreParticipants; // Maintenu par la taille de participantsDTO
    private LocalDateTime dateCreationReunion;


    public DetailsReunionDTO() {
        this.participantsDTO = new ArrayList<>();
        this.nombreParticipants = 0;
    }

    public DetailsReunionDTO(long idReunion, String titre, String description, LocalDateTime dateHeureDebut, int dureeEstimeeMinutes, TypeReunion typeReunion, StatutReunion statutReunion, long idOrganisateur, String nomOrganisateur, String motDePasseOptionnelValeur, List<DonneesUtilisateurDTO> participantsDTO, LocalDateTime dateCreationReunion) {
        this.idReunion = idReunion;
        this.titre = titre;
        this.description = description;
        this.dateHeureDebut = dateHeureDebut;
        this.dureeEstimeeMinutes = dureeEstimeeMinutes;
        this.typeReunion = typeReunion;
        this.statutReunion = statutReunion;
        this.idOrganisateur = idOrganisateur;
        this.nomOrganisateur = nomOrganisateur;
        this.motDePasseOptionnelValeur = motDePasseOptionnelValeur;
        this.participantsDTO = participantsDTO != null ? participantsDTO : new ArrayList<>();
        this.nombreParticipants = this.participantsDTO.size();
        this.dateCreationReunion = dateCreationReunion;
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

    public String getMotDePasseOptionnelValeur() {
        return motDePasseOptionnelValeur;
    }

    public void setMotDePasseOptionnelValeur(String motDePasseOptionnelValeur) {
        this.motDePasseOptionnelValeur = motDePasseOptionnelValeur;
    }

    public List<DonneesUtilisateurDTO> getParticipantsDTO() {
        return participantsDTO;
    }

    public void setParticipantsDTO(List<DonneesUtilisateurDTO> participantsDTO) {
        this.participantsDTO = participantsDTO != null ? participantsDTO : new ArrayList<>();
        this.nombreParticipants = this.participantsDTO.size();
    }

    public int getNombreParticipants() {
        return nombreParticipants; // Dérivé de la taille de la liste
    }

    // Pas de setter direct pour nombreParticipants s'il est toujours dérivé
    // public void setNombreParticipants(int nombreParticipants) { this.nombreParticipants = nombreParticipants; }


    public LocalDateTime getDateCreationReunion() {
        return dateCreationReunion;
    }

    public void setDateCreationReunion(LocalDateTime dateCreationReunion) {
        this.dateCreationReunion = dateCreationReunion;
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
        jsonObject.put("motDePasseOptionnelValeur", this.motDePasseOptionnelValeur);

        if (this.participantsDTO != null) {
            JSONArray participantsArray = new JSONArray();
            for (DonneesUtilisateurDTO participant : this.participantsDTO) {
                if (participant != null) {
                    participantsArray.put(new JSONObject(participant.toJsonString())); // Sérialiser chaque DTO participant
                }
            }
            jsonObject.put("participantsDTO", participantsArray);
        }
        jsonObject.put("nombreParticipants", this.nombreParticipants);
        if (this.dateCreationReunion != null) {
            jsonObject.put("dateCreationReunion", this.dateCreationReunion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
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
            } catch (DateTimeParseException e) { /* Log error */ }
        }
        dto.setDureeEstimeeMinutes(jsonObject.optInt("dureeEstimeeMinutes"));

        if (jsonObject.has("typeReunion") && !jsonObject.isNull("typeReunion")) {
            try {
                dto.setTypeReunion(TypeReunion.valueOf(jsonObject.getString("typeReunion")));
            } catch (IllegalArgumentException e) { /* Log error */ }
        }
        if (jsonObject.has("statutReunion") && !jsonObject.isNull("statutReunion")) {
            try {
                dto.setStatutReunion(StatutReunion.valueOf(jsonObject.getString("statutReunion")));
            } catch (IllegalArgumentException e) { /* Log error */ }
        }
        dto.setIdOrganisateur(jsonObject.optLong("idOrganisateur"));
        dto.setNomOrganisateur(jsonObject.optString("nomOrganisateur", null));
        dto.setMotDePasseOptionnelValeur(jsonObject.optString("motDePasseOptionnelValeur", null));

        if (jsonObject.has("participantsDTO") && !jsonObject.isNull("participantsDTO")) {
            JSONArray participantsArray = jsonObject.getJSONArray("participantsDTO");
            List<DonneesUtilisateurDTO> participants = new ArrayList<>();
            for (int i = 0; i < participantsArray.length(); i++) {
                JSONObject participantJson = participantsArray.optJSONObject(i);
                if (participantJson != null) {
                    participants.add(DonneesUtilisateurDTO.fromJson(participantJson.toString()));
                }
            }
            dto.setParticipantsDTO(participants);
        }
        dto.nombreParticipants = dto.getParticipantsDTO().size(); // Assurer la cohérence

        if (jsonObject.has("dateCreationReunion") && !jsonObject.isNull("dateCreationReunion")) {
            try {
                dto.setDateCreationReunion(LocalDateTime.parse(jsonObject.getString("dateCreationReunion"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) { /* Log error */ }
        }
        return dto;
    }

    @Override
    public String toString() {
        try {
            return toJsonString();
        } catch (JSONException e) {
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
                    ", nombreParticipants=" + getNombreParticipants() +
                    ", (Erreur JSON: " + e.getMessage() + ")" +
                    '}';
        }
    }
}
