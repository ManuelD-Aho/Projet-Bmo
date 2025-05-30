package akandan.bahou.kassy.commun.dto;

import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

public class DetailsReunionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(DetailsReunionDTO.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private long idReunion;
    private String titre;
    private String description;
    private LocalDateTime dateHeureDebut;
    private int dureeEstimeeMinutes;
    private TypeReunion typeReunion;
    private StatutReunion statutReunion;
    private long idOrganisateur;
    private String nomOrganisateur;
    private List<DonneesUtilisateurDTO> participantsDTO;
    private String motDePasseOptionnelValeur;
    private LocalDateTime dateCreationReunion;
    private int nombreParticipants;

    public DetailsReunionDTO() {
        this.participantsDTO = new ArrayList<>();
        this.statutReunion = StatutReunion.PLANIFIEE;
        this.typeReunion = TypeReunion.STANDARD;
        this.dateCreationReunion = LocalDateTime.now();
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

    public List<DonneesUtilisateurDTO> getParticipantsDTO() {
        return participantsDTO;
    }

    public void setParticipantsDTO(List<DonneesUtilisateurDTO> participantsDTO) {
        this.participantsDTO = participantsDTO;
        this.nombreParticipants = (participantsDTO != null) ? participantsDTO.size() : 0;
    }

    public String getMotDePasseOptionnelValeur() {
        return motDePasseOptionnelValeur;
    }

    public void setMotDePasseOptionnelValeur(String motDePasseOptionnelValeur) {
        this.motDePasseOptionnelValeur = motDePasseOptionnelValeur;
    }

    public LocalDateTime getDateCreationReunion() {
        return dateCreationReunion;
    }

    public void setDateCreationReunion(LocalDateTime dateCreationReunion) {
        this.dateCreationReunion = dateCreationReunion;
    }

    public int getNombreParticipants() {
        return nombreParticipants;
    }

    public void setNombreParticipants(int nombreParticipants) {
        this.nombreParticipants = nombreParticipants;
    }

    public String toJsonString() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("idReunion", idReunion);
        json.put("titre", titre);
        json.put("description", description);
        if (dateHeureDebut != null) json.put("dateHeureDebut", dateHeureDebut.format(FORMATTER));
        json.put("dureeEstimeeMinutes", dureeEstimeeMinutes);
        if (typeReunion != null) json.put("typeReunion", typeReunion.name());
        if (statutReunion != null) json.put("statutReunion", statutReunion.name());
        json.put("idOrganisateur", idOrganisateur);
        json.put("nomOrganisateur", nomOrganisateur);
        if (motDePasseOptionnelValeur != null) json.put("motDePasseOptionnelValeur", motDePasseOptionnelValeur);
        if (dateCreationReunion != null) json.put("dateCreationReunion", dateCreationReunion.format(FORMATTER));
        json.put("nombreParticipants", nombreParticipants);

        if (participantsDTO != null && !participantsDTO.isEmpty()) {
            JSONArray participantsJsonArray = new JSONArray();
            for (DonneesUtilisateurDTO participant : participantsDTO) {
                participantsJsonArray.put(new JSONObject(participant.toJsonString()));
            }
            json.put("participantsDTO", participantsJsonArray);
        }
        return json.toString();
    }

    public static DetailsReunionDTO fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        DetailsReunionDTO dto = new DetailsReunionDTO();
        dto.setIdReunion(json.optLong("idReunion"));
        dto.setTitre(json.optString("titre", null));
        dto.setDescription(json.optString("description", null));
        if (json.has("dateHeureDebut")) {
            try {
                dto.setDateHeureDebut(LocalDateTime.parse(json.getString("dateHeureDebut"), FORMATTER));
            } catch (DateTimeParseException e) {
                journal.warn("Format de dateHeureDebut invalide dans JSON: {}", json.getString("dateHeureDebut"));
            }
        }
        dto.setDureeEstimeeMinutes(json.optInt("dureeEstimeeMinutes"));
        if (json.has("typeReunion")) dto.setTypeReunion(TypeReunion.valueOf(json.getString("typeReunion")));
        if (json.has("statutReunion")) dto.setStatutReunion(StatutReunion.valueOf(json.getString("statutReunion")));
        dto.setIdOrganisateur(json.optLong("idOrganisateur"));
        dto.setNomOrganisateur(json.optString("nomOrganisateur", null));
        dto.setMotDePasseOptionnelValeur(json.optString("motDePasseOptionnelValeur", null));
        if (json.has("dateCreationReunion")) {
            try {
                dto.setDateCreationReunion(LocalDateTime.parse(json.getString("dateCreationReunion"), FORMATTER));
            } catch (DateTimeParseException e) {
                journal.warn("Format de dateCreationReunion invalide dans JSON: {}", json.getString("dateCreationReunion"));
            }
        }
        dto.setNombreParticipants(json.optInt("nombreParticipants", 0));

        if (json.has("participantsDTO")) {
            JSONArray participantsJsonArray = json.getJSONArray("participantsDTO");
            List<DonneesUtilisateurDTO> participantsList = new ArrayList<>();
            for (int i = 0; i < participantsJsonArray.length(); i++) {
                participantsList.add(DonneesUtilisateurDTO.fromJson(participantsJsonArray.getJSONObject(i).toString()));
            }
            dto.setParticipantsDTO(participantsList);
        }
        return dto;
    }
}