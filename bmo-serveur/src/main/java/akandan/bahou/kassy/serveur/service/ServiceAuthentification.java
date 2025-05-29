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
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceAuthentification {

    private final InterfaceUtilisateurDAO utilisateurDAO;
    private static final Logger journal = LoggerFactory.getLogger(ServiceAuthentification.class);

    public ServiceAuthentification(InterfaceUtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
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

    public Optional<DonneesUtilisateurDTO> authentifierUtilisateur(String identifiant, String motDePasseEnClair) {
        journal.debug("Tentative d'authentification pour l'identifiant : {}", identifiant);
        try {
            Optional<Utilisateur> utilisateurOpt = this.utilisateurDAO.trouverParIdentifiantConnexion(identifiant);

            if (utilisateurOpt.isEmpty()) {
                journal.warn("Échec de l'authentification : Identifiant de connexion '{}' non trouvé.", identifiant);
                return Optional.empty();
            }

            Utilisateur utilisateurTrouve = utilisateurOpt.get();

            if (utilisateurTrouve.getStatutCompte() != StatutCompteUtilisateur.ACTIF) {
                journal.warn("Échec de l'authentification pour '{}' : Compte non actif (Statut: {}).", identifiant, utilisateurTrouve.getStatutCompte());
                return Optional.empty();
            }

            boolean motDePasseValide = UtilitaireSecuriteMessagerie.verifierMotDePasse(
                    motDePasseEnClair,
                    utilisateurTrouve.getMotDePasseHache(),
                    utilisateurTrouve.getSelMotDePasse()
            );

            if (motDePasseValide) {
                utilisateurTrouve.setDerniereConnexion(LocalDateTime.now());
                this.utilisateurDAO.mettreAJour(utilisateurTrouve);
                journal.info("Authentification réussie pour l'utilisateur : {}", identifiant);
                return Optional.of(convertirEntiteUtilisateurVersDTO(utilisateurTrouve));
            } else {
                journal.warn("Échec de l'authentification pour '{}' : Mot de passe incorrect.", identifiant);
                return Optional.empty();
            }

        } catch (ExceptionPersistance | ExceptionCryptage e) {
            journal.error("Erreur lors du processus d'authentification pour '{}': {}", identifiant, e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de l'authentification pour '{}': {}", identifiant, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public ReponseGeneriqueDTO inscrireNouvelUtilisateur(String identifiant, String motDePasseEnClair, String nomComplet) {
        journal.debug("Tentative d'inscription pour le nouvel identifiant : {}", identifiant);
        try {
            ValidateurEntreeUtilisateur.validerIdentifiantConnexion(identifiant, "Identifiant de connexion");
            ValidateurEntreeUtilisateur.validerComplexiteMotDePasse(motDePasseEnClair, "Mot de passe");
            ValidateurEntreeUtilisateur.validerNonNulOuVide(nomComplet, "Nom complet");

            if (this.utilisateurDAO.existeParIdentifiantConnexion(identifiant)) {
                journal.warn("Échec de l'inscription : L'identifiant de connexion '{}' existe déjà.", identifiant);
                return new ReponseGeneriqueDTO(false, "Cet identifiant de connexion est déjà utilisé.", "ID_CONN_EXISTANT");
            }

            String sel = UtilitaireSecuriteMessagerie.genererSelAleatoireEnBase64();
            String motDePasseHache = UtilitaireSecuriteMessagerie.hacherMotDePasse(motDePasseEnClair, sel);

            Utilisateur nouvelUtilisateur = new Utilisateur();
            nouvelUtilisateur.setIdentifiant(identifiant);
            nouvelUtilisateur.setMotDePasseHache(motDePasseHache);
            nouvelUtilisateur.setSelMotDePasse(sel);
            nouvelUtilisateur.setNomComplet(nomComplet);
            nouvelUtilisateur.setRole(RoleUtilisateur.PARTICIPANT);
            nouvelUtilisateur.setStatutCompte(StatutCompteUtilisateur.ACTIF);
            nouvelUtilisateur.setDateCreationCompte(LocalDateTime.now());
            nouvelUtilisateur.setDerniereConnexion(null);

            this.utilisateurDAO.creer(nouvelUtilisateur);
            journal.info("Inscription réussie pour le nouvel utilisateur : {}", identifiant);
            return new ReponseGeneriqueDTO(true, "Inscription réussie. Vous pouvez maintenant vous connecter.");

        } catch (ExceptionValidation e) {
            journal.warn("Échec de l'inscription pour '{}' dû à une validation : {}", identifiant, e.getMessage());
            return new ReponseGeneriqueDTO(false, e.getMessage(), "VALIDATION_ECHOUEE");
        } catch (ExceptionCryptage | ExceptionPersistance e) {
            journal.error("Erreur technique lors de l'inscription pour '{}': {}", identifiant, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Une erreur technique est survenue lors de l'inscription.", "INSCRIPTION_ERREUR_TECH");
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de l'inscription pour '{}': {}", identifiant, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Une erreur inattendue est survenue.", "ERREUR_INATTENDUE_INSCRIPTION");
        }
    }
}