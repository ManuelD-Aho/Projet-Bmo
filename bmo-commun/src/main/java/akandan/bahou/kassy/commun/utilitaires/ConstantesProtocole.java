package akandan.bahou.kassy.commun.utilitaires;

/**
 * Classe utilitaire ConstantesProtocole
 */
public final class ConstantesProtocole {

    private ConstantesProtocole() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Méthode utilitaire exemple
     */
    public static boolean valider(String entree) {
        return entree != null && !entree.trim().isEmpty();
    }
}
