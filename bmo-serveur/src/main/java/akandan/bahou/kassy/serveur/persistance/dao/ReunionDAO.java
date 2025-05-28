package akandan.bahou.kassy.serveur.persistance.dao;

import akandan.bahou.kassy.serveur.persistance.entites.EntiteReunion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO pour Reunion
 */
@Repository
public interface ReunionDAO extends JpaRepository<EntiteReunion, Long> {

    // Méthodes de requête personnalisées peuvent être ajoutées ici
}
