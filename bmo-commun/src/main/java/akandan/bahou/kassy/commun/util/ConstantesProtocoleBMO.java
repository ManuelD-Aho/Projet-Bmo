package akandan.bahou.kassy.commun.util;

public final class ConstantesProtocoleBMO {

    private ConstantesProtocoleBMO() {
    }

    public static final String DELIMITEUR_COMMANDE = "|";
    public static final String DELIMITEUR_FIN_MESSAGE = "\n";

    public static final String COMMANDE_CONNEXION = "CONNEXION";
    public static final String COMMANDE_INSCRIPTION = "INSCRIPTION";
    public static final String COMMANDE_NOUVELLE_REUNION = "NOUVELLE_REUNION";
    public static final String COMMANDE_REJOINDRE_REUNION = "REJOINDRE";
    public static final String COMMANDE_QUITTER_REUNION = "QUITTER";
    public static final String COMMANDE_MESSAGE_CHAT = "MESSAGE_CHAT";
    public static final String COMMANDE_DEMANDER_PAROLE = "DEMANDER_PAROLE";
    public static final String COMMANDE_AUTORISER_PAROLE = "AUTORISER_PAROLE";
    public static final String COMMANDE_REFUSER_PAROLE = "REFUSER_PAROLE";
    public static final String COMMANDE_CLOTURER_REUNION = "CLOTURER_REUNION";
    public static final String COMMANDE_DECONNEXION = "DECONNEXION";
    public static final String COMMANDE_OBTENIR_REUNIONS = "OBTENIR_REUNIONS";
    public static final String COMMANDE_ADMIN_OBTENIR_UTILISATEURS = "ADMIN_OBTENIR_UTILISATEURS";
    public static final String COMMANDE_ADMIN_CREER_UTILISATEUR = "ADMIN_CREER_UTILISATEUR";
    public static final String COMMANDE_ADMIN_MODIFIER_UTILISATEUR = "ADMIN_MODIFIER_UTILISATEUR";
    public static final String COMMANDE_ADMIN_SUPPRIMER_UTILISATEUR = "ADMIN_SUPPRIMER_UTILISATEUR";
    public static final String COMMANDE_ADMIN_OBTENIR_CONFIG = "ADMIN_OBTENIR_CONFIG";
    public static final String COMMANDE_ADMIN_DEFINIR_CONFIG = "ADMIN_DEFINIR_CONFIG";
    public static final String COMMANDE_MODIFIER_PROFIL = "MODIFIER_PROFIL";
    public static final String COMMANDE_MODIFIER_REUNION = "MODIFIER_REUNION";

    public static final String REPONSE_AUTH_OK = "AUTH_OK";
    public static final String REPONSE_AUTH_ECHEC = "AUTH_ECHEC";
    public static final String REPONSE_INSCRIPTION_OK = "INSCRIPTION_OK";
    public static final String REPONSE_INSCRIPTION_ECHEC = "INSCRIPTION_ECHEC";
    public static final String REPONSE_REUNION_CREEE = "REUNION_CREEE";
    public static final String NOTIFICATION_REUNION_MISE_A_JOUR = "REUNION_MISE_A_JOUR";
    public static final String NOTIFICATION_REUNION_SUPPRIMEE = "REUNION_SUPPRIMEE";
    public static final String REPONSE_LISTE_REUNIONS = "LISTE_REUNIONS";
    public static final String REPONSE_REJOINDRE_OK = "REJOINDRE_OK";
    public static final String REPONSE_REJOINDRE_ECHEC = "REJOINDRE_ECHEC";
    public static final String NOTIFICATION_NOUVEAU_MESSAGE_CHAT = "NOUVEAU_MESSAGE_CHAT";
    public static final String NOTIFICATION_UTILISATEUR_REJOINT_REUNION = "UTILISATEUR_REJOINT_REUNION";
    public static final String NOTIFICATION_UTILISATEUR_QUITTE_REUNION = "UTILISATEUR_QUITTE_REUNION";
    public static final String NOTIFICATION_DEMANDE_PAROLE_ENTRANTE = "DEMANDE_PAROLE_ENTRANTE";
    public static final String NOTIFICATION_PAROLE_ACCORDEE = "PAROLE_ACCORDEE";
    public static final String NOTIFICATION_PAROLE_REFUSEE = "PAROLE_REFUSEE";
    public static final String NOTIFICATION_REUNION_CLOTUREE = "NOTIFICATION_REUNION_CLOTUREE";
    public static final String REPONSE_VALEUR_CONFIG = "VALEUR_CONFIG";
    public static final String REPONSE_LISTE_UTILISATEURS = "LISTE_UTILISATEURS";
    public static final String REPONSE_ERREUR = "ERREUR";
    public static final String REPONSE_OPERATION_OK = "OPERATION_OK";
    public static final String REPONSE_OPERATION_ECHEC = "OPERATION_ECHEC";
}