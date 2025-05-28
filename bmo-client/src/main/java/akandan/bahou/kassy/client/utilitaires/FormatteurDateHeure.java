package akandan.bahou.kassy.commun.utilitaires;

/**
 * Classe utilitaire FormatteurDateHeure
 */
public final class FormatteurDateHeure {

    private FormatteurDateHeure() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Méthode utilitaire exemple
     */
    public static boolean valider(String entree) {
        return entree != null && !entree.trim().isEmpty();
    }
}
