package akandan.bahou.kassy.commun.modele;

/**
 * Énumération des rôles spécifiques qu'un utilisateur peut avoir au sein d'une réunion donnée dans BMO.
 * Ces valeurs correspondent généralement aux chaînes définies dans la base de données
 * (par exemple, dans la colonne `participants_reunion.role_dans_reunion`).
 */
public enum RoleDansReunion {
    /**
     * L'utilisateur est l'organisateur principal de la réunion, avec des droits de gestion étendus.
     */
    ORGANISATEUR,

    /**
     * L'utilisateur est un participant standard à la réunion.
     */
    PARTICIPANT,

    /**
     * L'utilisateur a un rôle d'animateur, pouvant avoir des droits spécifiques
     * pour la gestion de la parole ou d'autres aspects de la réunion.
     */
    ANIMATEUR;
}