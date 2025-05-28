package akandan.bahou.kassy.serveur.configuration;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurateurServeur {

    private static volatile ConfigurateurServeur instanceUnique;
    private static final String NOM_FICHIER_PROPRIETES_APPLICATION = "application.properties";
    private static final Logger journal = LoggerFactory.getLogger(ConfigurateurServeur.class);

    private final Properties proprietesConfiguration;

    private ConfigurateurServeur() {
        this.proprietesConfiguration = new Properties();
        chargerFichierDeProprietes();
    }

    public static ConfigurateurServeur obtenirInstance() {
        if (instanceUnique == null) {
            synchronized (ConfigurateurServeur.class) {
                if (instanceUnique == null) {
                    instanceUnique = new ConfigurateurServeur();
                }
            }
        }
        return instanceUnique;
    }

    private void chargerFichierDeProprietes() {
        try (InputStream lInputStream = ConfigurateurServeur.class.getClassLoader().getResourceAsStream(NOM_FICHIER_PROPRIETES_APPLICATION)) {
            if (lInputStream == null) {
                journal.error("Le fichier de propriétés '{}' est introuvable dans le classpath.", NOM_FICHIER_PROPRIETES_APPLICATION);
                throw new RuntimeException("Fichier de configuration " + NOM_FICHIER_PROPRIETES_APPLICATION + " introuvable.");
            }
            this.proprietesConfiguration.load(lInputStream);
            journal.info("Les propriétés de configuration ont été chargées depuis '{}'.", NOM_FICHIER_PROPRIETES_APPLICATION);
        } catch (IOException e) {
            journal.error("Erreur d'entrée/sortie lors du chargement du fichier de propriétés '{}'.", NOM_FICHIER_PROPRIETES_APPLICATION, e);
            throw new RuntimeException("Erreur lors du chargement du fichier de configuration " + NOM_FICHIER_PROPRIETES_APPLICATION + ".", e);
        }
    }

    public String recupererProprieteChaine(String cleDePropriete) {
        return this.proprietesConfiguration.getProperty(cleDePropriete);
    }

    public String recupererProprieteChaine(String cleDePropriete, String valeurParDefaut) {
        return this.proprietesConfiguration.getProperty(cleDePropriete, valeurParDefaut);
    }

    public int recupererProprieteEntier(String cleDePropriete, int valeurParDefaut) {
        String valeurChaine = recupererProprieteChaine(cleDePropriete);
        if (valeurChaine != null && !valeurChaine.trim().isEmpty()) {
            try {
                return Integer.parseInt(valeurChaine.trim());
            } catch (NumberFormatException e) {
                journal.warn("La valeur pour la clé '{}' n'est pas un entier valide : '{}'. Utilisation de la valeur par défaut : {}.", cleDePropriete, valeurChaine, valeurParDefaut);
                return valeurParDefaut;
            }
        }
        return valeurParDefaut;
    }

    public boolean recupererProprieteBooleen(String cleDePropriete, boolean valeurParDefaut) {
        String valeurChaine = recupererProprieteChaine(cleDePropriete);
        if (valeurChaine != null) {
            return "true".equalsIgnoreCase(valeurChaine.trim());
        }
        return valeurParDefaut;
    }
}