package akandan.bahou.kassy.serveur.dao;

import java.util.List;
import java.util.Optional;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;

public interface InterfaceUtilisateurDAO {

    Utilisateur creer(Utilisateur utilisateurACreer) throws ExceptionPersistance;

    Optional<Utilisateur> trouverParId(int idUtilisateur) throws ExceptionPersistance;

    Optional<Utilisateur> trouverParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance;

    List<Utilisateur> trouverTous() throws ExceptionPersistance;

    boolean mettreAJour(Utilisateur utilisateurAMettreAJour) throws ExceptionPersistance;

    boolean supprimer(int idUtilisateur) throws ExceptionPersistance;

    boolean existeParIdentifiantConnexion(String identifiantConnexion) throws ExceptionPersistance;
}