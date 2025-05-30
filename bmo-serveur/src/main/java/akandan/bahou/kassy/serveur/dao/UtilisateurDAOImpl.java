package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilisateurDAOImpl implements InterfaceUtilisateurDAO {

    // Stockage en mémoire des utilisateurs (clé : id)
    private static final ConcurrentHashMap<Long, Utilisateur> utilisateurs = new ConcurrentHashMap<>();
    private static final AtomicLong sequenceId = new AtomicLong(1);
    private static final Logger journal = LoggerFactory.getLogger(UtilisateurDAOImpl.class);

    public UtilisateurDAOImpl() {
        // Constructeur vide, plus de gestionnaire de connexions
    }

    @Override
    public Utilisateur creer(Utilisateur utilisateurACreer) throws ExceptionPersistance {
        if (utilisateurACreer == null) {
            throw new ExceptionPersistance("L'utilisateur à créer ne peut pas être null.");
        }
        // Génération d'un nouvel ID unique
        long nouvelId = sequenceId.getAndIncrement();
        utilisateurACreer.setId(nouvelId);
        if (utilisateurACreer.getDateCreationCompte() == null) {
            utilisateurACreer.setDateCreationCompte(LocalDateTime.now());
        }
        utilisateurs.put(nouvelId, utilisateurACreer);
        journal.debug("Utilisateur créé en mémoire avec ID : {}", nouvelId);
        return utilisateurACreer;
    }

    @Override
    public Optional<Utilisateur> trouverParId(long idUtilisateur) throws ExceptionPersistance {
        Utilisateur utilisateur = utilisateurs.get(idUtilisateur);
        if (utilisateur != null) {
            journal.debug("Utilisateur trouvé en mémoire par ID : {}", idUtilisateur);
            return Optional.of(utilisateur);
        }
        journal.debug("Aucun utilisateur trouvé en mémoire pour ID : {}", idUtilisateur);
        return Optional.empty();
    }

    @Override
    public Optional<Utilisateur> trouverParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance {
        for (Utilisateur utilisateur : utilisateurs.values()) {
            if (utilisateur.getIdentifiant() != null && utilisateur.getIdentifiant().equals(identifiantConnexion)) {
                journal.debug("Utilisateur trouvé en mémoire par identifiant de connexion : {}", identifiantConnexion);
                return Optional.of(utilisateur);
            }
        }
        journal.debug("Aucun utilisateur trouvé en mémoire pour l'identifiant de connexion : {}", identifiantConnexion);
        return Optional.empty();
    }

    @Override
    public List<Utilisateur> trouverTous() throws ExceptionPersistance {
        List<Utilisateur> liste = new ArrayList<>(utilisateurs.values());
        journal.debug("{} utilisateurs trouvés en mémoire au total.", liste.size());
        return liste;
    }

    @Override
    public boolean mettreAJour(Utilisateur utilisateurAMettreAJour) throws ExceptionPersistance {
        if (utilisateurAMettreAJour == null || utilisateurAMettreAJour.getId() == 0) {
            throw new ExceptionPersistance("L'utilisateur à mettre à jour est invalide.");
        }
        if (utilisateurs.containsKey(utilisateurAMettreAJour.getId())) {
            utilisateurs.put(utilisateurAMettreAJour.getId(), utilisateurAMettreAJour);
            journal.debug("Mise à jour de l'utilisateur en mémoire ID : {}", utilisateurAMettreAJour.getId());
            return true;
        }
        journal.debug("Aucune mise à jour effectuée, utilisateur ID : {} non trouvé.", utilisateurAMettreAJour.getId());
        return false;
    }

    @Override
    public boolean supprimer(long idUtilisateur) throws ExceptionPersistance {
        Utilisateur retire = utilisateurs.remove(idUtilisateur);
        boolean resultat = (retire != null);
        journal.debug("Suppression de l'utilisateur en mémoire ID : {}, succès : {}", idUtilisateur, resultat);
        return resultat;
    }

    @Override
    public boolean existeParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance {
        for (Utilisateur utilisateur : utilisateurs.values()) {
            if (utilisateur.getIdentifiant() != null && utilisateur.getIdentifiant().equals(identifiantConnexion)) {
                journal.debug("Vérification existence en mémoire pour identifiant de connexion '{}': true", identifiantConnexion);
                return true;
            }
        }
        journal.debug("Vérification existence en mémoire pour identifiant de connexion '{}': false", identifiantConnexion);
        return false;
    }
}