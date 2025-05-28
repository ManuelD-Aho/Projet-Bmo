package akandan.bahou.kassy.commun.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet de Transfert de Données pour ReponseServeurDTO
 */
public class ReponseServeurDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime dateCreation;

    // Constructeurs
    public ReponseServeurDTO() {
        this.dateCreation = LocalDateTime.now();
    }

    public ReponseServeurDTO(Long id) {
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
        return "ReponseServeurDTO{id=" + id + ", dateCreation=" + dateCreation + "}";
    }
}
