package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.serveur.dao.InterfaceReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceParticipationReunionDAO;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Reunion;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.RoleDansReunion;
import akandan.bahou.kassy.commun.modele.StatutParticipationReunion;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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

    private DetailsReunionDTO convertirEntiteReunionVersDTO(Reunion entite) {
        if (entite == null) {
            return null;
        }
        DetailsReunionDTO dto = new DetailsReunionDTO();
        dto.setIdReunion(entite.getId());
        dto.setTitre(entite.getTitre());
        dto.setOrdreDuJour(entite.getOrdreDuJour());
        if (entite.getDateHeureDebut() != null) {
            dto.setDateHeureDebut(entite.getDateHeureDebut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        dto.setDureeMinutes(entite.getDureeMinutes());
        dto.setTypeReunion(entite.getTypeReunion());
        dto.setStatutReunion(entite.getStatutReunion());
        dto.setIdOrganisateur(entite.getOrganisateurId());
        try {
            Optional<Utilisateur> organisateurOpt = utilisateurDAO.trouverParId(entite.getOrganisateurId());
            organisateurOpt.ifPresent(utilisateur -> dto.setNomOrganisateur(utilisateur.getNomComplet()));
        } catch (ExceptionPersistance e) {
            journal.warn("Impossible de récupérer le nom de l'organisateur ID {} pour la réunion ID {}: {}", entite.getOrganisateurId(), entite.getId(), e.getMessage());
            dto.setNomOrganisateur("Inconnu");
        }
        dto.setMotDePasseOptionnelValeur(entite.getMotDePasseOptionnel());

        return dto;
    }

    public DetailsReunionDTO creerNouvelleReunion(String titre, String ordreDuJour, String dateHeureDebutStr, int dureeMinutes, TypeReunion typeReunion, String motDePasseOptionnel, int idOrganisateur) {
        journal.debug("Tentative de création d'une nouvelle réunion par l'utilisateur ID {}", idOrganisateur);
        try {
            ValidateurEntreeUtilisateur.validerNonNulOuVide(titre, "Titre de la réunion");
            if (dureeMinutes <= 0) {
                throw new IllegalArgumentException("La durée en minutes doit être positive.");
            }
            LocalDateTime dateHeureDebut = LocalDateTime.parse(dateHeureDebutStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Reunion nouvelleReunionEntite = new Reunion();
            nouvelleReunionEntite.setTitre(titre);
            nouvelleReunionEntite.setOrdreDuJour(ordreDuJour);
            nouvelleReunionEntite.setDateHeureDebut(dateHeureDebut);
            nouvelleReunionEntite.setDureeMinutes(dureeMinutes);
            nouvelleReunionEntite.setTypeReunion(typeReunion);
            nouvelleReunionEntite.setStatutReunion(StatutReunion.PLANIFIEE);
            nouvelleReunionEntite.setOrganisateurId(idOrganisateur);
            if (typeReunion == TypeReunion.PRIVEE && (motDePasseOptionnel == null || motDePasseOptionnel.trim().isEmpty())) {
                journal.warn("Tentative de création d'une réunion privée sans mot de passe par l'utilisateur ID {}", idOrganisateur);
            }
            nouvelleReunionEntite.setMotDePasseOptionnel(motDePasseOptionnel != null ? motDePasseOptionnel.trim() : null);
            nouvelleReunionEntite.setDateCreationReunion(LocalDateTime.now());

            Reunion reunionCreee = this.reunionDAO.creer(nouvelleReunionEntite);

            if (reunionCreee != null && reunionCreee.getId() > 0) {
                this.participationReunionDAO.ajouterParticipant(reunionCreee.getId(), idOrganisateur, RoleDansReunion.ORGANISATEUR, StatutParticipationReunion.REJOINT);
                journal.info("Nouvelle réunion '{}' (ID: {}) créée par l'utilisateur ID {}.", titre, reunionCreee.getId(), idOrganisateur);
                return convertirEntiteReunionVersDTO(reunionCreee);
            } else {
                journal.error("Échec de la persistance de la nouvelle réunion '{}' par l'utilisateur ID {}.", titre, idOrganisateur);
                return null;
            }
        } catch (DateTimeParseException e) {
            journal.error("Format de dateHeureDebut invalide '{}' fourni par l'utilisateur ID {}.", dateHeureDebutStr, idOrganisateur, e);
            return null;
        } catch (IllegalArgumentException | ExceptionPersistance e) {
            journal.error("Erreur lors de la création de la nouvelle réunion '{}' par l'utilisateur ID {}: {}", titre, idOrganisateur, e.getMessage(), e);
            return null;
        }
    }

    public List<DetailsReunionDTO> listerToutesLesReunionsActivesEtPlanifiees() throws ExceptionPersistance {
        journal.debug("Listage des réunions actives et planifiées.");
        List<Reunion> reunionsPlanifiees = this.reunionDAO.trouverParStatut(StatutReunion.PLANIFIEE);
        List<Reunion> reunionsOuvertes = this.reunionDAO.trouverParStatut(StatutReunion.OUVERTE);

        return Stream.concat(reunionsPlanifiees.stream(), reunionsOuvertes.stream())
                .map(this::convertirEntiteReunionVersDTO)
                .collect(Collectors.toList());
    }

    public List<DetailsReunionDTO> listerReunionsParUtilisateur(int idUtilisateur) throws ExceptionPersistance {
        journal.debug("Listage des réunions pour l'utilisateur ID {}.", idUtilisateur);
        List<Reunion> reunionsOrganisees = this.reunionDAO.trouverParOrganisateurId(idUtilisateur);
        return reunionsOrganisees.stream()
                .map(this::convertirEntiteReunionVersDTO)
                .collect(Collectors.toList());
    }


    public DetailsReunionDTO obtenirDetailsReunion(int idReunion) throws ExceptionPersistance {
        journal.debug("Obtention des détails pour la réunion ID {}.", idReunion);
        Reunion reunionEntite = this.reunionDAO.trouverParId(idReunion);
        if (reunionEntite == null) {
            journal.warn("Réunion ID {} non trouvée.", idReunion);
            return null;
        }
        DetailsReunionDTO dto = convertirEntiteReunionVersDTO(reunionEntite);
        if (dto != null) {
            List<DonneesUtilisateurDTO> participantsDTO = this.participationReunionDAO.recupererParticipantsDetailsParIdReunion(idReunion);
            dto.setParticipants(participantsDTO);
        }
        return dto;
    }

    public ReponseGeneriqueDTO rejoindreReunion(int idReunion, int idUtilisateur, ThreadClientDedie clientThread, String motDePasseFourni) {
        journal.debug("Tentative de l'utilisateur ID {} de rejoindre la réunion ID {}.", idUtilisateur, idReunion);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId(idReunion);
            if (reunionEntite == null) {
                return new ReponseGeneriqueDTO(false, "Réunion non trouvée.", "REUNION_INEXISTANTE");
            }
            if (reunionEntite.getStatutReunion() != StatutReunion.OUVERTE) {
                return new ReponseGeneriqueDTO(false, "La réunion n'est pas ouverte.", "REUNION_NON_OUVERTE");
            }
            if (reunionEntite.getTypeReunion() == TypeReunion.PRIVEE) {
                String motDePasseReunion = reunionEntite.getMotDePasseOptionnel();
                if (motDePasseReunion == null || motDePasseReunion.isEmpty()) {
                    journal.warn("Réunion privée ID {} sans mot de passe défini, accès refusé par mesure de sécurité.", idReunion);
                    return new ReponseGeneriqueDTO(false, "Cette réunion privée est mal configurée (pas de mot de passe).", "REUNION_PRIVEE_MDP_MANQUANT");
                }
                if (!motDePasseReunion.equals(motDePasseFourni)) {
                    return new ReponseGeneriqueDTO(false, "Mot de passe incorrect pour la réunion privée.", "MDP_INCORRECT");
                }
            }

            RoleDansReunion roleExistant = participationReunionDAO.recupererRoleDansReunion(idReunion, idUtilisateur);
            if (roleExistant == null) {
                this.participationReunionDAO.ajouterParticipant(idReunion, idUtilisateur, RoleDansReunion.PARTICIPANT, StatutParticipationReunion.INVITE);
            }

            boolean majStatut = this.participationReunionDAO.mettreAJourStatutParticipation(idReunion, idUtilisateur, StatutParticipationReunion.REJOINT);
            if (majStatut) {
                this.serviceCommunication.enregistrerParticipantConnecte(idReunion, clientThread);
                journal.info("Utilisateur ID {} a rejoint la réunion ID {}.", idUtilisateur, idReunion);
                return new ReponseGeneriqueDTO(true, "Vous avez rejoint la réunion.");
            } else {
                journal.warn("Échec de la mise à jour du statut de participation pour l'utilisateur ID {} dans la réunion ID {}.", idUtilisateur, idReunion);
                return new ReponseGeneriqueDTO(false, "Impossible de mettre à jour le statut de participation.", "MAJ_STATUT_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance en tentant de rejoindre la réunion ID {}: {}", idReunion, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la tentative de rejoindre la réunion.", "ERREUR_SERVEUR_REJOINDRE");
        }
    }

    public ReponseGeneriqueDTO quitterReunion(int idReunion, int idUtilisateur, ThreadClientDedie clientThread) {
        journal.debug("Tentative de l'utilisateur ID {} de quitter la réunion ID {}.", idUtilisateur, idReunion);
        try {
            boolean majStatut = this.participationReunionDAO.mettreAJourStatutParticipation(idReunion, idUtilisateur, StatutParticipationReunion.PARTI);
            this.serviceCommunication.retirerParticipantConnecte(idReunion, clientThread);
            if(majStatut) {
                journal.info("Utilisateur ID {} a quitté la réunion ID {}.", idUtilisateur, idReunion);
                return new ReponseGeneriqueDTO(true, "Vous avez quitté la réunion.");
            } else {
                journal.warn("Échec de la mise à jour du statut (PARTI) pour l'utilisateur ID {} dans la réunion ID {}, mais retiré de la communication.", idUtilisateur, idReunion);
                return new ReponseGeneriqueDTO(false, "Impossible de mettre à jour le statut de départ, mais vous êtes déconnecté de la communication.", "MAJ_STATUT_PARTI_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance en tentant de quitter la réunion ID {}: {}", idReunion, e.getMessage(), e);
            this.serviceCommunication.retirerParticipantConnecte(idReunion, clientThread);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la tentative de quitter la réunion.", "ERREUR_SERVEUR_QUITTER");
        }
    }

    public ReponseGeneriqueDTO ouvrirReunion(int idReunion, int idOrganisateurDemandeur) {
        journal.debug("Tentative d'ouverture de la réunion ID {} par l'utilisateur ID {}.", idReunion, idOrganisateurDemandeur);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId(idReunion);
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

    public ReponseGeneriqueDTO cloreReunion(int idReunion, int idOrganisateurDemandeur) {
        journal.debug("Tentative de clôture de la réunion ID {} par l'utilisateur ID {}.", idReunion, idOrganisateurDemandeur);
        try {
            Reunion reunionEntite = this.reunionDAO.trouverParId(idReunion);
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