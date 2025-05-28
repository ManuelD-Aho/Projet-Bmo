package akandan.bahou.kassy.commun.modele;

/**
 * Énumération des rôles des utilisateurs au sein de l'application BMO.
 * Ces rôles déterminent les permissions et les fonctionnalités accessibles.
 */
public enum RoleUtilisateur {
    /**
     * Rôle d'administrateur, avec tous les droits sur l'application.
     */
    ADMINISTRATEUR,

    /**
     * Rôle d'organisateur, peut créer et gérer des réunions.
     */
    ORGANISATEUR,

    /**
     * Rôle de participant, peut rejoindre des réunions et interagir.
     */
    PARTICIPANT;
}