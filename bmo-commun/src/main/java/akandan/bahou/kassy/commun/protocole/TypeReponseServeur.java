package akandan.bahou.kassy.commun.protocole;

import java.util.HashMap;
import java.util.Map;

public enum TypeReponseServeur {
    AUTH_OK("AUTH_OK"),
    AUTH_ECHEC("AUTH_ECHEC"),
    INSCRIPTION_OK("INSCRIPTION_OK"),
    INSCRIPTION_ECHEC("INSCRIPTION_ECHEC"),
    REUNION_CREEE("REUNION_CREEE"),
    REUNION_MISE_A_JOUR("REUNION_MISE_A_JOUR"),
    REUNION_SUPPRIMEE("REUNION_SUPPRIMEE"),
    LISTE_REUNIONS("LISTE_REUNIONS"),
    REJOINDRE_OK("REJOINDRE_OK"),
    REJOINDRE_ECHEC("REJOINDRE_ECHEC"),
    NOUVEAU_MESSAGE_CHAT("NOUVEAU_MESSAGE_CHAT"),
    UTILISATEUR_REJOINT_REUNION("UTILISATEUR_REJOINT_REUNION"),
    UTILISATEUR_QUITTE_REUNION("UTILISATEUR_QUITTE_REUNION"),
    DEMANDE_PAROLE_ENTRANTE("DEMANDE_PAROLE_ENTRANTE"),
    PAROLE_ACCORDEE("PAROLE_ACCORDEE"),
    PAROLE_REFUSEE("PAROLE_REFUSEE"),
    NOTIFICATION_REUNION_CLOTUREE("NOTIFICATION_REUNION_CLOTUREE"),
    VALEUR_CONFIG("VALEUR_CONFIG"),
    LISTE_UTILISATEURS("LISTE_UTILISATEURS"),
    ERREUR("ERREUR"),
    OPERATION_OK("OPERATION_OK"),
    OPERATION_ECHEC("OPERATION_ECHEC");

    private final String valeurProtocole;
    private static final Map<String, TypeReponseServeur> indexValeurProtocole = new HashMap<>();

    static {
        for (TypeReponseServeur type : TypeReponseServeur.values()) {
            indexValeurProtocole.put(type.getValeurProtocole(), type);
        }
    }

    TypeReponseServeur(String valeurProtocole) {
        this.valeurProtocole = valeurProtocole;
    }

    public String getValeurProtocole() {
        return valeurProtocole;
    }

    public static TypeReponseServeur depuisValeurProtocole(String valeur) {
        return indexValeurProtocole.get(valeur);
    }
}