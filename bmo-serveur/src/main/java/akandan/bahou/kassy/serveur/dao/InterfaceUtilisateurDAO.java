package akandan.bahou.kassy.serveur.dao;

import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.util.List;
import java.util.Optional;

public interface InterfaceUtilisateurDAO {

    Utilisateur creer(Utilisateur utilisateurACreer) throws ExceptionPersistance;

    Optional<Utilisateur> trouverParId(long idUtilisateur) throws ExceptionPersistance; // Corrigé en long

    Optional<Utilisateur> trouverParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance;

    List<Utilisateur> trouverTous() throws ExceptionPersistance;

    boolean mettreAJour(Utilisateur utilisateurAMettreAJour) throws ExceptionPersistance;

    boolean supprimer(long idUtilisateur) throws ExceptionPersistance; // Corrigé en long

    boolean existeParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance;
}