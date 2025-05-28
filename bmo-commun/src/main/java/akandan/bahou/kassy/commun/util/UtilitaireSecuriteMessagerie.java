package akandan.bahou.kassy.commun.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public final class UtilitaireSecuriteMessagerie {

    private static final String ALGORITHME_HACHAGE = "SHA-256";
    private static final int LONGUEUR_SEL_OCTETS = 16;

    private UtilitaireSecuriteMessagerie() {
    }

    public static String hacherMotDePasse(String motDePasseEnClair, String selEnBase64) throws ExceptionCryptage {
        if (motDePasseEnClair == null || motDePasseEnClair.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe en clair ne peut pas être nul ou vide.");
        }
        if (selEnBase64 == null || selEnBase64.isEmpty()) {
            throw new IllegalArgumentException("Le sel en Base64 ne peut pas être nul ou vide.");
        }

        try {
            byte[] sel = Base64.getDecoder().decode(selEnBase64);
            MessageDigest digest = MessageDigest.getInstance(ALGORITHME_HACHAGE);
            digest.update(sel);
            byte[] motDePasseHache = digest.digest(motDePasseEnClair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(motDePasseHache);
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionCryptage("Algorithme de hachage non disponible.", e);
        } catch (IllegalArgumentException e) {
            throw new ExceptionCryptage("Erreur lors du décodage du sel en Base64.", e);
        }
    }

    public static String genererSelAleatoireEnBase64() throws ExceptionCryptage {
        try {
            SecureRandom random = new SecureRandom();
            byte[] sel = new byte[LONGUEUR_SEL_OCTETS];
            random.nextBytes(sel);
            return Base64.getEncoder().encodeToString(sel);
        } catch (Exception e) {
            throw new ExceptionCryptage("Erreur lors de la génération du sel aléatoire.", e);
        }
    }

    public static boolean verifierMotDePasse(String motDePasseEnClair, String motDePasseHacheStockeEnBase64, String selEnBase64) throws ExceptionCryptage {
        if (motDePasseEnClair == null || motDePasseHacheStockeEnBase64 == null || selEnBase64 == null) {
            // Il serait préférable de lever une IllegalArgumentException ou de retourner false directement
            // selon la politique de gestion des erreurs pour des entrées nulles.
            // Pour cet exercice, on propage une ExceptionCryptage si le hachage échoue.
            throw new IllegalArgumentException("Les entrées pour la vérification de mot de passe ne peuvent pas être nulles.");
        }
        String nouveauHachage = hacherMotDePasse(motDePasseEnClair, selEnBase64);
        return nouveauHachage.equals(motDePasseHacheStockeEnBase64);
    }
}