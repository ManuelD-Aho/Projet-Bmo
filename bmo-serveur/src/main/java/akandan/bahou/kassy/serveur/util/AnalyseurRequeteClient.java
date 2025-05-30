package akandan.bahou.kassy.serveur.util;

import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.protocole.TypeRequeteClient;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.serveur.noyau.ThreadClientDedie;
import akandan.bahou.kassy.serveur.service.ServiceAdministration;
import akandan.bahou.kassy.serveur.service.ServiceAuthentification;
import akandan.bahou.kassy.serveur.service.ServiceCommunicationReunion;
import akandan.bahou.kassy.serveur.service.ServiceGestionReunions;
import akandan.bahou.kassy.serveur.service.ServiceGestionUtilisateurs;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyseurRequeteClient {

    private final ServiceAuthentification serviceAuthentification;
    private final ServiceGestionUtilisateurs serviceGestionUtilisateurs;
    private final ServiceGestionReunions serviceGestionReunions;
    private final ServiceCommunicationReunion serviceCommunicationReunion;
    private final ServiceAdministration serviceAdministration;
    private static final Logger journal = LoggerFactory.getLogger(AnalyseurRequeteClient.class);

    public AnalyseurRequeteClient(ServiceAuthentification authService, ServiceGestionUtilisateurs userGestService, ServiceGestionReunions meetingGestService, ServiceCommunicationReunion commMeetingService, ServiceAdministration adminService) {
        this.serviceAuthentification = authService;
        this.serviceGestionUtilisateurs = userGestService;
        this.serviceGestionReunions = meetingGestService;
        this.serviceCommunicationReunion = commMeetingService;
        this.serviceAdministration = adminService;
    }

    private String formaterReponseJson(TypeReponseServeur typeReponse, Object objetPayload) throws JSONException {
        String payloadStr = "";
        if (objetPayload == null) {
            // Pour certains types de réponse, un payload null est acceptable (ex: OPERATION_OK)
            // Pour d'autres, cela pourrait être une erreur à gérer en amont.
        } else if (objetPayload instanceof JSONObject || objetPayload instanceof JSONArray) {
            payloadStr = objetPayload.toString();
        } else if (objetPayload instanceof DonneesUtilisateurDTO) {
            payloadStr = ((DonneesUtilisateurDTO) objetPayload).toJsonString();
        } else if (objetPayload instanceof DetailsReunionDTO) {
            payloadStr = ((DetailsReunionDTO) objetPayload).toJsonString();
        } else if (objetPayload instanceof MessageChatDTO) {
            payloadStr = ((MessageChatDTO) objetPayload).toJsonString();
        } else if (objetPayload instanceof ReponseGeneriqueDTO) {
            // ReponseGeneriqueDTO n'a pas de toJsonString, on prend son message
            payloadStr = ((ReponseGeneriqueDTO) objetPayload).getMessage();
            if (((ReponseGeneriqueDTO) objetPayload).getCodeErreur() != null) {
                payloadStr += ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + ((ReponseGeneriqueDTO) objetPayload).getCodeErreur();
            }
        } else if (objetPayload instanceof List) {
            JSONArray jsonArray = new JSONArray();
            for (Object item : (List<?>) objetPayload) {
                if (item instanceof DonneesUtilisateurDTO) {
                    jsonArray.put(new JSONObject(((DonneesUtilisateurDTO) item).toJsonString()));
                } else if (item instanceof DetailsReunionDTO) {
                    jsonArray.put(new JSONObject(((DetailsReunionDTO) item).toJsonString()));
                } else if (item instanceof MessageChatDTO) {
                    jsonArray.put(new JSONObject(((MessageChatDTO) item).toJsonString()));
                } else {
                    jsonArray.put(item.toString()); // Fallback pour autres types dans la liste
                }
            }
            payloadStr = jsonArray.toString();
        } else { // Pour les types simples comme Long, Integer, String, Boolean
            payloadStr = String.valueOf(objetPayload);
        }
        return typeReponse.getValeurProtocole() + (payloadStr.isEmpty() ? "" : ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + payloadStr);
    }

    private String formaterReponseErreur(String codeErreur, String messageErreur) {
        JSONObject erreurJson = new JSONObject();
        try {
            erreurJson.put("codeErreur", codeErreur);
            erreurJson.put("message", messageErreur);
        } catch (JSONException e) {
            // Should not happen with simple puts
            journal.error("Erreur interne de JSONException lors du formatage de l'erreur.", e);
        }
        return TypeReponseServeur.ERREUR.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + erreurJson.toString();
    }

    private boolean estAuthentifie(ThreadClientDedie threadClient) {
        return threadClient.obtenirIdUtilisateurAuthentifie() != null;
    }

    private String reponseAccesRefuse() {
        return formaterReponseErreur("ACCES_REFUSE", "Authentification requise pour cette action.");
    }

    private boolean estAdmin(ThreadClientDedie threadClient) {
        Long idUtilisateur = threadClient.obtenirIdUtilisateurAuthentifie();
        if (idUtilisateur == null) return false;
        Optional<DonneesUtilisateurDTO> dtoOpt = this.serviceGestionUtilisateurs.obtenirDonneesUtilisateur(idUtilisateur);
        return dtoOpt.isPresent() && dtoOpt.get().getRole() == RoleUtilisateur.ADMINISTRATEUR;
    }

    private String reponseAccesRefuseAdmin() {
        return formaterReponseErreur("DROITS_INSUFFISANTS", "Droits administrateur requis.");
    }

    public String traiterRequete(String requeteBrute, ThreadClientDedie threadClientConcerne) {
        if (requeteBrute == null || requeteBrute.trim().isEmpty()) {
            return formaterReponseErreur("REQUETE_VIDE", "La requête reçue est vide.");
        }
        String[] tokens = requeteBrute.split(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE, 2);
        String commandeStr = tokens[0].toUpperCase();
        String payloadJsonString = (tokens.length > 1) ? tokens[1] : null;
        TypeRequeteClient typeRequete;

        try {
            typeRequete = TypeRequeteClient.valueOf(commandeStr);
        } catch (IllegalArgumentException e) {
            return formaterReponseErreur("COMMANDE_INCONNUE", "La commande '" + commandeStr + "' est inconnue.");
        }

        journal.debug("Traitement requête: {} de {}, payload: {}", typeRequete, threadClientConcerne.getSocketClientCommunication().getRemoteSocketAddress(), payloadJsonString);
        JSONObject jsonPayload = null;
        if (payloadJsonString != null && !payloadJsonString.isEmpty() && !"null".equalsIgnoreCase(payloadJsonString)) {
            try {
                jsonPayload = new JSONObject(payloadJsonString);
            } catch (JSONException e) {
                journal.warn("Payload JSON invalide pour {}: {}. Erreur: {}", typeRequete.name(), payloadJsonString, e.getMessage());
                return formaterReponseErreur("PAYLOAD_JSON_INVALIDE", "Format des données incorrect.");
            }
        }

        try {
            switch (typeRequete) {
                case CONNEXION:
                    return traiterConnexion(jsonPayload, threadClientConcerne);
                case INSCRIPTION:
                    return traiterInscription(jsonPayload);
                case DECONNEXION:
                    return traiterDeconnexion(threadClientConcerne);
                case NOUVELLE_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterNouvelleReunion(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case OBTENIR_REUNIONS:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterObtenirReunions();
                case OBTENIR_MES_REUNIONS:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterObtenirMesReunions(threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case OBTENIR_DETAILS_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterObtenirDetailsReunion(jsonPayload);
                case REJOINDRE_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterRejoindreReunion(jsonPayload, threadClientConcerne);
                case QUITTER_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterQuitterReunion(jsonPayload, threadClientConcerne);
                case MESSAGE_CHAT:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterMessageChat(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case OBTENIR_HISTORIQUE_MESSAGES:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterObtenirHistoriqueMessages(jsonPayload);
                case OUVRIR_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterOuvrirReunion(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case CLOTURER_REUNION:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterCloreReunion(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case MODIFIER_PROFIL:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterModifierProfil(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case CHANGER_MOT_DE_PASSE:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterChangerMotDePasse(jsonPayload, threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case OBTENIR_MON_PROFIL:
                    if (!estAuthentifie(threadClientConcerne)) return reponseAccesRefuse();
                    return traiterObtenirMonProfil(threadClientConcerne.obtenirIdUtilisateurAuthentifie());
                case ADMIN_OBTENIR_UTILISATEURS:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminObtenirUtilisateurs();
                case ADMIN_CREER_UTILISATEUR:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminCreerUtilisateur(jsonPayload);
                case ADMIN_MODIFIER_UTILISATEUR:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminModifierUtilisateur(jsonPayload);
                case ADMIN_SUPPRIMER_UTILISATEUR:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminSupprimerUtilisateur(jsonPayload);
                case ADMIN_OBTENIR_CONFIG:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminObtenirConfig();
                case ADMIN_DEFINIR_CONFIG:
                    if (!estAdmin(threadClientConcerne)) return reponseAccesRefuseAdmin();
                    return traiterAdminDefinirConfig(jsonPayload);
                default:
                    journal.warn("Commande non gérée reçue: {}", typeRequete.name());
                    return formaterReponseErreur("COMMANDE_NON_GEREE", "La commande '" + typeRequete.name() + "' n'est pas encore gérée.");
            }
        } catch (JSONException e) {
            journal.warn("Erreur de parsing JSON pour la commande {} avec payload '{}' : {}", typeRequete.name(), payloadJsonString, e.getMessage());
            return formaterReponseErreur("PAYLOAD_JSON_INVALIDE", "Format de données JSON incorrect pour la commande: " + e.getMessage());
        } catch (ExceptionPersistance ep) {
            journal.error("Erreur de persistance lors du traitement de {} : {}", typeRequete.name(), ep.getMessage(), ep);
            return formaterReponseErreur("ERREUR_PERSISTANCE", "Une erreur de base de données est survenue.");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors du traitement de la commande {} : {}", typeRequete.name(), e.getMessage(), e);
            return formaterReponseErreur("ERREUR_INTERNE_ANALYSEUR", "Une erreur interne est survenue lors du traitement.");
        }
    }

    private String traiterConnexion(JSONObject payload, ThreadClientDedie threadClient) throws JSONException {
        if (payload == null) return formaterReponseErreur("PARAM_MANQUANT", "Données de connexion manquantes.");
        String identifiant = payload.getString("identifiant");
        String motDePasse = payload.getString("motDePasse");
        Optional<DonneesUtilisateurDTO> dtoOpt = this.serviceAuthentification.authentifierUtilisateur(identifiant, motDePasse);
        if (dtoOpt.isPresent()) {
            DonneesUtilisateurDTO dto = dtoOpt.get();
            threadClient.definirUtilisateurAuthentifie(dto.getIdUtilisateur(), dto.getNomComplet());
            return formaterReponseJson(TypeReponseServeur.AUTH_OK, dto);
        } else {
            return formaterReponseJson(TypeReponseServeur.AUTH_ECHEC, "Identifiants incorrects ou compte inactif.");
        }
    }

    private String traiterInscription(JSONObject payload) throws JSONException {
        if (payload == null) return formaterReponseErreur("PARAM_MANQUANT", "Données d'inscription manquantes.");
        String identifiant = payload.getString("identifiant");
        String motDePasse = payload.getString("motDePasse");
        String nomComplet = payload.getString("nomComplet");
        ReponseGeneriqueDTO reponse = this.serviceAuthentification.inscrireNouvelUtilisateur(identifiant, motDePasse, nomComplet);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.INSCRIPTION_OK : TypeReponseServeur.INSCRIPTION_ECHEC, reponse);
    }

    private String traiterDeconnexion(ThreadClientDedie threadClient) throws JSONException {
        Long idUtilisateur = threadClient.obtenirIdUtilisateurAuthentifie();
        journal.info("Client {} (Utilisateur ID: {}) a demandé la déconnexion.", threadClient.getSocketClientCommunication().getRemoteSocketAddress(), idUtilisateur);
        threadClient.reinitialiserAuthentification();
        threadClient.setClientConnecte(false);
        return formaterReponseJson(TypeReponseServeur.OPERATION_OK, "Déconnexion réussie.");
    }

    private String traiterNouvelleReunion(JSONObject payload, Long idOrganisateur) throws JSONException {
        if (payload == null) return formaterReponseErreur("PARAM_MANQUANT", "Données de réunion manquantes.");
        if (idOrganisateur == null) return reponseAccesRefuse();
        DetailsReunionDTO dtoRequete = DetailsReunionDTO.fromJson(payload.toString());
        Optional<DetailsReunionDTO> dtoCreeOpt = this.serviceGestionReunions.creerNouvelleReunion(
                dtoRequete.getTitre(), dtoRequete.getDescription(), dtoRequete.getDateHeureDebut(),
                dtoRequete.getDureeEstimeeMinutes(), dtoRequete.getTypeReunion(),
                dtoRequete.getMotDePasseOptionnelValeur(), idOrganisateur);
        if (dtoCreeOpt.isPresent()) {
            return formaterReponseJson(TypeReponseServeur.REUNION_CREEE, dtoCreeOpt.get());
        } else {
            return formaterReponseErreur("CREATION_REUNION_ECHEC", "Impossible de créer la réunion (vérifiez les logs serveur pour détails).");
        }
    }

    private String traiterObtenirReunions() throws ExceptionPersistance, JSONException {
        List<DetailsReunionDTO> reunions = this.serviceGestionReunions.listerToutesLesReunionsActivesEtPlanifiees();
        return formaterReponseJson(TypeReponseServeur.LISTE_REUNIONS, reunions);
    }

    private String traiterObtenirMesReunions(Long idUtilisateur) throws ExceptionPersistance, JSONException {
        if (idUtilisateur == null) return reponseAccesRefuse();
        List<DetailsReunionDTO> reunions = this.serviceGestionReunions.listerReunionsParOrganisateur(idUtilisateur);
        return formaterReponseJson(TypeReponseServeur.LISTE_MES_REUNIONS, reunions);
    }

    private String traiterObtenirDetailsReunion(JSONObject payload) throws ExceptionPersistance, JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        long idReunion = payload.getLong("idReunion");
        Optional<DetailsReunionDTO> dtoOpt = this.serviceGestionReunions.obtenirDetailsReunion(idReunion);
        if (dtoOpt.isPresent()) {
            return formaterReponseJson(TypeReponseServeur.DETAILS_REUNION, dtoOpt.get());
        } else {
            return formaterReponseErreur("REUNION_NON_TROUVEE", "Réunion non trouvée pour ID: " + idReunion);
        }
    }

    private String traiterRejoindreReunion(JSONObject payload, ThreadClientDedie threadClient) throws JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        long idReunion = payload.getLong("idReunion");
        String motDePasseFourni = payload.optString("motDePasseFourni", null);
        Long idUtilisateur = threadClient.obtenirIdUtilisateurAuthentifie();
        if (idUtilisateur == null) return reponseAccesRefuse();
        ReponseGeneriqueDTO reponse = this.serviceGestionReunions.rejoindreReunion(idReunion, idUtilisateur, threadClient, motDePasseFourni);
        if (reponse.isSucces()) {
            try {
                Optional<DetailsReunionDTO> dtoOpt = serviceGestionReunions.obtenirDetailsReunion(idReunion);
                return formaterReponseJson(TypeReponseServeur.REJOINDRE_OK, dtoOpt.orElse(null));
            } catch (ExceptionPersistance e) {
                journal.error("Erreur récupération détails réunion après REJOINDRE_OK: {}", e.getMessage());
                return formaterReponseJson(TypeReponseServeur.REJOINDRE_OK, new JSONObject().put("message", reponse.getMessage()).put("details_error", true).toString());
            }
        } else {
            return formaterReponseJson(TypeReponseServeur.REJOINDRE_ECHEC, reponse.getMessage());
        }
    }

    private String traiterQuitterReunion(JSONObject payload, ThreadClientDedie threadClient) throws JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        long idReunion = payload.getLong("idReunion");
        Long idUtilisateur = threadClient.obtenirIdUtilisateurAuthentifie();
        if (idUtilisateur == null) return reponseAccesRefuse();
        ReponseGeneriqueDTO reponse = this.serviceGestionReunions.quitterReunion(idReunion, idUtilisateur, threadClient);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.QUITTER_OK : TypeReponseServeur.QUITTER_ECHEC, reponse.getMessage());
    }

    private String traiterMessageChat(JSONObject payload, Long idUtilisateurEmetteur) throws JSONException {
        if (payload == null || !payload.has("idReunion") || !payload.has("contenu")) return formaterReponseErreur("PARAM_MANQUANT", "Données de message chat manquantes.");
        if (idUtilisateurEmetteur == null) return reponseAccesRefuse();
        MessageChatDTO messageRecu = MessageChatDTO.fromJson(payload.toString());
        this.serviceCommunicationReunion.traiterNouveauMessageChat(messageRecu.getIdReunion(), idUtilisateurEmetteur, messageRecu.getContenu());
        return null;
    }

    private String traiterObtenirHistoriqueMessages(JSONObject payload) throws ExceptionPersistance, JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        long idReunion = payload.getLong("idReunion");
        List<MessageChatDTO> messages = serviceCommunicationReunion.recupererHistoriqueMessages(idReunion);
        return formaterReponseJson(TypeReponseServeur.HISTORIQUE_MESSAGES, messages);
    }

    private String traiterOuvrirReunion(JSONObject payload, Long idOrganisateur) throws JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        if (idOrganisateur == null) return reponseAccesRefuse();
        long idReunion = payload.getLong("idReunion");
        ReponseGeneriqueDTO reponse = serviceGestionReunions.ouvrirReunion(idReunion, idOrganisateur);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.REUNION_OUVERTE : TypeReponseServeur.ERREUR, reponse.isSucces() ? String.valueOf(idReunion) : reponse.getMessage());
    }

    private String traiterCloreReunion(JSONObject payload, Long idOrganisateur) throws JSONException {
        if (payload == null || !payload.has("idReunion")) return formaterReponseErreur("PARAM_MANQUANT", "ID de réunion manquant.");
        if (idOrganisateur == null) return reponseAccesRefuse();
        long idReunion = payload.getLong("idReunion");
        ReponseGeneriqueDTO reponse = serviceGestionReunions.cloreReunion(idReunion, idOrganisateur);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.REUNION_CLOTUREE : TypeReponseServeur.ERREUR, reponse.isSucces() ? String.valueOf(idReunion) : reponse.getMessage());
    }

    private String traiterModifierProfil(JSONObject payload, Long idUtilisateur) throws JSONException {
        if (payload == null) return formaterReponseErreur("PARAM_MANQUANT", "Données de profil manquantes.");
        if (idUtilisateur == null) return reponseAccesRefuse();
        String nomComplet = payload.optString("nomComplet", null);
        String identifiant = payload.optString("identifiant", null);
        ReponseGeneriqueDTO reponse = serviceGestionUtilisateurs.mettreAJourProfilUtilisateur(idUtilisateur, nomComplet, identifiant);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.PROFIL_MODIFIE_OK : TypeReponseServeur.ERREUR, reponse.getMessage());
    }

    private String traiterChangerMotDePasse(JSONObject payload, Long idUtilisateur) throws JSONException {
        if (payload == null || !payload.has("motDePasseActuel") || !payload.has("nouveauMotDePasse")) return formaterReponseErreur("PARAM_MANQUANT", "Mots de passe manquants.");
        if (idUtilisateur == null) return reponseAccesRefuse();
        String motDePasseActuel = payload.getString("motDePasseActuel");
        String nouveauMotDePasse = payload.getString("nouveauMotDePasse");
        ReponseGeneriqueDTO reponse = serviceGestionUtilisateurs.changerMotDePasseUtilisateur(idUtilisateur, motDePasseActuel, nouveauMotDePasse);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.MOT_DE_PASSE_MODIFIE_OK : TypeReponseServeur.ERREUR, reponse.getMessage());
    }

    private String traiterObtenirMonProfil(Long idUtilisateur) throws JSONException {
        if (idUtilisateur == null) return reponseAccesRefuse();
        Optional<DonneesUtilisateurDTO> dtoOpt = serviceGestionUtilisateurs.obtenirDonneesUtilisateur(idUtilisateur);
        if (dtoOpt.isPresent()) {
            return formaterReponseJson(TypeReponseServeur.DONNEES_UTILISATEUR, dtoOpt.get());
        } else {
            return formaterReponseErreur("PROFIL_NON_TROUVE", "Profil utilisateur non trouvé.");
        }
    }

    private String traiterAdminObtenirUtilisateurs() throws ExceptionPersistance, JSONException {
        List<DonneesUtilisateurDTO> utilisateurs = serviceAdministration.listerTousLesUtilisateurs();
        return formaterReponseJson(TypeReponseServeur.LISTE_UTILISATEURS, utilisateurs);
    }

    private String traiterAdminCreerUtilisateur(JSONObject payload) throws JSONException {
        if (payload == null) return formaterReponseErreur("PARAM_MANQUANT", "Données de création admin manquantes.");
        String identifiant = payload.getString("identifiant");
        String motDePasse = payload.getString("motDePasse");
        String nomComplet = payload.getString("nomComplet");
        RoleUtilisateur role = RoleUtilisateur.valueOf(payload.getString("role"));
        StatutCompteUtilisateur statut = StatutCompteUtilisateur.valueOf(payload.optString("statutCompte", StatutCompteUtilisateur.ACTIF.name()));
        ReponseGeneriqueDTO reponse = serviceAdministration.creerUtilisateurParAdmin(identifiant, motDePasse, nomComplet, role, statut);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.ADMIN_UTILISATEUR_CREE : TypeReponseServeur.ERREUR, reponse.getMessage());
    }

    private String traiterAdminModifierUtilisateur(JSONObject payload) throws JSONException {
        if (payload == null || !payload.has("idUtilisateur")) return formaterReponseErreur("PARAM_MANQUANT", "ID utilisateur manquant pour modification admin.");
        long idUtilisateur = payload.getLong("idUtilisateur");
        String identifiant = payload.optString("identifiant", null);
        String nomComplet = payload.optString("nomComplet", null);
        RoleUtilisateur role = payload.has("role") && !payload.isNull("role") ? RoleUtilisateur.valueOf(payload.getString("role")) : null;
        StatutCompteUtilisateur statut = payload.has("statutCompte") && !payload.isNull("statutCompte") ? StatutCompteUtilisateur.valueOf(payload.getString("statutCompte")) : null;
        ReponseGeneriqueDTO reponse = serviceAdministration.modifierUtilisateurParAdmin(idUtilisateur, identifiant, nomComplet, role, statut);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.ADMIN_UTILISATEUR_MODIFIE : TypeReponseServeur.ERREUR, reponse.getMessage());
    }

    private String traiterAdminSupprimerUtilisateur(JSONObject payload) throws JSONException {
        if (payload == null || !payload.has("idUtilisateur")) return formaterReponseErreur("PARAM_MANQUANT", "ID utilisateur manquant pour suppression admin.");
        long idUtilisateur = payload.getLong("idUtilisateur");
        ReponseGeneriqueDTO reponse = serviceAdministration.supprimerUtilisateurParAdmin(idUtilisateur);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.ADMIN_UTILISATEUR_SUPPRIME : TypeReponseServeur.ERREUR, reponse.getMessage());
    }

    private String traiterAdminObtenirConfig() throws JSONException {
        Map<String, String> config = serviceAdministration.obtenirConfigurationServeur();
        return formaterReponseJson(TypeReponseServeur.ADMIN_CONFIG_LISTE, new JSONObject(config));
    }

    private String traiterAdminDefinirConfig(JSONObject payload) throws JSONException {
        if (payload == null || !payload.has("cle") || !payload.has("valeur")) return formaterReponseErreur("PARAM_MANQUANT", "Clé et valeur de configuration manquantes.");
        String cle = payload.getString("cle");
        String valeur = payload.getString("valeur");
        ReponseGeneriqueDTO reponse = serviceAdministration.definirConfigurationServeur(cle, valeur);
        return formaterReponseJson(reponse.isSucces() ? TypeReponseServeur.ADMIN_CONFIG_MODIFIEE : TypeReponseServeur.ERREUR, reponse.getMessage());
    }
}