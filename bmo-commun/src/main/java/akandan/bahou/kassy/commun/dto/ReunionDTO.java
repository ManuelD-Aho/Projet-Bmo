package akandan.bahou.kassy.commun.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet de Transfert de Données pour ReunionDTO
 */
public class ReunionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime dateCreation;

    // Constructeurs
    public ReunionDTO() {
        this.dateCreation = LocalDateTime.now();
    }

    public ReunionDTO(Long id) {
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
        return "ReunionDTO{id=" + id + ", dateCreation=" + dateCreation + "}";
    }
}
