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
        chargerFichiersEtVariables();
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

    private void chargerFichiersEtVariables() {
        // 1. Charger depuis application.properties (valeurs par défaut)
        try (InputStream lInputStream = ConfigurateurServeur.class.getClassLoader().getResourceAsStream(NOM_FICHIER_PROPRIETES_APPLICATION)) {
            if (lInputStream != null) {
                this.proprietesConfiguration.load(lInputStream);
                journal.info("Propriétés de configuration initiales chargées depuis '{}'.", NOM_FICHIER_PROPRIETES_APPLICATION);
            } else {
                journal.warn("Le fichier de propriétés '{}' est introuvable dans le classpath. Utilisation des valeurs par défaut et des variables d'environnement uniquement.", NOM_FICHIER_PROPRIETES_APPLICATION);
            }
        } catch (IOException e) {
            journal.error("Erreur E/S lors du chargement de '{}'.", NOM_FICHIER_PROPRIETES_APPLICATION, e);
            // Ne pas lever d'exception ici, les variables d'env peuvent surcharger
        }

        // 2. Surcharger avec les variables d'environnement (plus prioritaires)
        //    Les clés dans application.properties doivent correspondre aux noms des variables d'environnement
        //    ou vous devez avoir un mapping.
        //    Exemple: Si docker-compose.yml a BMO_DB_URL, application.properties devrait avoir bmo.db.url
        //    et nous mappons BMO_DB_URL (variable d'env) à bmo.db.url (clé de propriété).

        surchargerDepuisVariableEnv("BMO_DB_URL", "bmo.db.url");
        surchargerDepuisVariableEnv("BMO_DB_UTILISATEUR", "bmo.db.utilisateur");
        surchargerDepuisVariableEnv("BMO_DB_MOTDEPASSE", "bmo.db.motdepasse");
        surchargerDepuisVariableEnv("BMO_SERVEUR_PORT", "bmo.serveur.port");
        surchargerDepuisVariableEnv("BMO_POOLTHREADS_TAILLEMAXIMALE", "bmo.poolthreads.taillemaximale");
        // Ajoutez d'autres mappings ici si nécessaire
    }

    private void surchargerDepuisVariableEnv(String nomVariableEnv, String clePropriete) {
        String valeurEnv = System.getenv(nomVariableEnv);
        if (valeurEnv != null && !valeurEnv.isEmpty()) {
            this.proprietesConfiguration.setProperty(clePropriete, valeurEnv);
            journal.info("Propriété '{}' surchargée par la variable d'environnement '{}'.", clePropriete, nomVariableEnv);
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