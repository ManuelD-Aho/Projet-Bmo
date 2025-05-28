package akandan.bahou.kassy.commun.dto;

public class ReponseGeneriqueDTO {

    private boolean succes;
    private String message;
    private String codeErreur;

    public ReponseGeneriqueDTO() {
        this.succes = false;
        this.message = "";
    }

    public ReponseGeneriqueDTO(boolean succes, String message) {
        this.succes = succes;
        this.message = message;
    }

    public ReponseGeneriqueDTO(boolean succes, String message, String codeErreur) {
        this.succes = succes;
        this.message = message;
        this.codeErreur = codeErreur;
    }

    public boolean isSucces() {
        return succes;
    }

    public void setSucces(boolean succes) {
        this.succes = succes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCodeErreur() {
        return codeErreur;
    }

    public void setCodeErreur(String codeErreur) {
        this.codeErreur = codeErreur;
    }
}