package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.serveur.configuration.ConfigurateurServeur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.commun.util.UtilitaireSecuriteMessagerie;
import akandan.bahou.kassy.commun.util.ExceptionCryptage;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

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
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdUtilisateur(entite.getId());
        dto.setIdentifiantConnexion(entite.getIdentifiantConnexion());
        dto.setNomComplet(entite.getNomComplet());
        dto.setRoleUtilisateur(entite.getRoleSysteme());
        dto.setStatutCompte(entite.getStatutCompte());
        if (entite.getDerniereConnexion() != null) {
            dto.setDateDerniereConnexion(entite.getDerniereConnexion().toString());
        }
        // Les champs photo ne sont pas dans DonneesUtilisateurDTO selon la version précédente.
        // Si le DTO est mis à jour pour les inclure, ils pourraient être mappés ici.
        return dto;
    }

    public List<DonneesUtilisateurDTO> listerTousLesUtilisateurs() throws ExceptionPersistance {
        journal.debug("Tentative de listage de tous les utilisateurs par un administrateur.");
        List<Utilisateur> entitesUtilisateur = this.utilisateurDAO.trouverTous();
        List<DonneesUtilisateurDTO> dtosUtilisateur = new ArrayList<>();
        for (Utilisateur entite : entitesUtilisateur) {
            dtosUtilisateur.add(convertirEntiteUtilisateurVersDTO(entite));
        }
        journal.info("Liste de {} utilisateurs récupérée par un administrateur.", dtosUtilisateur.size());
        return dtosUtilisateur;
    }

    public ReponseGeneriqueDTO creerUtilisateurParAdmin(String identifiantConnexion, String motDePasseInitial, String nomComplet, RoleUtilisateur roleSysteme, StatutCompteUtilisateur statutInitial) {
        journal.debug("Tentative de création d'utilisateur par admin : {}", identifiantConnexion);
        try {
            ValidateurEntreeUtilisateur.validerIdentifiantConnexion(identifiantConnexion, "Identifiant de connexion");
            ValidateurEntreeUtilisateur.validerComplexiteMotDePasse(motDePasseInitial, "Mot de passe initial");
            ValidateurEntreeUtilisateur.validerNonNulOuVide(nomComplet, "Nom complet");

            if (this.utilisateurDAO.existeParIdentifiantConnexion(identifiantConnexion)) {
                journal.warn("Échec création utilisateur par admin : Identifiant '{}' déjà existant.", identifiantConnexion);
                return new ReponseGeneriqueDTO(false, "L'identifiant de connexion existe déjà.", "ID_CONN_EXISTANT");
            }

            String sel = UtilitaireSecuriteMessagerie.genererSelAleatoireEnBase64();
            String motDePasseHache = UtilitaireSecuriteMessagerie.hacherMotDePasse(motDePasseInitial, sel);

            Utilisateur nouvelUtilisateur = new Utilisateur();
            nouvelUtilisateur.setIdentifiantConnexion(identifiantConnexion);
            nouvelUtilisateur.setMotDePasseHache(motDePasseHache);
            nouvelUtilisateur.setSelMotDePasse(sel);
            nouvelUtilisateur.setNomComplet(nomComplet);
            nouvelUtilisateur.setRoleSysteme(roleSysteme);
            nouvelUtilisateur.setStatutCompte(statutInitial);
            nouvelUtilisateur.setDateCreationCompte(LocalDateTime.now());

            this.utilisateurDAO.creer(nouvelUtilisateur);
            journal.info("Nouvel utilisateur '{}' créé par un administrateur avec le rôle '{}'.", identifiantConnexion, roleSysteme.name());
            return new ReponseGeneriqueDTO(true, "Utilisateur créé avec succès.");

        } catch (ExceptionValidation | ExceptionCryptage | ExceptionPersistance e) {
            journal.error("Erreur lors de la création de l'utilisateur '{}' par admin : {}", identifiantConnexion, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur lors de la création de l'utilisateur: " + e.getMessage(), "CREATION_UTIL_ERREUR");
        }
    }

    public ReponseGeneriqueDTO modifierUtilisateurParAdmin(int idUtilisateurAModifier, String nouvelIdentifiantConnexion, String nouveauNomComplet, RoleUtilisateur nouveauRoleSysteme, StatutCompteUtilisateur nouveauStatutCompte) {
        journal.debug("Tentative de modification de l'utilisateur ID {} par admin.", idUtilisateurAModifier);
        try {
            Optional<Utilisateur> utilisateurOpt = this.utilisateurDAO.trouverParId(idUtilisateurAModifier);
            if (utilisateurOpt.isEmpty()) {
                journal.warn("Échec modification utilisateur par admin : Utilisateur ID {} non trouvé.", idUtilisateurAModifier);
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }

            Utilisateur utilisateurAModifier = utilisateurOpt.get();

            if (nouvelIdentifiantConnexion != null && !nouvelIdentifiantConnexion.isEmpty()) {
                ValidateurEntreeUtilisateur.validerIdentifiantConnexion(nouvelIdentifiantConnexion, "Nouvel identifiant de connexion");
                if (!utilisateurAModifier.getIdentifiantConnexion().equals(nouvelIdentifiantConnexion) && this.utilisateurDAO.existeParIdentifiantConnexion(nouvelIdentifiantConnexion)) {
                    journal.warn("Échec modification utilisateur par admin : Nouvel identifiant '{}' déjà existant.", nouvelIdentifiantConnexion);
                    return new ReponseGeneriqueDTO(false, "Le nouvel identifiant de connexion est déjà utilisé par un autre compte.", "ID_CONN_EXISTANT");
                }
                utilisateurAModifier.setIdentifiantConnexion(nouvelIdentifiantConnexion);
            }

            if (nouveauNomComplet != null && !nouveauNomComplet.isEmpty()) {
                ValidateurEntreeUtilisateur.validerNonNulOuVide(nouveauNomComplet, "Nouveau nom complet");
                utilisateurAModifier.setNomComplet(nouveauNomComplet);
            }

            if (nouveauRoleSysteme != null) {
                utilisateurAModifier.setRoleSysteme(nouveauRoleSysteme);
            }

            if (nouveauStatutCompte != null) {
                utilisateurAModifier.setStatutCompte(nouveauStatutCompte);
            }

            this.utilisateurDAO.mettreAJour(utilisateurAModifier);
            journal.info("Utilisateur ID {} modifié par un administrateur.", idUtilisateurAModifier);
            return new ReponseGeneriqueDTO(true, "Utilisateur modifié avec succès.");

        } catch (ExceptionValidation | ExceptionPersistance e) {
            journal.error("Erreur lors de la modification de l'utilisateur ID {} par admin : {}", idUtilisateurAModifier, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur lors de la modification de l'utilisateur: " + e.getMessage(), "MODIF_UTIL_ERREUR");
        }
    }

    public ReponseGeneriqueDTO supprimerUtilisateurParAdmin(int idUtilisateurASupprimer) {
        journal.debug("Tentative de suppression de l'utilisateur ID {} par admin.", idUtilisateurASupprimer);
        try {
            if (this.utilisateurDAO.trouverParId(idUtilisateurASupprimer).isEmpty()) {
                journal.warn("Échec suppression utilisateur par admin : Utilisateur ID {} non trouvé.", idUtilisateurASupprimer);
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }

            boolean supprime = this.utilisateurDAO.supprimer(idUtilisateurASupprimer);
            if (supprime) {
                journal.info("Utilisateur ID {} supprimé par un administrateur.", idUtilisateurASupprimer);
                return new ReponseGeneriqueDTO(true, "Utilisateur supprimé avec succès.");
            } else {
                journal.warn("La suppression de l'utilisateur ID {} par admin n'a affecté aucune ligne (déjà supprimé ou problème inconnu).", idUtilisateurASupprimer);
                return new ReponseGeneriqueDTO(false, "La suppression de l'utilisateur a échoué ou l'utilisateur était déjà supprimé.", "SUPPR_UTIL_ECHEC");
            }
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la suppression de l'utilisateur ID {} par admin : {}", idUtilisateurASupprimer, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur de persistance lors de la suppression de l'utilisateur: " + e.getMessage(), "SUPPR_UTIL_ERREUR_PERSIST");
        }
    }

    public Map<String, String> obtenirConfigurationServeur() {
        journal.debug("Tentative d'obtention de la configuration serveur par un administrateur.");
        // Exemple : retourner un sous-ensemble sûr des propriétés.
        // La classe ConfigurateurServeur devrait avoir une méthode pour exposer les propriétés de manière sécurisée.
        // Pour ce prompt, nous supposons que ConfigurateurServeur n'a pas de telle méthode et nous ne pouvons pas le modifier.
        // Donc, cette méthode ne peut pas être implémentée comme décrit sans modifier ConfigurateurServeur.
        // Retournons une map vide ou une indication que la fonctionnalité n'est pas pleinement supportée.
        journal.warn("La fonctionnalité obtenirConfigurationServeur n'est pas pleinement implémentée sans modification de ConfigurateurServeur pour exposer les propriétés.");
        return Map.of("bmo.serveur.port", String.valueOf(configurateurServeur.recupererProprieteEntier("bmo.serveur.port", 0)));
    }

    public ReponseGeneriqueDTO definirConfigurationServeur(String cleConfiguration, String valeurConfiguration) {
        journal.warn("Tentative de définition de la configuration serveur ('{}'='{}') par un administrateur. Cette fonctionnalité n'est pas supportée pour modification à la volée.", cleConfiguration, valeurConfiguration);
        // La modification de configuration à la volée est complexe et potentiellement dangereuse.
        // ConfigurateurServeur charge les propriétés au démarrage et n'est pas conçu pour les modifier à la volée.
        // Cette méthode retournerait typiquement une erreur ou indiquerait que l'opération n'est pas supportée.
        return new ReponseGeneriqueDTO(false, "La modification de la configuration serveur à la volée n'est pas supportée.", "CONFIG_NON_MODIFIABLE");
    }
}