package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.RoleDansReunion;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.serveur.dao.InterfaceParticipationReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Reunion;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceGestionReunions {

    private final InterfaceReunionDAO reunionDAO;
    private final InterfaceParticipationReunionDAO participationReunionDAO;
    private final InterfaceUtilisateurDAO utilisateurDAO;
    private final ServiceCommunicationReunion serviceCommunication;
    private static final Logger journal = LoggerFactory.getLogger(ServiceGestionReunions.class);

    public ServiceGestionReunions(InterfaceReunionDAO reunionDAO, InterfaceParticipationReunionDAO participationReunionDAO, InterfaceUtilisateurDAO utilisateurDAO, ServiceCommunicationReunion serviceCommunication) {
        this.reunionDAO = reunionDAO;
        this.participationReunionDAO = participationReunionDAO;
        this.utilisateurDAO = utilisateurDAO;
        this.serviceCommunication = serviceCommunication;
    }

    private DetailsReunionDTO convertirEntiteReunionVersDTO(Reunion entite, List<DonneesUtilisateurDTO> participantsDTO) {
        if (entite == null) {
            return null;
        }
        String nomOrganisateur = "Inconnu";
        try {
            // Supposant que utilisateurDAO.trouverParId attend un int, mais l'ID organisateur est long
            Optional<Utilisateur> organisateurOpt = utilisateurDAO.trouverParId((int) entite.getOrganisateurId());
            if (organisateurOpt.isPresent()) {
                nomOrganisateur = organisateurOpt.get().getNomComplet();
            }
        } catch (ExceptionPersistance e) {
            journal.warn("Impossible de récupérer le nom de l'organisateur ID {} pour la réunion ID {}: {}", entite.getOrganisateurId(), entite.getId(), e.getMessage());
        }

        return new DetailsReunionDTO(
                entite.getId(),
                entite.getTitre(),
                entite.getDescription(),
                entite.getDateHeureDebut(),
                entite.getDureeEstimeeMinutes(),
                entite.getTypeReunion(),
                entite.getStatutReunion(),
                entite.getOrganisateurId(),
                nomOrganisateur,
                entite.getMotDePasseOptionnel(),
                participantsDTO != null ? participantsDTO : new ArrayList<>(), // Assurer une liste non nulle
                entite.getDateCreationReunion()
        );
    }

    public Optional<DetailsReunionDTO> creerNouvelleReunion(String titre, String description, LocalDateTime dateHeureDebut, int dureeMinutes, TypeReunion typeReunion, String motDePasseOptionnel, long idOrganisateur) {
        journal.debug("Tentative de création d'une nouvelle réunion par l'utilisateur ID {}", idOrganisateur);
        try {
            ValidateurEntreeUtilisateur.validerNonNulOuVide(titre, "Titre de la réunion");
            if (dureeMinutes <= 0) {
                throw new IllegalArgumentException("La durée en minutes doit être positive.");
            }
            if (dateHeureDebut == null || dateHeureDebut.isBefore(LocalDateTime.now().plusMinutes(1))) {
                throw new IllegalArgumentException("La date et heure de début doit être dans le futur.");
            }
            if (typeReunion == TypeReunion.PRIVEE && (motDePasseOptionnel == null || motDePasseOptionnel.trim().isEmpty())) {
                journal.warn("Création d'une réunion privée ID {} sans mot de passe par l'utilisateur ID {}.", titre, idOrganisateur);
            }

            Reunion nouvelleReunionEntite = new Reunion();
            nouvelleReunionEntite.setTitre(titre);
            nouvelleReunionEntite.setDescription(description); // Utiliser setDescription
            nouvelleReunionEntite.setDateHeureDebut(dateHeureDebut);
            nouvelleReunionEntite.setDureeEstimeeMinutes(dureeMinutes); // Utiliser setDureeEstimeeMinutes
            nouvelleReunionEntite.setTypeReunion(typeReunion);
            nouvelleReunionEntite.setStatutReunion(StatutReunion.PLANIFIEE);
            nouvelleReunionEntite.setOrganisateurId(idOrganisateur);
            nouvelleReunionEntite.setMotDePasseOptionnel(motDePasseOptionnel != null ? motDePasseOptionnel.trim() : null);
            nouvelleReunionEntite.setDateCreationReunion(LocalDateTime.now());

            Reunion reunionCreee = this.reunionDAO.creer(nouvelleReunionEntite);

            if (reunionCreee != null && reunionCreee.getId() > 0) {
                this.participationReunionDAO.ajouterParticipant((int)reunionCreee.getId(), (int)idOrganisateur, RoleDansReunion.ORGANISATEUR, StatutParticipationReunion.REJOINT);
                journal.info("Nouvelle réunion '{}' (ID: {}) créée par l'utilisateur ID {}.", titre, reunionCreee.getId(), idOrganisateur);
                List<DonneesUtilisateurDTO> participantsInitiaux = this.participationReunionDAO.recupererParticipantsDetailsParIdReunion((int)reunionCreee.getId());
                return Optional.of(convertirEntiteReunionVersDTO(reunionCreee, participantsInitiaux));
            } else {
                journal.error("Échec de la persistance de la nouvelle réunion '{}' par l'utilisateur ID {}.", titre, idOrganisateur);
                return Optional.empty();
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            journal.error("Données invalides pour la création de la nouvelle réunion '{}' par l'utilisateur ID {}: {}", titre, idOrganisateur, e.getMessage(), e);
            return Optional.empty();
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la création de la nouvelle réunion '{}' par l'utilisateur ID {}: {}", titre, idOrganisateur, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<DetailsReunionDTO> listerToutesLesReunionsActivesEtPlanifiees() throws ExceptionPersistance {
        journal.debug("Listage des réunions actives et planifiées.");
        List<Reunion> reunionsPlanifiees = this.reunionDAO.trouverParStatut(StatutReunion.PLANIFIEE);
        List<Reunion> reunionsOuvertes = this.reunionDAO.trouverParStatut(StatutReunion.OUVERTE);

        return Stream.concat(reunionsPlanifiees.stream(), reunionsOuvertes.stream())
                .map(entite -> convertirEntiteReunionVersDTO(entite, new ArrayList<>()))
                .collect(Collectors.toList());
    }

    public List<DetailsReunionDTO> listerReunionsParOrganisateur(long idOrganisateur) throws ExceptionPersistance {
        journal.debug("Listage des réunions pour l'organisateur ID {}.", idOrganisateur);
        List<Reunion> reunionsOrganisees = this.reunionDAO.trouverParOrganisateurId((int)idOrganisateur);
        return reunionsOrganisees.stream()
                .map(entite -> convertirEntiteReunionVersDTO(entite, new ArrayList<>()))
                .collect(Collectors.toList());
    }

    public Optional<DetailsReunionDTO> obtenirDetailsReunion(long idReunion) throws ExceptionPersistance {
        journal.debug("Obtention des détails pour la réunion ID {}.", idReunion);
        Reunion reunionEntite = this.reunionDAO.trouverParId((int)idReunion);
        if (reunionEntite == null) {
            journal.warn("Réunion ID {} non trouvée.", idReunion);
            return Optional.empty();
        }
        List<DonneesUtilisateurDTO> participantsDTO = this.participationReunionDAO.recupererParticipantsDetailsParIdReunion((int)idReunion);
        return Optional.of(convertirEntiteReunionVersDTO(reunionEntite, participantsDTO));
    }

    public ReponseGeneriqueDTO rejoindreReunion(long idReunion, long idUtilisateur, ThreadClientDedie clientThread, String motDePasseFourni) {
        journal.debug("Tentative de l'utilisateur ID {} de rejoindre la réunion ID {}.", idUtilisateur, idReunion);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId((int)idReunion);
            if (reunionEntite == null) {
                return new ReponseGeneriqueDTO(false, "Réunion non trouvée.", "REUNION_INEXISTANTE");
            }
            if (reunionEntite.getStatutReunion() != StatutReunion.OUVERTE) {
                return new ReponseGeneriqueDTO(false, "La réunion n'est pas ouverte.", "REUNION_NON_OUVERTE");
            }
            if (reunionEntite.getTypeReunion() == TypeReunion.PRIVEE) {
                String motDePasseReunion = reunionEntite.getMotDePasseOptionnel();
                if (motDePasseReunion == null || motDePasseReunion.isEmpty()) {
                    return new ReponseGeneriqueDTO(false, "Configuration de mot de passe invalide pour cette réunion privée.", "REUNION_PRIVEE_MDP_ERREUR_CONFIG");
                }
                if (!motDePasseReunion.equals(motDePasseFourni)) {
                    return new ReponseGeneriqueDTO(false, "Mot de passe incorrect pour la réunion privée.", "MDP_INCORRECT");
                }
            }

            RoleDansReunion roleExistant = participationReunionDAO.recupererRoleDansReunion((int)idReunion, (int)idUtilisateur);
            if (roleExistant == null) {
                this.participationReunionDAO.ajouterParticipant((int)idReunion, (int)idUtilisateur, RoleDansReunion.PARTICIPANT, StatutParticipationReunion.REJOINT);
            } else {
                this.participationReunionDAO.mettreAJourStatutParticipation((int)idReunion, (int)idUtilisateur, StatutParticipationReunion.REJOINT);
            }

            this.serviceCommunication.enregistrerParticipantConnecte(idReunion, clientThread);
            journal.info("Utilisateur ID {} a rejoint la réunion ID {}.", idUtilisateur, idReunion);
            return new ReponseGeneriqueDTO(true, "Vous avez rejoint la réunion.");

        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance en tentant de rejoindre la réunion ID {}: {}", idReunion, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la tentative de rejoindre la réunion.", "ERREUR_SERVEUR_REJOINDRE");
        }
    }

    public ReponseGeneriqueDTO quitterReunion(long idReunion, long idUtilisateur, ThreadClientDedie clientThread) {
        journal.debug("Tentative de l'utilisateur ID {} de quitter la réunion ID {}.", idUtilisateur, idReunion);
        try {
            this.participationReunionDAO.mettreAJourStatutParticipation((int)idReunion, (int)idUtilisateur, StatutParticipationReunion.PARTI);
            this.serviceCommunication.retirerParticipantConnecte(idReunion, clientThread);
            journal.info("Utilisateur ID {} a quitté la réunion ID {}.", idUtilisateur, idReunion);
            return new ReponseGeneriqueDTO(true, "Vous avez quitté la réunion.");
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance en tentant de quitter la réunion ID {}: {}", idReunion, e.getMessage(), e);
            this.serviceCommunication.retirerParticipantConnecte(idReunion, clientThread);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la tentative de quitter la réunion.", "ERREUR_SERVEUR_QUITTER");
        }
    }

    public ReponseGeneriqueDTO ouvrirReunion(long idReunion, long idOrganisateurDemandeur) {
        journal.debug("Tentative d'ouverture de la réunion ID {} par l'utilisateur ID {}.", idReunion, idOrganisateurDemandeur);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId((int)idReunion);
            if (reunionEntite == null) {
                return new ReponseGeneriqueDTO(false, "Réunion non trouvée.", "REUNION_INEXISTANTE");
            }
            if (reunionEntite.getOrganisateurId() != idOrganisateurDemandeur) {
                return new ReponseGeneriqueDTO(false, "Seul l'organisateur peut ouvrir la réunion.", "ACTION_NON_AUTORISEE");
            }
            if (reunionEntite.getStatutReunion() != StatutReunion.PLANIFIEE) {
                return new ReponseGeneriqueDTO(false, "La réunion n'est pas planifiée ou est déjà ouverte/terminée.", "STATUT_INCORRECT_OUVERTURE");
            }

            reunionEntite.setStatutReunion(StatutReunion.OUVERTE);
            boolean succesMAJ = this.reunionDAO.mettreAJour(reunionEntite);
            if (succesMAJ) {
                journal.info("Réunion ID {} ouverte par l'organisateur ID {}.", idReunion, idOrganisateurDemandeur);
                return new ReponseGeneriqueDTO(true, "Réunion ouverte avec succès.");
            } else {
                return new ReponseGeneriqueDTO(false, "Échec de la mise à jour du statut de la réunion.", "MAJ_STATUT_REUNION_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de l'ouverture de la réunion ID {}: {}", idReunion, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de l'ouverture de la réunion.", "ERREUR_SERVEUR_OUVRIR");
        }
    }

    public ReponseGeneriqueDTO cloreReunion(long idReunion, long idOrganisateurDemandeur) {
        journal.debug("Tentative de clôture de la réunion ID {} par l'utilisateur ID {}.", idReunion, idOrganisateurDemandeur);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId((int)idReunion);
            if (reunionEntite == null) {
                return new ReponseGeneriqueDTO(false, "Réunion non trouvée.", "REUNION_INEXISTANTE");
            }
            if (reunionEntite.getOrganisateurId() != idOrganisateurDemandeur) {
                return new ReponseGeneriqueDTO(false, "Seul l'organisateur peut clore la réunion.", "ACTION_NON_AUTORISEE");
            }
            if (reunionEntite.getStatutReunion() == StatutReunion.CLOTUREE || reunionEntite.getStatutReunion() == StatutReunion.ANNULEE) {
                return new ReponseGeneriqueDTO(false, "La réunion est déjà clôturée ou annulée.", "STATUT_INCORRECT_CLOTURE");
            }

            reunionEntite.setStatutReunion(StatutReunion.CLOTUREE);
            boolean succesMAJ = this.reunionDAO.mettreAJour(reunionEntite);

            if (succesMAJ) {
                journal.info("Réunion ID {} clôturée par l'organisateur ID {}.", idReunion, idOrganisateurDemandeur);
                this.serviceCommunication.notifierFermetureReunion(idReunion);
                return new ReponseGeneriqueDTO(true, "Réunion clôturée avec succès.");
            } else {
                return new ReponseGeneriqueDTO(false, "Échec de la mise à jour du statut de la réunion pour clôture.", "MAJ_STATUT_REUNION_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la clôture de la réunion ID {}: {}", idReunion, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la clôture de la réunion.", "ERREUR_SERVEUR_CLORE");
        }
    }
}