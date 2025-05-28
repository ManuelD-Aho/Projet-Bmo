package akandan.bahou.kassy.commun.util;

import java.util.regex.Pattern;

public final class ValidateurEntreeUtilisateur {

    private static final Pattern PATRON_COURRIEL = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;
    private static final int LONGUEUR_MAX_NOM_UTILISATEUR = 50;
    private static final int LONGUEUR_MIN_IDENTIFIANT_CONNEXION = 3;
    private static final int LONGUEUR_MAX_IDENTIFIANT_CONNEXION = 50;


    private ValidateurEntreeUtilisateur() {
    }

    public static void validerNonNulOuVide(String entree, String nomChamp) throws ExceptionValidation {
        if (entree == null || entree.trim().isEmpty()) {
            throw new ExceptionValidation(nomChamp + " ne peut pas être vide.");
        }
    }

    public static void validerLongueurMaximale(String entree, int longueurMax, String nomChamp) throws ExceptionValidation {
        if (entree != null && entree.length() > longueurMax) {
            throw new ExceptionValidation(nomChamp + " ne doit pas dépasser " + longueurMax + " caractères.");
        }
    }

    public static void validerFormatCourriel(String courriel, String nomChamp) throws ExceptionValidation {
        validerNonNulOuVide(courriel, nomChamp);
        if (!PATRON_COURRIEL.matcher(courriel).matches()) {
            throw new ExceptionValidation(nomChamp + " n'est pas une adresse courriel valide.");
        }
    }

    public static void validerComplexiteMotDePasse(String motDePasse, String nomChamp) throws ExceptionValidation {
        validerNonNulOuVide(motDePasse, nomChamp);
        if (motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new ExceptionValidation(nomChamp + " doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        }
        // Ajouter d'autres règles si nécessaire :
        // if (!motDePasse.matches(".*[A-Z].*")) { throw new ExceptionValidation(nomChamp + " doit contenir au moins une majuscule."); }
        // if (!motDePasse.matches(".*[a-z].*")) { throw new ExceptionValidation(nomChamp + " doit contenir au moins une minuscule."); }
        // if (!motDePasse.matches(".*[0-9].*")) { throw new ExceptionValidation(nomChamp + " doit contenir au moins un chiffre."); }
    }

    public static void validerIdentifiantConnexion(String identifiant, String nomChamp) throws ExceptionValidation {
        validerNonNulOuVide(identifiant, nomChamp);
        if (identifiant.length() < LONGUEUR_MIN_IDENTIFIANT_CONNEXION) {
            throw new ExceptionValidation(nomChamp + " doit contenir au moins " + LONGUEUR_MIN_IDENTIFIANT_CONNEXION + " caractères.");
        }
        if (identifiant.length() > LONGUEUR_MAX_IDENTIFIANT_CONNEXION) {
            throw new ExceptionValidation(nomChamp + " ne doit pas dépasser " + LONGUEUR_MAX_IDENTIFIANT_CONNEXION + " caractères.");
        }
        if (identifiant.contains(" ")) {
            throw new ExceptionValidation(nomChamp + " ne doit pas contenir d'espaces.");
        }
        // Ajouter d'autres validations si nécessaire (ex: caractères autorisés)
    }
}