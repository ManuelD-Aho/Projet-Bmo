package akandan.bahou.kassy.serveur.util;

import akandan.bahou.kassy.serveur.service.ServiceAuthentification;
import akandan.bahou.kassy.serveur.service.ServiceGestionUtilisateurs;
import akandan.bahou.kassy.serveur.service.ServiceGestionReunions;
import akandan.bahou.kassy.serveur.service.ServiceCommunicationReunion;
import akandan.bahou.kassy.serveur.service.ServiceAdministration;
import akandan.bahou.kassy.commun.protocole.TypeRequeteClient;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyseurRequeteClient {

    private final ServiceAuthentification serviceAuthentification;
    private final ServiceGestionUtilisateurs serviceGestionUtilisateurs;
    private final ServiceGestionReunions serviceGestionReunions;
    private final ServiceCommunicationReunion serviceCommunicationReunion;
    private final ServiceAdministration serviceAdministration;
    private static final Logger journal = LoggerFactory.getLogger(AnalyseurRequeteClient.class);
    private static final String DELIMITEUR_PARAMETRES_REQUETE = "\\" + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE;

    public AnalyseurRequeteClient(ServiceAuthentification authService, ServiceGestionUtilisateurs userGestService, ServiceGestionReunions meetingGestService, ServiceCommunicationReunion commMeetingService, ServiceAdministration adminService) {
        this.serviceAuthentification = authService;
        this.serviceGestionUtilisateurs = userGestService;
        this.serviceGestionReunions = meetingGestService;
        this.serviceCommunicationReunion = commMeetingService;
        this.serviceAdministration = adminService;
    }

    public String traiterRequete(String requeteBrute, ThreadClientDedie threadClientConcerne) {
        if (requeteBrute == null || requeteBrute.trim().isEmpty()) {
            return formaterReponseErreur("REQUETE_VIDE", "La requête reçue est vide.");
        }
        String[] tokens = requeteBrute.split(DELIMITEUR_PARAMETRES_REQUETE, -1);
        if (tokens.length == 0) {
            return formaterReponseErreur("FORMAT_REQUETE_INVALIDE", "Format de requête invalide.");
        }
        String commandeStr = tokens[0].toUpperCase();
        TypeRequeteClient typeRequete;
        try {
            typeRequete = TypeRequeteClient.valueOf(commandeStr);
        } catch (IllegalArgumentException e) {
            return formaterReponseErreur("COMMANDE_INCONNUE", "La commande '" + commandeStr + "' est inconnue.");
        }

        journal.debug("Traitement de la requête de type {} pour le client {}", typeRequete, threadClientConcerne.getSocketClientCommunication().getRemoteSocketAddress());

        try {
            return switch (typeRequete) {
                case CONNEXION -> traiterConnexion(tokens, threadClientConcerne);
                case INSCRIPTION -> traiterInscription(tokens);
                case DECONNEXION -> traiterDeconnexion(threadClientConcerne);
                case NOUVELLE_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterNouvelleReunion(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case OBTENIR_REUNIONS -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterObtenirReunions();
                }
                case OBTENIR_MES_REUNIONS -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterObtenirMesReunions(threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case OBTENIR_DETAILS_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterObtenirDetailsReunion(tokens);
                }
                case REJOINDRE_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterRejoindreReunion(tokens, threadClientConcerne);
                }
                case QUITTER_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterQuitterReunion(tokens, threadClientConcerne);
                }
                case MESSAGE_CHAT -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterMessageChat(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case OBTENIR_HISTORIQUE_MESSAGES -> {
                    if(!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterObtenirHistoriqueMessages(tokens);
                }
                case OUVRIR_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterOuvrirReunion(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case CLOTURER_REUNION -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterCloreReunion(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case MODIFIER_PROFIL -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterModifierProfil(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case CHANGER_MOT_DE_PASSE -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterChangerMotDePasse(tokens, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case OBTENIR_MON_PROFIL -> {
                    if (!estAuthentifie(threadClientConcerne)) yield reponseAccesRefuse();
                    yield traiterObtenirMonProfil(threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                }
                case ADMIN_OBTENIR_UTILISATEURS -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminObtenirUtilisateurs();
                }
                case ADMIN_CREER_UTILISATEUR -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminCreerUtilisateur(tokens);
                }
                case ADMIN_MODIFIER_UTILISATEUR -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminModifierUtilisateur(tokens);
                }
                case ADMIN_SUPPRIMER_UTILISATEUR -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminSupprimerUtilisateur(tokens);
                }
                case ADMIN_OBTENIR_CONFIG -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminObtenirConfig();
                }
                case ADMIN_DEFINIR_CONFIG -> {
                    if (!estAdmin(threadClientConcerne)) yield reponseAccesRefuseAdmin();
                    yield traiterAdminDefinirConfig(tokens);
                }
                default -> formaterReponseErreur("COMMANDE_NON_GEREE", "La commande '" + typeRequete.name() + "' n'est pas encore gérée.");
            };
        } catch (ExceptionValidation ev) {
            journal.warn("Erreur de validation lors du traitement de {} : {}", typeRequete.name(), ev.getMessage());
            return formaterReponseErreur("VALIDATION_ECHOUEE", ev.getMessage());
        } catch (ExceptionPersistance ep) {
            journal.error("Erreur de persistance lors du traitement de {} : {}", typeRequete.name(), ep.getMessage(), ep);
            return formaterReponseErreur("ERREUR_PERSISTANCE", "Une erreur de base de données est survenue.");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors du traitement de {} : {}", typeRequete.name(), e.getMessage(), e);
            return formaterReponseErreur("ERREUR_INTERNE_ANALYSEUR", "Une erreur interne est survenue lors du traitement de votre requête.");
        }
    }

    private String traiterConnexion(String[] tokens, ThreadClientDedie threadClient) {
        if (tokens.length < 3) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour CONNEXION.");
        String login = tokens[1];
        String mdp = tokens[2];
        Optional<DonneesUtilisateurDTO> dtoOpt = this.serviceAuthentification.authentifierUtilisateur(login, mdp);
        if (dtoOpt.isPresent()) {
            DonneesUtilisateurDTO dto = dtoOpt.get();
            threadClient.definirUtilisateurAuthentifie(dto.getIdUtilisateur(), dto.getNomComplet());
            return TypeReponseServeur.AUTH_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + dto.getIdUtilisateur() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + dto.getNomComplet() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + dto.getRoleUtilisateur().name();
        } else {
            return TypeReponseServeur.AUTH_ECHEC.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + "Identifiants incorrects ou compte inactif.";
        }
    }

    private String traiterInscription(String[] tokens) {
        if (tokens.length < 4) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour INSCRIPTION.");
        String login = tokens[1];
        String mdp = tokens[2];
        String nom = tokens[3];
        ReponseGeneriqueDTO reponse = this.serviceAuthentification.inscrireNouvelUtilisateur(login, mdp, nom);
        if (reponse.isSucces()) {
            return TypeReponseServeur.INSCRIPTION_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return TypeReponseServeur.INSCRIPTION_ECHEC.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        }
    }

    private String traiterDeconnexion(ThreadClientDedie threadClient) {
        journal.info("Client {} (Utilisateur ID: {}) a demandé la déconnexion.", threadClient.getSocketClientCommunication().getRemoteSocketAddress(), threadClient.obtenirIdUtilisateurAuthentifie());
        threadClient.reinitialiserAuthentification();
        threadClient.setClientConnecte(false);
        return TypeReponseServeur.OPERATION_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + "Déconnexion réussie.";
    }

    private String traiterNouvelleReunion(String[] tokens, int idOrganisateur) {
        if (tokens.length < 6) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour NOUVELLE_REUNION.");
        String titre = tokens[1];
        String ordreDuJour = tokens[2];
        String dateHeureDebutStr = tokens[3];
        int dureeMinutes;
        TypeReunion typeReunion;
        String motDePasseOptionnel = (tokens.length > 6) ? tokens[6] : null;
        try {
            dureeMinutes = Integer.parseInt(tokens[4]);
            typeReunion = TypeReunion.valueOf(tokens[5].toUpperCase());
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "Durée en minutes invalide.");
        } catch (IllegalArgumentException e) {
            return formaterReponseErreur("PARAM_INVALID", "Type de réunion invalide.");
        }

        DetailsReunionDTO dto = this.serviceGestionReunions.creerNouvelleReunion(titre, ordreDuJour, dateHeureDebutStr, dureeMinutes, typeReunion, motDePasseOptionnel, idOrganisateur);
        if (dto != null) {
            return TypeReponseServeur.REUNION_CREEE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + construireChaineDetailsReunionDTO(dto);
        } else {
            return formaterReponseErreur("CREATION_REUNION_ECHEC", "Impossible de créer la réunion.");
        }
    }

    private String traiterObtenirReunions() throws ExceptionPersistance {
        List<DetailsReunionDTO> reunions = this.serviceGestionReunions.listerToutesLesReunionsActivesEtPlanifiees();
        return TypeReponseServeur.LISTE_REUNIONS.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + convertirListeDetailsReunionDTOEnChaineProtocole(reunions);
    }

    private String traiterObtenirMesReunions(int idUtilisateur) throws ExceptionPersistance {
        List<DetailsReunionDTO> reunions = this.serviceGestionReunions.listerReunionsParUtilisateur(idUtilisateur);
        return TypeReponseServeur.LISTE_MES_REUNIONS.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + convertirListeDetailsReunionDTOEnChaineProtocole(reunions);
    }

    private String traiterObtenirDetailsReunion(String[] tokens) throws ExceptionPersistance {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        DetailsReunionDTO dto = this.serviceGestionReunions.obtenirDetailsReunion(idReunion);
        if (dto != null) {
            return TypeReponseServeur.DETAILS_REUNION.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + construireChaineDetailsReunionDTOAvecParticipants(dto);
        } else {
            return formaterReponseErreur("REUNION_NON_TROUVEE", "Réunion non trouvée.");
        }
    }

    private String traiterRejoindreReunion(String[] tokens, ThreadClientDedie threadClient) throws ExceptionPersistance {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        String motDePasseFourni = (tokens.length > 2) ? tokens[2] : null;

        ReponseGeneriqueDTO reponse = this.serviceGestionReunions.rejoindreReunion(idReunion, threadClient.obtenirIdUtilisateurAuthentifie(), threadClient, motDePasseFourni);
        if (reponse.isSucces()) {
            DetailsReunionDTO detailsReunion = serviceGestionReunions.obtenirDetailsReunion(idReunion);
            String participantsChaine = (detailsReunion != null && detailsReunion.getParticipants() != null) ? convertirListeDonneesUtilisateurDTOEnChaineProtocole(detailsReunion.getParticipants()) : "";
            return TypeReponseServeur.REJOINDRE_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + participantsChaine;
        } else {
            return TypeReponseServeur.REJOINDRE_ECHEC.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        }
    }

    private String traiterQuitterReunion(String[] tokens, ThreadClientDedie threadClient) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        ReponseGeneriqueDTO reponse = this.serviceGestionReunions.quitterReunion(idReunion, threadClient.obtenirIdUtilisateurAuthentifie(), threadClient);
        if (reponse.isSucces()) {
            return TypeReponseServeur.QUITTER_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion;
        } else {
            return TypeReponseServeur.QUITTER_ECHEC.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        }
    }

    private String traiterMessageChat(String[] tokens, int idUtilisateurEmetteur) {
        if (tokens.length < 3) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion ou message manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide pour MESSAGE_CHAT.");
        }
        String contenuMessage = String.join(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE, Arrays.copyOfRange(tokens, 2, tokens.length));
        if (contenuMessage.trim().isEmpty()) {
            return formaterReponseErreur("PARAM_MANQUANT", "Le contenu du message ne peut pas être vide.");
        }
        this.serviceCommunicationReunion.traiterNouveauMessageChat(idReunion, idUtilisateurEmetteur, contenuMessage);
        return null;
    }

    private String traiterObtenirHistoriqueMessages(String[] tokens) throws ExceptionPersistance {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        List<MessageChatDTO> messages = serviceCommunicationReunion.recupererHistoriqueMessages(idReunion);
        return TypeReponseServeur.HISTORIQUE_MESSAGES.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + convertirListeMessageChatDTOEnChaineProtocole(messages);
    }

    private String traiterOuvrirReunion(String[] tokens, int idOrganisateurDemandeur) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        ReponseGeneriqueDTO reponse = serviceGestionReunions.ouvrirReunion(idReunion, idOrganisateurDemandeur);
        if (reponse.isSucces()) {
            return TypeReponseServeur.REUNION_OUVERTE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion;
        } else {
            return formaterReponseErreur("OUVERTURE_REUNION_ECHEC", reponse.getMessage());
        }
    }

    private String traiterCloreReunion(String[] tokens, int idOrganisateurDemandeur) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        int idReunion;
        try {
            idReunion = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID de réunion invalide.");
        }
        ReponseGeneriqueDTO reponse = serviceGestionReunions.cloreReunion(idReunion, idOrganisateurDemandeur);
        if (reponse.isSucces()) {
            return TypeReponseServeur.REUNION_CLOTUREE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + idReunion;
        } else {
            return formaterReponseErreur("CLOTURE_REUNION_ECHEC", reponse.getMessage());
        }
    }

    private String traiterModifierProfil(String[] tokens, int idUtilisateurDemandeur) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour MODIFIER_PROFIL.");
        String nouveauNomComplet = tokens[1];
        String nouvelIdentifiantConnexion = (tokens.length > 2) ? tokens[2] : null;

        ReponseGeneriqueDTO reponse = serviceGestionUtilisateurs.mettreAJourProfilUtilisateur(idUtilisateurDemandeur, nouveauNomComplet, nouvelIdentifiantConnexion);
        if (reponse.isSucces()) {
            return TypeReponseServeur.PROFIL_MODIFIE_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("MODIF_PROFIL_ECHEC", reponse.getMessage());
        }
    }

    private String traiterChangerMotDePasse(String[] tokens, int idUtilisateurDemandeur) {
        if (tokens.length < 3) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour CHANGER_MOT_DE_PASSE.");
        String motDePasseActuel = tokens[1];
        String nouveauMotDePasse = tokens[2];
        ReponseGeneriqueDTO reponse = serviceGestionUtilisateurs.changerMotDePasseUtilisateur(idUtilisateurDemandeur, motDePasseActuel, nouveauMotDePasse);
        if (reponse.isSucces()) {
            return TypeReponseServeur.MOT_DE_PASSE_MODIFIE_OK.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("CHANGER_MDP_ECHEC", reponse.getMessage());
        }
    }

    private String traiterObtenirMonProfil(int idUtilisateur) {
        Optional<DonneesUtilisateurDTO> dtoOpt = serviceGestionUtilisateurs.obtenirDonneesUtilisateur(idUtilisateur);
        if (dtoOpt.isPresent()) {
            return TypeReponseServeur.DONNEES_UTILISATEUR.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + construireChaineDonneesUtilisateurDTO(dtoOpt.get());
        } else {
            return formaterReponseErreur("PROFIL_NON_TROUVE", "Impossible de récupérer les données de votre profil.");
        }
    }

    private String traiterAdminObtenirUtilisateurs() throws ExceptionPersistance {
        List<DonneesUtilisateurDTO> utilisateurs = serviceAdministration.listerTousLesUtilisateurs();
        return TypeReponseServeur.LISTE_UTILISATEURS.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + convertirListeDonneesUtilisateurDTOEnChaineProtocole(utilisateurs);
    }

    private String traiterAdminCreerUtilisateur(String[] tokens) {
        if (tokens.length < 6) return formaterReponseErreur("PARAM_MANQUANT", "Paramètres manquants pour ADMIN_CREER_UTILISATEUR.");
        String login = tokens[1];
        String mdp = tokens[2];
        String nom = tokens[3];
        RoleUtilisateur role;
        StatutCompteUtilisateur statut;
        try {
            role = RoleUtilisateur.valueOf(tokens[4].toUpperCase());
            statut = StatutCompteUtilisateur.valueOf(tokens[5].toUpperCase());
        } catch (IllegalArgumentException e) {
            return formaterReponseErreur("PARAM_INVALID", "Rôle ou statut invalide.");
        }
        ReponseGeneriqueDTO reponse = serviceAdministration.creerUtilisateurParAdmin(login, mdp, nom, role, statut);
        if (reponse.isSucces()) {
            return TypeReponseServeur.ADMIN_UTILISATEUR_CREE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("ADMIN_CREATION_UTIL_ECHEC", reponse.getMessage());
        }
    }

    private String traiterAdminModifierUtilisateur(String[] tokens) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID utilisateur manquant.");
        int idUtilisateur;
        try {
            idUtilisateur = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID utilisateur invalide.");
        }
        String login = (tokens.length > 2 && !tokens[2].isEmpty()) ? tokens[2] : null;
        String nom = (tokens.length > 3 && !tokens[3].isEmpty()) ? tokens[3] : null;
        RoleUtilisateur role = null;
        StatutCompteUtilisateur statut = null;
        try {
            if (tokens.length > 4 && !tokens[4].isEmpty()) role = RoleUtilisateur.valueOf(tokens[4].toUpperCase());
            if (tokens.length > 5 && !tokens[5].isEmpty()) statut = StatutCompteUtilisateur.valueOf(tokens[5].toUpperCase());
        } catch (IllegalArgumentException e) {
            return formaterReponseErreur("PARAM_INVALID", "Rôle ou statut invalide pour modification admin.");
        }

        ReponseGeneriqueDTO reponse = serviceAdministration.modifierUtilisateurParAdmin(idUtilisateur, login, nom, role, statut);
        if (reponse.isSucces()) {
            return TypeReponseServeur.ADMIN_UTILISATEUR_MODIFIE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("ADMIN_MODIF_UTIL_ECHEC", reponse.getMessage());
        }
    }

    private String traiterAdminSupprimerUtilisateur(String[] tokens) {
        if (tokens.length < 2) return formaterReponseErreur("PARAM_MANQUANT", "ID utilisateur manquant.");
        int idUtilisateur;
        try {
            idUtilisateur = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            return formaterReponseErreur("PARAM_INVALID", "ID utilisateur invalide.");
        }
        ReponseGeneriqueDTO reponse = serviceAdministration.supprimerUtilisateurParAdmin(idUtilisateur);
        if (reponse.isSucces()) {
            return TypeReponseServeur.ADMIN_UTILISATEUR_SUPPRIME.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("ADMIN_SUPPR_UTIL_ECHEC", reponse.getMessage());
        }
    }

    private String traiterAdminObtenirConfig() {
        java.util.Map<String, String> config = serviceAdministration.obtenirConfigurationServeur();
        String configStr = config.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
        return TypeReponseServeur.ADMIN_CONFIG_LISTE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + configStr;
    }

    private String traiterAdminDefinirConfig(String[] tokens) {
        if (tokens.length < 3) return formaterReponseErreur("PARAM_MANQUANT", "Clé et valeur de configuration manquantes.");
        String cle = tokens[1];
        String valeur = tokens[2];
        ReponseGeneriqueDTO reponse = serviceAdministration.definirConfigurationServeur(cle, valeur);
        if (reponse.isSucces()) {
            return TypeReponseServeur.ADMIN_CONFIG_MODIFIEE.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + reponse.getMessage();
        } else {
            return formaterReponseErreur("ADMIN_MODIF_CONFIG_ECHEC", reponse.getMessage());
        }
    }

    private boolean estAuthentifie(ThreadClientDedie threadClient) {
        return threadClient.obtenirIdUtilisateurAuthentifie() != null;
    }

    private String reponseAccesRefuse() {
        return formaterReponseErreur("ACCES_REFUSE", "Authentification requise pour cette action.");
    }

    private boolean estAdmin(ThreadClientDedie threadClient) {
        if (!estAuthentifie(threadClient)) return false;
        Optional<DonneesUtilisateurDTO> dtoOpt = this.serviceGestionUtilisateurs.obtenirDonneesUtilisateur(threadClient.obtenirIdUtilisateurAuthentifie());
        return dtoOpt.isPresent() && dtoOpt.get().getRoleUtilisateur() == RoleUtilisateur.ADMINISTRATEUR;
    }

    private String reponseAccesRefuseAdmin() {
        return formaterReponseErreur("DROITS_INSUFFISANTS", "Droits administrateur requis.");
    }

    private String formaterReponseErreur(String codeErreur, String messageErreur) {
        return TypeReponseServeur.ERREUR.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + codeErreur + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + messageErreur;
    }

    private String construireChaineDetailsReunionDTO(DetailsReunionDTO dto) {
        return dto.getIdReunion() + ";" +
                dto.getTitre() + ";" +
                dto.getIdOrganisateur() + ";" +
                dto.getNomOrganisateur() + ";" +
                dto.getDateHeureDebut() + ";" +
                dto.getDureeMinutes() + ";" +
                dto.getTypeReunion().name() + ";" +
                dto.getStatutReunion().name() + ";" +
                dto.isMotDePasseOptionnelPresent();
    }

    private String construireChaineDetailsReunionDTOAvecParticipants(DetailsReunionDTO dto) {
        String base = construireChaineDetailsReunionDTO(dto);
        String participants = (dto.getParticipants() != null) ? convertirListeDonneesUtilisateurDTOEnChaineProtocole(dto.getParticipants()) : "";
        return base + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + participants;
    }

    private String convertirListeDetailsReunionDTOEnChaineProtocole(List<DetailsReunionDTO> reunions) {
        if (reunions == null || reunions.isEmpty()) return "";
        return reunions.stream()
                .map(this::construireChaineDetailsReunionDTO)
                .collect(Collectors.joining(ConstantesProtocoleBMO.DELIMITEUR_LISTE_ENTREES));
    }

    private String construireChaineDonneesUtilisateurDTO(DonneesUtilisateurDTO dto) {
        return dto.getIdUtilisateur() + ";" +
                dto.getIdentifiantConnexion() + ";" +
                dto.getNomComplet() + ";" +
                dto.getRoleUtilisateur().name() + ";" +
                (dto.getStatutCompte() != null ? dto.getStatutCompte().name() : "INCONNU") + ";" +
                (dto.getDateDerniereConnexion() != null ? dto.getDateDerniereConnexion() : "JAMAIS");
    }

    private String convertirListeDonneesUtilisateurDTOEnChaineProtocole(List<DonneesUtilisateurDTO> utilisateurs) {
        if (utilisateurs == null || utilisateurs.isEmpty()) return "";
        return utilisateurs.stream()
                .map(this::construireChaineDonneesUtilisateurDTO)
                .collect(Collectors.joining(ConstantesProtocoleBMO.DELIMITEUR_LISTE_ENTREES));
    }

    private String construireChaineMessageChatDTO(MessageChatDTO dto) {
        return dto.getIdMessage() + ";" +
                dto.getIdReunion() + ";" +
                dto.getIdUtilisateurEmetteur() + ";" +
                dto.getNomUtilisateurEmetteur() + ";" +
                dto.getHorodatage() + ";" +
                dto.getContenuMessage();
    }

    private String convertirListeMessageChatDTOEnChaineProtocole(List<MessageChatDTO> messages) {
        if (messages == null || messages.isEmpty()) return "";
        return messages.stream()
                .map(this::construireChaineMessageChatDTO)
                .collect(Collectors.joining(ConstantesProtocoleBMO.DELIMITEUR_LISTE_ENTREES));
    }
}