package akandan.bahou.kassy.serveur.persistance.dao;

import akandan.bahou.kassy.serveur.persistance.entites.EntiteMessageChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO pour MessageChat
 */
@Repository
public interface MessageChatDAO extends JpaRepository<EntiteMessageChat, Long> {

    // Méthodes de requête personnalisées peuvent être ajoutées ici
}
