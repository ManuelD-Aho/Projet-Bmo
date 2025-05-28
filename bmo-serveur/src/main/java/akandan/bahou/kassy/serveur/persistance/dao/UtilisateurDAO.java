package akandan.bahou.kassy.serveur.persistance.dao;

import akandan.bahou.kassy.serveur.persistance.entites.EntiteUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO pour Utilisateur
 */
@Repository
public interface UtilisateurDAO extends JpaRepository<EntiteUtilisateur, Long> {

    // Méthodes de requête personnalisées peuvent être ajoutées ici
}
