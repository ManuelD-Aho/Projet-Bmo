package akandan.bahou.kassy.serveur.modele;

import java.time.LocalDateTime;

public class MessageChat {

    private long idMessage;
    private long idReunion; // Corrigé en long pour cohérence potentielle
    private long idUtilisateurEmetteur; // Corrigé en long
    private String contenuMessage;
    private LocalDateTime horodatage;

    public MessageChat() {
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

    public long getIdUtilisateurEmetteur() {
        return idUtilisateurEmetteur;
    }

    public void setIdUtilisateurEmetteur(long idUtilisateurEmetteur) {
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