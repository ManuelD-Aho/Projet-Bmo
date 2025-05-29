package akandan.bahou.kassy.serveur.modele;

import java.time.LocalDateTime;

public class MessageChat {

    private long idMessage;
    private int idReunion;
    private int idUtilisateurEmetteur;
    private String contenuMessage;
    private LocalDateTime horodatage;

    public MessageChat() {
    }

    public MessageChat(int idReunion, int idUtilisateurEmetteur, String contenuMessage, LocalDateTime horodatage) {
        this.idReunion = idReunion;
        this.idUtilisateurEmetteur = idUtilisateurEmetteur;
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

    public String getContenuMessage() {
        return contenuMessage;
    }

    public void setContenuMessage(String contenuMessage) {
        this.contenuMessage = contenuMessage;
    }

    public LocalDateTime getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(LocalDateTime horodatage) {
        this.horodatage = horodatage;
    }
}