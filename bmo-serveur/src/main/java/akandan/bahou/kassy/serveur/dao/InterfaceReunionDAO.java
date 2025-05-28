package akandan.bahou.kassy.serveur.dao;

import java.util.List;
import akandan.bahou.kassy.serveur.modele.Reunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.modele.StatutReunion;

public interface InterfaceReunionDAO {

    Reunion creer(Reunion reunionACreer) throws ExceptionPersistance;

    Reunion trouverParId(int idReunion) throws ExceptionPersistance;

    List<Reunion> trouverToutes() throws ExceptionPersistance;

    boolean mettreAJour(Reunion reunionAMettreAJour) throws ExceptionPersistance;

    boolean supprimer(int idReunion) throws ExceptionPersistance;

    List<Reunion> trouverParOrganisateurId(int idOrganisateur) throws ExceptionPersistance;

    List<Reunion> trouverParStatut(StatutReunion statut) throws ExceptionPersistance;
}