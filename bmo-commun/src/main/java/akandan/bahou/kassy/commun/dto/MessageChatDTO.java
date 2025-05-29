package akandan.bahou.kassy.commun.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.json.JSONObject;
import org.json.JSONException;

public class MessageChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long idMessage;
    private long idReunion;
    private long idExpediteur;
    private String nomExpediteur;
    private String contenu;
    private LocalDateTime horodatage;

    public MessageChatDTO() {
    }

    public MessageChatDTO(long idReunion, long idExpediteur, String nomExpediteur, String contenu, LocalDateTime horodatage) {
        this.idReunion = idReunion;
        this.idExpediteur = idExpediteur;
        this.nomExpediteur = nomExpediteur;
        this.contenu = contenu;
        this.horodatage = horodatage;
    }

    public MessageChatDTO(long idMessage, long idReunion, long idExpediteur, String nomExpediteur, String contenu, LocalDateTime horodatage) {
        this(idReunion, idExpediteur, nomExpediteur, contenu, horodatage);
        this.idMessage = idMessage;
    }


    public long getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(long idMessage) {
        this.idMessage = idMessage;
    }

    public long getIdReunion() {
        return idReunion;
    }

    public void setIdReunion(long idReunion) {
        this.idReunion = idReunion;
    }

    public long getIdExpediteur() {
        return idExpediteur;
    }

    public void setIdExpediteur(long idExpediteur) {
        this.idExpediteur = idExpediteur;
    }

    public String getNomExpediteur() {
        return nomExpediteur;
    }

    public void setNomExpediteur(String nomExpediteur) {
        this.nomExpediteur = nomExpediteur;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(LocalDateTime horodatage) {
        this.horodatage = horodatage;
    }

    public String toJsonString() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("idMessage", this.idMessage);
        jsonObject.put("idReunion", this.idReunion);
        jsonObject.put("idExpediteur", this.idExpediteur);
        jsonObject.put("nomExpediteur", this.nomExpediteur);
        jsonObject.put("contenu", this.contenu);
        if (this.horodatage != null) {
            jsonObject.put("horodatage", this.horodatage.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return jsonObject.toString();
    }

    public static MessageChatDTO fromJson(String jsonString) throws JSONException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(jsonString);
        MessageChatDTO dto = new MessageChatDTO();

        dto.setIdMessage(jsonObject.optLong("idMessage"));
        dto.setIdReunion(jsonObject.optLong("idReunion"));
        dto.setIdExpediteur(jsonObject.optLong("idExpediteur"));
        dto.setNomExpediteur(jsonObject.optString("nomExpediteur", null));
        dto.setContenu(jsonObject.optString("contenu", null));

        if (jsonObject.has("horodatage") && !jsonObject.isNull("horodatage")) {
            try {
                dto.setHorodatage(LocalDateTime.parse(jsonObject.getString("horodatage"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
            return "MessageChatDTO{" +
                    "idMessage=" + idMessage +
                    ", idReunion=" + idReunion +
                    ", idExpediteur=" + idExpediteur +
                    ", nomExpediteur='" + nomExpediteur + '\'' +
                    ", contenu='" + contenu + '\'' +
                    ", horodatage=" + horodatage +
                    ", (Erreur JSON: " + e.getMessage() + ")" +
                    '}';
        }
    }
}