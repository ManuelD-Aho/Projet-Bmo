package akandan.bahou.kassy.serveur.persistance.entites;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA EntiteUtilisateur
 */
@Entity
@Table(name = "entiteutilisateur")
public class EntiteUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    public EntiteUtilisateur() {
        this.dateCreation = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
