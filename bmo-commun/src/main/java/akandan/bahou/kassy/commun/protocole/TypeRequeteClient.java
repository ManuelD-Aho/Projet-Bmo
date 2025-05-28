package akandan.bahou.kassy.commun.protocole;

import java.util.HashMap;
import java.util.Map;

public enum TypeRequeteClient {
    CONNEXION("CONNEXION"),
    INSCRIPTION("INSCRIPTION"),
    NOUVELLE_REUNION("NOUVELLE_REUNION"),
    REJOINDRE_REUNION("REJOINDRE"),
    QUITTER_REUNION("QUITTER"),
    MESSAGE_CHAT("MESSAGE_CHAT"),
    DEMANDER_PAROLE("DEMANDER_PAROLE"),
    AUTORISER_PAROLE("AUTORISER_PAROLE"),
    REFUSER_PAROLE("REFUSER_PAROLE"),
    CLOTURER_REUNION("CLOTURER_REUNION"),
    DECONNEXION("DECONNEXION"),
    OBTENIR_REUNIONS("OBTENIR_REUNIONS"),
    ADMIN_OBTENIR_UTILISATEURS("ADMIN_OBTENIR_UTILISATEURS"),
    ADMIN_CREER_UTILISATEUR("ADMIN_CREER_UTILISATEUR"),
    ADMIN_MODIFIER_UTILISATEUR("ADMIN_MODIFIER_UTILISATEUR"),
    ADMIN_SUPPRIMER_UTILISATEUR("ADMIN_SUPPRIMER_UTILISATEUR"),
    ADMIN_OBTENIR_CONFIG("ADMIN_OBTENIR_CONFIG"),
    ADMIN_DEFINIR_CONFIG("ADMIN_DEFINIR_CONFIG"),
    MODIFIER_PROFIL("MODIFIER_PROFIL"),
    MODIFIER_REUNION("MODIFIER_REUNION");

    private final String valeurProtocole;
    private static final Map<String, TypeRequeteClient> indexValeurProtocole = new HashMap<>();

    static {
        for (TypeRequeteClient type : TypeRequeteClient.values()) {
            indexValeurProtocole.put(type.getValeurProtocole(), type);
        }
    }

    TypeRequeteClient(String valeurProtocole) {
        this.valeurProtocole = valeurProtocole;
    }

    public String getValeurProtocole() {
        return valeurProtocole;
    }

    public static TypeRequeteClient depuisValeurProtocole(String valeur) {
        return indexValeurProtocole.get(valeur);
    }
}