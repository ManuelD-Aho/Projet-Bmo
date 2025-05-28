package akandan.bahou.kassy.commun.utilitaires;

/**
 * Classe utilitaire UtilitaireExportationBDD
 */
public final class UtilitaireExportationBDD {

    private UtilitaireExportationBDD() {
        // Classe utilitaire - constructeur privé
    }

    /**
     * Méthode utilitaire exemple
     */
    public static boolean valider(String entree) {
        return entree != null && !entree.trim().isEmpty();
    }
}
