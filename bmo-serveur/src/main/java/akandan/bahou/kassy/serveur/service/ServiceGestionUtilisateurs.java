package akandan.bahou.kassy.serveur.service;

import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.modele.Utilisateur;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.ReponseGeneriqueDTO;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import akandan.bahou.kassy.commun.util.UtilitaireSecuriteMessagerie;
import akandan.bahou.kassy.commun.util.ExceptionCryptage;

import java.util.Optional;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceGestionUtilisateurs {

    private final InterfaceUtilisateurDAO utilisateurDAO;
    private static final Logger journal = LoggerFactory.getLogger(ServiceGestionUtilisateurs.class);

    public ServiceGestionUtilisateurs(InterfaceUtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
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
            dto.setDateDerniereConnexion(entite.getDerniereConnexion().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return dto;
    }

    public Optional<DonneesUtilisateurDTO> obtenirDonneesUtilisateur(int idUtilisateur) {
        journal.debug("Tentative d'obtention des données pour l'utilisateur ID {}.", idUtilisateur);
        try {
            Optional<Utilisateur> entiteOpt = this.utilisateurDAO.trouverParId(idUtilisateur);
            return entiteOpt.map(this::convertirEntiteUtilisateurVersDTO);
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la récupération de l'utilisateur ID {}: {}", idUtilisateur, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public ReponseGeneriqueDTO mettreAJourProfilUtilisateur(int idUtilisateurDemandeur, String nouveauNomComplet, String nouvelIdentifiantConnexion) {
        journal.debug("Tentative de mise à jour du profil pour l'utilisateur ID {}.", idUtilisateurDemandeur);
        try {
            Optional<Utilisateur> utilisateurOpt = this.utilisateurDAO.trouverParId(idUtilisateurDemandeur);
            if (utilisateurOpt.isEmpty()) {
                journal.warn("Échec de mise à jour du profil : Utilisateur ID {} non trouvé.", idUtilisateurDemandeur);
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }
            Utilisateur utilisateur = utilisateurOpt.get();

            if (nouveauNomComplet != null && !nouveauNomComplet.trim().isEmpty()) {
                ValidateurEntreeUtilisateur.validerNonNulOuVide(nouveauNomComplet, "Nouveau nom complet");
                utilisateur.setNomComplet(nouveauNomComplet.trim());
            }

            if (nouvelIdentifiantConnexion != null && !nouvelIdentifiantConnexion.trim().isEmpty()) {
                ValidateurEntreeUtilisateur.validerIdentifiantConnexion(nouvelIdentifiantConnexion, "Nouvel identifiant de connexion");
                if (!utilisateur.getIdentifiantConnexion().equals(nouvelIdentifiantConnexion.trim())) {
                    if (this.utilisateurDAO.existeParIdentifiantConnexion(nouvelIdentifiantConnexion.trim())) {
                        journal.warn("Échec de mise à jour du profil pour ID {} : Nouvel identifiant '{}' déjà utilisé.", idUtilisateurDemandeur, nouvelIdentifiantConnexion.trim());
                        return new ReponseGeneriqueDTO(false, "Ce nouvel identifiant de connexion est déjà utilisé.", "ID_CONN_EXISTANT");
                    }
                    utilisateur.setIdentifiantConnexion(nouvelIdentifiantConnexion.trim());
                }
            }

            this.utilisateurDAO.mettreAJour(utilisateur);
            journal.info("Profil de l'utilisateur ID {} mis à jour avec succès.", idUtilisateurDemandeur);
            return new ReponseGeneriqueDTO(true, "Profil mis à jour avec succès.");

        } catch (ExceptionValidation e) {
            journal.warn("Échec de mise à jour du profil pour ID {} dû à une validation : {}", idUtilisateurDemandeur, e.getMessage());
            return new ReponseGeneriqueDTO(false, e.getMessage(), "VALIDATION_ECHOUEE");
        } catch (ExceptionPersistance e) {
            journal.error("Erreur de persistance lors de la mise à jour du profil pour ID {} : {}", idUtilisateurDemandeur, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur serveur lors de la mise à jour du profil.", "MAJ_PROFIL_ERREUR_PERSIST");
        }
    }

    public ReponseGeneriqueDTO changerMotDePasseUtilisateur(int idUtilisateurDemandeur, String motDePasseActuel, String nouveauMotDePasse) {
        journal.debug("Tentative de changement de mot de passe pour l'utilisateur ID {}.", idUtilisateurDemandeur);
        try {
            Optional<Utilisateur> utilisateurOpt = this.utilisateurDAO.trouverParId(idUtilisateurDemandeur);
            if (utilisateurOpt.isEmpty()) {
                journal.warn("Échec changement de mot de passe : Utilisateur ID {} non trouvé.", idUtilisateurDemandeur);
                return new ReponseGeneriqueDTO(false, "Utilisateur non trouvé.", "UTIL_NON_TROUVE");
            }
            Utilisateur utilisateur = utilisateurOpt.get();

            if (!UtilitaireSecuriteMessagerie.verifierMotDePasse(motDePasseActuel, utilisateur.getMotDePasseHache(), utilisateur.getSelMotDePasse())) {
                journal.warn("Échec changement de mot de passe pour ID {} : Mot de passe actuel incorrect.", idUtilisateurDemandeur);
                return new ReponseGeneriqueDTO(false, "Le mot de passe actuel est incorrect.", "MDP_ACTUEL_INCORRECT");
            }

            ValidateurEntreeUtilisateur.validerComplexiteMotDePasse(nouveauMotDePasse, "Nouveau mot de passe");

            String nouveauSel = UtilitaireSecuriteMessagerie.genererSelAleatoireEnBase64();
            String nouveauMotDePasseHache = UtilitaireSecuriteMessagerie.hacherMotDePasse(nouveauMotDePasse, nouveauSel);

            utilisateur.setMotDePasseHache(nouveauMotDePasseHache);
            utilisateur.setSelMotDePasse(nouveauSel);

            this.utilisateurDAO.mettreAJour(utilisateur);
            journal.info("Mot de passe de l'utilisateur ID {} changé avec succès.", idUtilisateurDemandeur);
            return new ReponseGeneriqueDTO(true, "Mot de passe changé avec succès.");

        } catch (ExceptionValidation e) {
            journal.warn("Échec changement de mot de passe pour ID {} dû à une validation : {}", idUtilisateurDemandeur, e.getMessage());
            return new ReponseGeneriqueDTO(false, e.getMessage(), "VALIDATION_MDP_ECHOUEE");
        } catch (ExceptionCryptage | ExceptionPersistance e) {
            journal.error("Erreur technique lors du changement de mot de passe pour ID {} : {}", idUtilisateurDemandeur, e.getMessage(), e);
            return new ReponseGeneriqueDTO(false, "Erreur technique lors du changement de mot de passe.", "CHANGER_MDP_ERREUR_TECH");
        }
    }
}