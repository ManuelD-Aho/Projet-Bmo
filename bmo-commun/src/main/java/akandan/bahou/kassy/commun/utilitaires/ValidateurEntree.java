package akandan.bahou.kassy.commun.utilitaires;

/**
 * Classe utilitaire ValidateurEntree
 */
public final class ValidateurEntree {

    private ValidateurEntree() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Méthode utilitaire exemple
     */
    public static boolean valider(String entree) {
        return entree != null && !entree.trim().isEmpty();
    }
}
