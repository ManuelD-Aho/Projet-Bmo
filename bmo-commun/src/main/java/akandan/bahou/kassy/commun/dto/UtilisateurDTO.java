package akandan.bahou.kassy.commun.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet de Transfert de Données pour UtilisateurDTO
 */
public class UtilisateurDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime dateCreation;

    // Constructeurs
    public UtilisateurDTO() {
        this.dateCreation = LocalDateTime.now();
    }

    public UtilisateurDTO(Long id) {
        this();
        this.id = id;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "UtilisateurDTO{id=" + id + ", dateCreation=" + dateCreation + "}";
    }
}
