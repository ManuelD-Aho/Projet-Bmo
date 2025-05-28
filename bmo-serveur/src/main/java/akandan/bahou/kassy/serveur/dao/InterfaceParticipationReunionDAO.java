package akandan.bahou.kassy.serveur.dao;

import java.util.List;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.commun.modele.RoleDansReunion;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;

public interface InterfaceParticipationReunionDAO {

    void ajouterParticipant(int idReunion, int idUtilisateur, RoleDansReunion role, StatutParticipationReunion statutInitial) throws ExceptionPersistance;

    boolean mettreAJourStatutParticipation(int idReunion, int idUtilisateur, StatutParticipationReunion nouveauStatut) throws ExceptionPersistance;

    boolean mettreAJourRoleDansReunion(int idReunion, int idUtilisateur, RoleDansReunion nouveauRole) throws ExceptionPersistance;

    void supprimerParticipant(int idReunion, int idUtilisateur) throws ExceptionPersistance;

    List<Integer> recupererIdsParticipantsParIdReunion(int idReunion) throws ExceptionPersistance;

    List<Integer> recupererIdsParticipantsActifsParIdReunion(int idReunion) throws ExceptionPersistance;

    StatutParticipationReunion recupererStatutParticipation(int idReunion, int idUtilisateur) throws ExceptionPersistance;

    RoleDansReunion recupererRoleDansReunion(int idReunion, int idUtilisateur) throws ExceptionPersistance;

    List<DonneesUtilisateurDTO> recupererParticipantsDetailsParIdReunion(int idReunion) throws ExceptionPersistance;
}