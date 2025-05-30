package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.ExceptionCryptage;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.UtilitaireSecuriteMessagerie;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.serveur.configuration.ConfigurateurServeur;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceAdministration {

    private final InterfaceUtilisateurDAO utilisateurDAO;
    private final ConfigurateurServeur configurateurServeur;
    private static final Logger journal = LoggerFactory.getLogger(ServiceAdministration.class);

    public ServiceAdministration(InterfaceUtilisateurDAO utilisateurDAO, ConfigurateurServeur configurateurServeur) {
        this.utilisateurDAO = utilisateurDAO;
        this.configurateurServeur = configurateurServeur;
    }

    private DonneesUtilisateurDTO convertirEntiteUtilisateurVersDTO(Utilisateur entite) {
        if (entite == null) {
            return null;
        }
        return new DonneesUtilisateurDTO(
                entite.getId(),
                entite.getIdentifiant(),
                entite.getNomComplet(),
                entite.getRole(),
                entite.getStatutCompte(),
                entite.getDateCreationCompte(),
                entite.getDerniereConnexion()
        );
    }

    public List<DonneesUtilisateurDTO> listerTousLesUtilisateurs() throws ExceptionPersistance {
        List<Utilisateur> entitesUtilisateur = this.utilisateurDAO.trouverTous();
        return entitesUtilisateur.stream()
                .map(this::convertirEntiteUtilisateurVersDTO)
                .collect(Collectors.toList());
    }

    public ReponseGeneriqueDTO creerUtilisateurParAdmin(String identifiant, String motDePasseInitial, String nomComplet, RoleUtilisateur role, StatutCompteUtilisateur statutInitial) {
        try {
            ValidateurEntreeUtilisateur.validerIdentifiantConnexion(identifiant, "Identifiant de connexion");
            ValidateurEntreeUtilisateur.validerComplexiteMotDePasse(motDePasseInitial, "Mot de passe initial");
            ValidateurEntreeUtilisateur.validerNonNulOuVide(nomComplet, "Nom complet");

            if (this.utilisateurDAO.existeParIdentifiantConnexion(identifiant)) {
                return new ReponseGeneriqueDTO(false, "L'identifiant de connexion existe déjà.", "ID_CONN_EXISTANT");
            }

            String sel = UtilitaireSecuriteMessagerie.genererSelAleatoireEnBase64();
            String motDePasseHache = UtilitaireSecuriteMessagerie.hacherMotDePasse(motDePasseInitial, sel);

            Utilisateur nouvelUtilisateur = new Utilisateur();
            nouvelUtilisateur.setIdentifiant(identifiant);
            nouvelUtilisateur.setMotDePasseHache(motDePasseHache);
            nouvelUtilisateur.setSelMotDePasse(sel);
            nouvelUtilisateur.setNomComplet(nomComplet);
            nouvelUtilisateur.setRole(role);
            nouvelUtilisateur.setStatutCompte(statutInitial);
            nouvelUtilisateur.setDateCreationCompte(LocalDateTime.now());

            Utilisateur utilisateurCree = this.utilisateurDAO.creer(nouvelUtilisateur);
            journal.info("Nouvel utilisateur '{}' (ID: {}) créé par un administrateur avec le rôle '{}'.", identifiant, utilisateurCree.getId(), role.name());
            return new ReponseGeneriqueDTO(true, "Utilisateur créé avec succès.");

        } catch (ExceptionValidation | ExceptionCryptage | ExceptionPersistance e) {
            journal.error("Erreur lors de la création de l'utilisateur '{}' par admin : {}", identifiant, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur lors de la création de l'utilisateur: " + e.getMessage(), "CREATION_UTIL_ERREUR");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de la création de l'utilisateur '{}' par admin : {}", identifiant, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur inattendue: " + e.getMessage(), "ERREUR_INATTENDUE_CREATION_UTIL");
        }
    }

    public ReponseGeneriqueDTO modifierUtilisateurParAdmin(long idUtilisateurAModifier, String nouvelIdentifiant, String nouveauNomComplet, RoleUtilisateur nouveauRole, StatutCompteUtilisateur nouveauStatutCompte) {
        try {
            Optional<Utilisateur> utilisateurOpt = this.utilisateurDAO.trouverParId(idUtilisateurAModifier);
            if (utilisateurOpt.isEmpty()) {
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }

            Utilisateur utilisateurAModifier = utilisateurOpt.get();
            boolean aEteModifie = false;

            if (nouvelIdentifiant != null && !nouvelIdentifiant.trim().isEmpty() && !nouvelIdentifiant.trim().equals(utilisateurAModifier.getIdentifiant())) {
                ValidateurEntreeUtilisateur.validerIdentifiantConnexion(nouvelIdentifiant.trim(), "Nouvel identifiant de connexion");
                if (this.utilisateurDAO.existeParIdentifiantConnexion(nouvelIdentifiant.trim())) {
                    return new ReponseGeneriqueDTO(false, "Le nouvel identifiant de connexion est déjà utilisé par un autre compte.", "ID_CONN_EXISTANT");
                }
                utilisateurAModifier.setIdentifiant(nouvelIdentifiant.trim());
                aEteModifie = true;
            }

            if (nouveauNomComplet != null && !nouveauNomComplet.trim().isEmpty() && !nouveauNomComplet.trim().equals(utilisateurAModifier.getNomComplet())) {
                ValidateurEntreeUtilisateur.validerNonNulOuVide(nouveauNomComplet.trim(), "Nouveau nom complet");
                utilisateurAModifier.setNomComplet(nouveauNomComplet.trim());
                aEteModifie = true;
            }

            if (nouveauRole != null && nouveauRole != utilisateurAModifier.getRole()) {
                utilisateurAModifier.setRole(nouveauRole);
                aEteModifie = true;
            }

            if (nouveauStatutCompte != null && nouveauStatutCompte != utilisateurAModifier.getStatutCompte()) {
                utilisateurAModifier.setStatutCompte(nouveauStatutCompte);
                aEteModifie = true;
            }

            if (aEteModifie) {
                this.utilisateurDAO.mettreAJour(utilisateurAModifier);
                journal.info("Utilisateur ID {} modifié par un administrateur.", idUtilisateurAModifier);
                return new ReponseGeneriqueDTO(true, "Utilisateur modifié avec succès.");
            } else {
                return new ReponseGeneriqueDTO(true, "Aucune modification n'a été apportée.");
            }

        } catch (ExceptionValidation | ExceptionPersistance e) {
            journal.error("Erreur lors de la modification de l'utilisateur ID {} par admin : {}", idUtilisateurAModifier, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur lors de la modification de l'utilisateur: " + e.getMessage(), "MODIF_UTIL_ERREUR");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de la modification de l'utilisateur ID {} par admin : {}", idUtilisateurAModifier, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur inattendue: " + e.getMessage(), "ERREUR_INATTENDUE_MODIF_UTIL");
        }
    }

    public ReponseGeneriqueDTO supprimerUtilisateurParAdmin(long idUtilisateurASupprimer) {
        try {
            if (this.utilisateurDAO.trouverParId(idUtilisateurASupprimer).isEmpty()) {
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }

            boolean supprime = this.utilisateurDAO.supprimer(idUtilisateurASupprimer);
            if (supprime) {
                journal.info("Utilisateur ID {} supprimé par un administrateur.", idUtilisateurASupprimer);
                return new ReponseGeneriqueDTO(true, "Utilisateur supprimé avec succès.");
            } else {
                return new ReponseGeneriqueDTO(false, "La suppression de l'utilisateur a échoué.", "SUPPR_UTIL_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la suppression de l'utilisateur ID {} par admin : {}", idUtilisateurASupprimer, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur de persistance: " + e.getMessage(), "SUPPR_UTIL_ERREUR_PERSIST");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de la suppression de l'utilisateur ID {} par admin : {}", idUtilisateurASupprimer, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur inattendue: " + e.getMessage(), "ERREUR_INATTENDUE_SUPPR_UTIL");
        }
    }

    public Map<String, String> obtenirConfigurationServeur() {
        Map<String, String> configAffichee = new HashMap<>();
        configAffichee.put("bmo.serveur.port", String.valueOf(configurateurServeur.recupererProprieteEntier("bmo.serveur.port", 0)));
        configAffichee.put("bmo.poolthreads.taillemaximale", String.valueOf(configurateurServeur.recupererProprieteEntier("bmo.poolthreads.taillemaximale", 0)));
        return configAffichee;
    }

    public ReponseGeneriqueDTO definirConfigurationServeur(String cleConfiguration, String valeurConfiguration) {
        journal.warn("Tentative de définition de la configuration serveur ('{}'='{}') par un administrateur. Non supporté.", cleConfiguration, valeurConfiguration);
        return new ReponseGeneriqueDTO(false, "La modification de la configuration serveur à la volée n'est pas supportée.", "CONFIG_NON_MODIFIABLE");
    }

    public void initialiserUtilisateursParDefautSiNecessaire() {
        journal.info("Vérification et initialisation des utilisateurs par défaut...");
        creerUtilisateurSiNonExistant("admin@bmo.com", "AdminBMO!2025", "Administrateur BMO", RoleUtilisateur.ADMINISTRATEUR, StatutCompteUtilisateur.ACTIF);
        creerUtilisateurSiNonExistant("organisateur@bmo.com", "OrgaBMO!2025", "Organisateur Principal", RoleUtilisateur.ORGANISATEUR, StatutCompteUtilisateur.ACTIF);
        creerUtilisateurSiNonExistant("participant@bmo.com", "PartBMO!2025", "Participant Test", RoleUtilisateur.PARTICIPANT, StatutCompteUtilisateur.ACTIF);
        journal.info("Vérification des utilisateurs par défaut terminée.");
    }

    private void creerUtilisateurSiNonExistant(String identifiant, String motDePasseClair, String nomComplet, RoleUtilisateur role, StatutCompteUtilisateur statut) {
        try {
            if (!this.utilisateurDAO.existeParIdentifiantConnexion(identifiant)) {
                String sel = UtilitaireSecuriteMessagerie.genererSelAleatoireEnBase64();
                String motDePasseHache = UtilitaireSecuriteMessagerie.hacherMotDePasse(motDePasseClair, sel);

                Utilisateur nouvelUtilisateur = new Utilisateur();
                nouvelUtilisateur.setIdentifiant(identifiant);
                nouvelUtilisateur.setMotDePasseHache(motDePasseHache);
                nouvelUtilisateur.setSelMotDePasse(sel);
                nouvelUtilisateur.setNomComplet(nomComplet);
                nouvelUtilisateur.setRole(role);
                nouvelUtilisateur.setStatutCompte(statut);
                nouvelUtilisateur.setDateCreationCompte(LocalDateTime.now());

                utilisateurDAO.creer(nouvelUtilisateur);
                journal.info("Utilisateur par défaut '{}' ({}) créé avec succès.", identifiant, role.name());
            } else {
                journal.debug("L'utilisateur par défaut '{}' existe déjà.", identifiant);
            }
        } catch (Exception e) {
            journal.error("Erreur lors de la tentative de création de l'utilisateur par défaut '{}': {}", identifiant, e.getMessage(), e);
        }
    }
}