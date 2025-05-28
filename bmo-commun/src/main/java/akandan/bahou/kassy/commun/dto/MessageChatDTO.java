package akandan.bahou.kassy.commun.dto;

public class MessageChatDTO {

    private long idMessage;
    private int idReunion;
    private int idUtilisateurEmetteur;
    private String nomUtilisateurEmetteur;
    private String contenuMessage;
    private String horodatage;

    public MessageChatDTO() {
    }

    public MessageChatDTO(int idReunion, int idUtilisateurEmetteur, String nomUtilisateurEmetteur, String contenuMessage) {
        this.idReunion = idReunion;
        this.idUtilisateurEmetteur = idUtilisateurEmetteur;
        this.nomUtilisateurEmetteur = nomUtilisateurEmetteur;
        this.contenuMessage = contenuMessage;
    }

    public MessageChatDTO(long idMessage, int idReunion, int idUtilisateurEmetteur, String nomUtilisateurEmetteur, String contenuMessage, String horodatage) {
        this.idMessage = idMessage;
        this.idReunion = idReunion;
        this.idUtilisateurEmetteur = idUtilisateurEmetteur;
        this.nomUtilisateurEmetteur = nomUtilisateurEmetteur;
        this.contenuMessage = contenuMessage;
        this.horodatage = horodatage;
    }

    public long getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(long idMessage) {
        this.idMessage = idMessage;
    }

    public int getIdReunion() {
        return idReunion;
    }

    public void setIdReunion(int idReunion) {
        this.idReunion = idReunion;
    }

    public int getIdUtilisateurEmetteur() {
        return idUtilisateurEmetteur;
    }

    public void setIdUtilisateurEmetteur(int idUtilisateurEmetteur) {
        this.idUtilisateurEmetteur = idUtilisateurEmetteur;
    }

    public String getNomUtilisateurEmetteur() {
        return nomUtilisateurEmetteur;
    }

    public void setNomUtilisateurEmetteur(String nomUtilisateurEmetteur) {
        this.nomUtilisateurEmetteur = nomUtilisateurEmetteur;
    }

    public String getContenuMessage() {
        return contenuMessage;
    }

    public void setContenuMessage(String contenuMessage) {
        this.contenuMessage = contenuMessage;
    }

    public String getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(String horodatage) {
        this.horodatage = horodatage;
    }
}