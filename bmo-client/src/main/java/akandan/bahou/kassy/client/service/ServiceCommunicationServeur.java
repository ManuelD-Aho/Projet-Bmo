package akandan.bahou.kassy.client.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.protocole.TypeRequeteClient;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;


public class ServiceCommunicationServeur {

    private Socket socketClient;
    private PrintWriter fluxSortie;
    private BufferedReader fluxEntree;
    private ExecutorService serviceEcouteServeur;
    private volatile boolean doitEcouter = false;
    private final ServiceSessionUtilisateur serviceSessionUtilisateur;
    private final ResourceBundle paquetRessourcesI18n;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ServiceCommunicationServeur.class);

    private final StringProperty dernierMessageErreurAuth = new SimpleStringProperty();
    private final ObjectProperty<MessageChatDTO> dernierMessageChatRecu = new SimpleObjectProperty<>();
    private final ListProperty<MessageChatDTO> historiqueMessagesReunion = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<DetailsReunionDTO> detailsReunionActuelle = new SimpleObjectProperty<>();
    private final ListProperty<DetailsReunionDTO> listeToutesReunions = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<DetailsReunionDTO> listeMesReunions = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<DonneesUtilisateurDTO> profilUtilisateurActuel = new SimpleObjectProperty<>();
    private final ListProperty<DonneesUtilisateurDTO> listeGlobaleUtilisateurs = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty etatConnexionServeur = new SimpleBooleanProperty(false);
    private final ObjectProperty<DonneesUtilisateurDTO> notificationUtilisateurRejoint = new SimpleObjectProperty<>();
    private final ObjectProperty<DonneesUtilisateurDTO> notificationUtilisateurQuitte = new SimpleObjectProperty<>();
    private final IntegerProperty notificationReunionClotureeId = new SimpleIntegerProperty(0);
    private final ObjectProperty<DonneesUtilisateurDTO> utilisateurMisAJourParAdmin = new SimpleObjectProperty<>();


    public ServiceCommunicationServeur(ServiceSessionUtilisateur serviceSession, ResourceBundle paquetRessources) {
        this.serviceSessionUtilisateur = serviceSession;
        this.paquetRessourcesI18n = paquetRessources;
        initialiserServiceEcoute();
    }

    private void initialiserServiceEcoute() {
        if (this.serviceEcouteServeur != null && !this.serviceEcouteServeur.isShutdown()) {
            this.serviceEcouteServeur.shutdownNow();
            try {
                if (!this.serviceEcouteServeur.awaitTermination(1, TimeUnit.SECONDS)) {
                    journal.warn("Le service d'écoute n'a pas pu être arrêté proprement lors de la réinitialisation.");
                }
            } catch (InterruptedException e) {
                journal.warn("Interruption lors de l'attente de l'arrêt du service d'écoute.");
                Thread.currentThread().interrupt();
            }
        }
        this.serviceEcouteServeur = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("ThreadEcouteServeurBMO");
            t.setDaemon(true);
            return t;
        });
    }

    public boolean connecterAuServeur(String adresseServeur, int portServeur) {
        if (etatConnexionServeur.get()) {
            journal.info("Déjà connecté au serveur.");
            return true;
        }
        try {
            this.socketClient = new Socket(adresseServeur, portServeur);
            this.fluxSortie = new PrintWriter(this.socketClient.getOutputStream(), true, StandardCharsets.UTF_8);
            this.fluxEntree = new BufferedReader(new InputStreamReader(this.socketClient.getInputStream(), StandardCharsets.UTF_8));
            Platform.runLater(() -> etatConnexionServeur.set(true));
            lancerEcouteServeur();
            journal.info("Connecté au serveur BMO sur {}:{}", adresseServeur, portServeur);
            return true;
        } catch (IOException e) {
            journal.error("Impossible de se connecter au serveur BMO {}:{}. Raison : {}", adresseServeur, portServeur, e.getMessage());
            Platform.runLater(() -> etatConnexionServeur.set(false));
            return false;
        }
    }

    public void deconnecterDuServeur() {
        this.doitEcouter = false; // Important pour arrêter la boucle du thread d'écoute
        Platform.runLater(() -> etatConnexionServeur.set(false));

        if (serviceEcouteServeur != null && !serviceEcouteServeur.isShutdown()) {
            serviceEcouteServeur.shutdown();
            try {
                if (!serviceEcouteServeur.awaitTermination(2, TimeUnit.SECONDS)) {
                    serviceEcouteServeur.shutdownNow();
                }
            } catch (InterruptedException e) {
                serviceEcouteServeur.shutdownNow();
                Thread.currentThread().interrupt();
                journal.warn("Thread d'écoute interrompu pendant l'attente de terminaison.");
            }
        }
        // Il est important de réinitialiser le service pour les futures connexions
        initialiserServiceEcoute();


        try {
            if (fluxSortie != null) {
                fluxSortie.close();
            }
            if (fluxEntree != null) {
                fluxEntree.close();
            }
            if (socketClient != null && !socketClient.isClosed()) {
                socketClient.close();
            }
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture des flux ou du socket : {}", e.getMessage());
        } finally {
            fluxSortie = null;
            fluxEntree = null;
            socketClient = null;
            if (serviceSessionUtilisateur != null) { // Vérifier la nullité avant d'appeler
                serviceSessionUtilisateur.viderSession();
            }
            journal.info("Déconnecté du serveur BMO.");
        }
    }

    public boolean estActuellementConnecte() {
        return etatConnexionServeur.get();
    }

    private void envoyerMessageAuServeur(String message) {
        if (estActuellementConnecte() && fluxSortie != null) {
            fluxSortie.println(message);
            journal.debug("Envoyé au serveur : {}", message);
        } else {
            journal.error("Impossible d'envoyer le message, non connecté ou flux de sortie nul.");
            Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.send.message.title"),
                    paquetRessourcesI18n.getString("error.send.message.notconnected.content")
            ));
            // Envisager une déconnexion plus propre ici si l'état est incohérent
            if(estActuellementConnecte()){ // Si on pense être connecté mais que fluxSortie est nul
                deconnecterDuServeur();
            }
        }
    }

    private void envoyerRequeteAvecParametres(TypeRequeteClient typeRequete, String... parametres) {
        StringBuilder sb = new StringBuilder(typeRequete.name());
        for (String param : parametres) {
            sb.append(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE).append(param == null ? "" : param);
        }
        envoyerMessageAuServeur(sb.toString());
    }

    private void envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient typeRequete, Object dto) {
        if (!estActuellementConnecte()) {
            journal.warn("Tentative d'envoi de requête {} alors que non connecté.", typeRequete.name());
            return;
        }
        try {
            String jsonPayload = "";
            if (dto instanceof DetailsReunionDTO) {
                jsonPayload = ((DetailsReunionDTO) dto).toJsonString();
            } else if (dto instanceof DonneesUtilisateurDTO) {
                jsonPayload = ((DonneesUtilisateurDTO) dto).toJsonString();
            } else if (dto instanceof MessageChatDTO) {
                jsonPayload = ((MessageChatDTO) dto).toJsonString();
            } else if (dto instanceof JSONObject) {
                jsonPayload = ((JSONObject) dto).toString();
            } else if (dto != null) {
                // Fallback ou erreur si le type de DTO n'est pas géré pour toJsonString()
                journal.error("Type de DTO non supporté pour la sérialisation JSON directe: {}", dto.getClass().getName());
                return; // Ou lancer une exception
            }

            String requete = typeRequete.name() + (jsonPayload.isEmpty() ? "" : ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + jsonPayload);
            envoyerMessageAuServeur(requete);
        } catch (JSONException e) {
            journal.error("Erreur de conversion DTO en JSON pour la requête {} : {}", typeRequete.name(), e.getMessage());
        }
    }


    private void lancerEcouteServeur() {
        if (serviceEcouteServeur.isShutdown()) {
            initialiserServiceEcoute();
        }
        doitEcouter = true;
        serviceEcouteServeur.submit(() -> {
            try {
                String ligneDuServeur;
                while (doitEcouter && fluxEntree != null && (ligneDuServeur = fluxEntree.readLine()) != null) {
                    journal.debug("Reçu du serveur : {}", ligneDuServeur);
                    traiterReponseServeur(ligneDuServeur);
                }
            } catch (SocketException e) {
                if (doitEcouter) { // Si l'exception n'est pas due à une fermeture volontaire
                    journal.error("SocketException lors de la lecture du serveur (déconnexion probable) : {}. Tentative de déconnexion propre.", e.getMessage());
                    deconnecterDuServeur();
                    Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                            paquetRessourcesI18n.getString("error.connection.lost.title"),
                            paquetRessourcesI18n.getString("error.connection.lost.content") + "\n" + e.getMessage()
                    ));
                }
            } catch (IOException e) {
                if (doitEcouter) {
                    journal.error("IOException lors de la lecture du serveur : {}. Tentative de déconnexion propre.", e.getMessage());
                    deconnecterDuServeur();
                }
            } finally {
                if (doitEcouter) { // Si la boucle s'est terminée mais qu'on est tjrs "connecté" logiquement
                    journal.info("Thread d'écoute serveur terminé de manière inattendue. Déconnexion.");
                    deconnecterDuServeur();
                } else {
                    journal.info("Thread d'écoute serveur arrêté normalement.");
                }
            }
        });
    }

    private void traiterReponseServeur(String reponseBrute) {
        if (reponseBrute == null || reponseBrute.trim().isEmpty()) {
            journal.warn("Réponse vide reçue du serveur.");
            return;
        }
        String[] tokens = reponseBrute.split(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE, 2); // Limiter à 2 pour avoir la commande et le reste (potentiellement JSON)
        if (tokens.length == 0) {
            journal.warn("Réponse malformée reçue du serveur (pas de tokens).");
            return;
        }

        TypeReponseServeur typeReponse;
        try {
            typeReponse = TypeReponseServeur.valueOf(tokens[0]);
        } catch (IllegalArgumentException e) {
            journal.error("Type de réponse serveur inconnu : {}", tokens[0]);
            return;
        }

        String donneesJsonString = tokens.length > 1 ? tokens[1] : null;

        Platform.runLater(() -> {
            try {
                switch (typeReponse) {
                    case AUTH_OK:
                        if (donneesJsonString != null) {
                            serviceSessionUtilisateur.definirSession(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                            dernierMessageErreurAuth.set("");
                        } else {
                            journal.error("Données manquantes pour AUTH_OK");
                            dernierMessageErreurAuth.set(paquetRessourcesI18n.getString("error.auth.generic"));
                        }
                        break;
                    case AUTH_ECHEC:
                        dernierMessageErreurAuth.set(donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("error.auth.generic"));
                        serviceSessionUtilisateur.viderSession();
                        break;
                    case INSCRIPTION_OK:
                        // Afficher un message de succès, peut-être naviguer vers connexion
                        AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("signup.success.title"), paquetRessourcesI18n.getString("signup.success.content"));
                        break;
                    case INSCRIPTION_ECHEC:
                        dernierMessageErreurAuth.set(donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("error.signup.generic"));
                        break;
                    case LISTE_REUNIONS:
                        if (donneesJsonString != null) {
                            JSONArray reunionsArr = new JSONArray(donneesJsonString);
                            List<DetailsReunionDTO> listeR = new ArrayList<>();
                            for (int i = 0; i < reunionsArr.length(); i++) {
                                listeR.add(DetailsReunionDTO.fromJson(reunionsArr.getJSONObject(i).toString()));
                            }
                            listeToutesReunions.setAll(listeR);
                        }
                        break;
                    case MES_REUNIONS:
                        if (donneesJsonString != null) {
                            JSONArray mesReunionsArr = new JSONArray(donneesJsonString);
                            List<DetailsReunionDTO> listeMR = new ArrayList<>();
                            for (int i = 0; i < mesReunionsArr.length(); i++) {
                                listeMR.add(DetailsReunionDTO.fromJson(mesReunionsArr.getJSONObject(i).toString()));
                            }
                            listeMesReunions.setAll(listeMR);
                        }
                        break;
                    case DETAILS_REUNION:
                    case REUNION_CREEE: // Serveur renvoie les détails de la réunion créée/modifiée
                    case REUNION_MODIFIEE:
                    case REUNION_REJOINTE: // Serveur renvoie les détails de la réunion rejointe
                        if (donneesJsonString != null) {
                            detailsReunionActuelle.set(DetailsReunionDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case NOUVEAU_MESSAGE_CHAT:
                        if (donneesJsonString != null) {
                            dernierMessageChatRecu.set(MessageChatDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case HISTORIQUE_MESSAGES:
                        if (donneesJsonString != null) {
                            JSONArray messagesArr = new JSONArray(donneesJsonString);
                            List<MessageChatDTO> hist = new ArrayList<>();
                            for (int i = 0; i < messagesArr.length(); i++) {
                                hist.add(MessageChatDTO.fromJson(messagesArr.getJSONObject(i).toString()));
                            }
                            historiqueMessagesReunion.setAll(hist);
                        }
                        break;
                    case MON_PROFIL:
                        if (donneesJsonString != null) {
                            profilUtilisateurActuel.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case LISTE_UTILISATEURS_ADMIN: // Pour l'admin
                        if (donneesJsonString != null) {
                            JSONArray utilisateursArr = new JSONArray(donneesJsonString);
                            List<DonneesUtilisateurDTO> listeU = new ArrayList<>();
                            for (int i = 0; i < utilisateursArr.length(); i++) {
                                listeU.add(DonneesUtilisateurDTO.fromJson(utilisateursArr.getJSONObject(i).toString()));
                            }
                            listeGlobaleUtilisateurs.setAll(listeU);
                        }
                        break;
                    case UTILISATEUR_MODIFIE_ADMIN: // Confirmation de modif par admin
                        if (donneesJsonString != null) {
                            utilisateurMisAJourParAdmin.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                            AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("admin.user.update.success.title"), paquetRessourcesI18n.getString("admin.user.update.success.content"));
                        }
                        break;
                    case UTILISATEUR_REJOINT_REUNION:
                        if (donneesJsonString != null) {
                            notificationUtilisateurRejoint.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case UTILISATEUR_QUITTE_REUNION:
                        if (donneesJsonString != null) {
                            notificationUtilisateurQuitte.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case NOTIFICATION_REUNION_CLOTUREE:
                        if (donneesJsonString != null) {
                            try {
                                notificationReunionClotureeId.set(Integer.parseInt(donneesJsonString));
                            } catch (NumberFormatException nfe) {
                                journal.error("ID de réunion pour clôture invalide: {}", donneesJsonString);
                            }
                        }
                        break;
                    case ERREUR:
                        String messageErreur = paquetRessourcesI18n.getString("error.server.generic");
                        String codeErreur = "";
                        if (donneesJsonString != null) {
                            try {
                                JSONObject errJson = new JSONObject(donneesJsonString);
                                messageErreur = errJson.optString("message", messageErreur);
                                codeErreur = errJson.optString("codeErreur", ""); // Adapter la clé si besoin
                            } catch (JSONException e) { // Si ce n'est pas du JSON, on prend la chaîne brute
                                messageErreur = donneesJsonString;
                            }
                        }
                        AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.server.title") + (codeErreur.isEmpty() ? "" : " ("+codeErreur+")"), messageErreur);
                        break;
                    default:
                        journal.warn("Traitement non implémenté pour le type de réponse serveur : {}", typeReponse);
                }
            } catch (JSONException e) {
                journal.error("Erreur de parsing JSON pour la réponse {}: {}", typeReponse, e.getMessage(), e);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.json.parsing.title"), paquetRessourcesI18n.getString("error.json.parsing.content") + "\n" + reponseBrute);
            } catch (Exception e) {
                journal.error("Erreur inattendue lors du traitement de la réponse {} : {}", typeReponse, e.getMessage(), e);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.response.processing.title"), paquetRessourcesI18n.getString("error.response.processing.content") + "\n" + e.getMessage());
            }
        });
    }

    // Méthodes d'envoi de requêtes
    public void envoyerRequeteConnexion(String identifiant, String motDePasse) {
        JSONObject json = new JSONObject();
        try {
            json.put("identifiant", identifiant); // Doit correspondre à ce que le serveur attend
            json.put("motDePasse", motDePasse);
            envoyerRequeteAvecParametres(TypeRequeteClient.CONNEXION, json.toString());
        } catch (JSONException e) {
            journal.error("Erreur création JSON pour connexion: {}", e.getMessage());
        }
    }

    public void envoyerRequeteInscription(String identifiant, String motDePasse, String nomComplet) {
        DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
        dto.setIdentifiant(identifiant);
        // Le mot de passe ne devrait pas être dans le DTO pour les autres usages.
        // Pour l'inscription, on peut construire un JSON spécifique.
        JSONObject json = new JSONObject();
        try {
            json.put("identifiant", identifiant);
            json.put("motDePasse", motDePasse); // Le serveur doit s'attendre à ce champ
            json.put("nomComplet", nomComplet);
            envoyerRequeteAvecParametres(TypeRequeteClient.INSCRIPTION, json.toString());
        } catch (JSONException e) {
            journal.error("Erreur création JSON pour inscription: {}", e.getMessage());
        }
    }

    public void envoyerRequeteDeconnexion() {
        envoyerRequeteAvecParametres(TypeRequeteClient.DECONNEXION);
        // La déconnexion locale (vider session, etc.) est gérée par deconnecterDuServeur()
        // qui peut être appelée après confirmation du serveur ou par l'UI directement.
    }

    public void envoyerRequeteCreationReunion(DetailsReunionDTO details) {
        envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient.NOUVELLE_REUNION, details);
    }

    public void envoyerRequeteModificationReunion(DetailsReunionDTO details) {
        envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient.MODIFIER_REUNION, details);
    }

    public void envoyerRequeteObtenirToutesLesReunions() {
        envoyerRequeteAvecParametres(TypeRequeteClient.OBTENIR_REUNIONS);
    }

    public void envoyerRequeteObtenirMesReunions() {
        envoyerRequeteAvecParametres(TypeRequeteClient.OBTENIR_MES_REUNIONS);
    }

    public void envoyerRequeteObtenirDetailsReunion(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.OBTENIR_DETAILS_REUNION, String.valueOf(idReunion));
    }

    public void envoyerRequeteRejoindreReunion(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.REJOINDRE_REUNION, String.valueOf(idReunion));
    }

    public void envoyerRequeteQuitterReunion(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.QUITTER_REUNION, String.valueOf(idReunion));
    }

    public void envoyerMessageChat(MessageChatDTO message) { // Renommé pour correspondre à la méthode du contrôleur
        envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient.MESSAGE_CHAT, message);
    }

    public void envoyerRequeteOuvrirReunion(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.OUVRIR_REUNION, String.valueOf(idReunion));
    }

    public void envoyerRequeteCloturerReunion(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.CLOTURER_REUNION, String.valueOf(idReunion));
    }

    public void envoyerRequeteObtenirHistoriqueMessages(long idReunion) {
        envoyerRequeteAvecParametres(TypeRequeteClient.OBTENIR_HISTORIQUE_MESSAGES, String.valueOf(idReunion));
    }

    public void envoyerRequeteObtenirMonProfil() {
        envoyerRequeteAvecParametres(TypeRequeteClient.OBTENIR_MON_PROFIL);
    }

    public void envoyerRequeteModifierProfil(DonneesUtilisateurDTO donneesUtilisateur) {
        envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient.MODIFIER_PROFIL, donneesUtilisateur);
    }

    public void envoyerRequeteChangerMotDePasse(String ancienMotDePasse, String nouveauMotDePasse) {
        JSONObject json = new JSONObject();
        try {
            json.put("ancienMotDePasse", ancienMotDePasse);
            json.put("nouveauMotDePasse", nouveauMotDePasse);
            envoyerRequeteAvecParametres(TypeRequeteClient.CHANGER_MOT_DE_PASSE, json.toString());
        } catch (JSONException e) {
            journal.error("Erreur création JSON pour changer MDP: {}", e.getMessage());
        }
    }

    public void envoyerRequeteAdminObtenirUtilisateurs(String filtreNomOuIdentifiant) {
        envoyerRequeteAvecParametres(TypeRequeteClient.ADMIN_LISTER_UTILISATEURS, filtreNomOuIdentifiant);
    }

    public void envoyerRequeteAdminModifierUtilisateur(DonneesUtilisateurDTO dto) {
        envoyerRequeteAvecDonneesJsonViaDTO(TypeRequeteClient.ADMIN_MODIFIER_UTILISATEUR, dto);
    }

    // Getters pour les Propriétés Observables
    public StringProperty dernierMessageErreurAuthProperty() { return dernierMessageErreurAuth; }
    public ObjectProperty<MessageChatDTO> dernierMessageChatRecuProperty() { return dernierMessageChatRecu; }
    public ListProperty<MessageChatDTO> historiqueMessagesReunionProperty() { return historiqueMessagesReunion; }
    public ObjectProperty<DetailsReunionDTO> detailsReunionActuelleProperty() { return detailsReunionActuelle; }
    public ListProperty<DetailsReunionDTO> listeToutesReunionsProperty() { return listeToutesReunions; }
    public ListProperty<DetailsReunionDTO> listeMesReunionsProperty() { return listeMesReunions; }
    public ObjectProperty<DonneesUtilisateurDTO> profilUtilisateurActuelProperty() { return profilUtilisateurActuel; }
    public ListProperty<DonneesUtilisateurDTO> listeGlobaleUtilisateursProperty() { return listeGlobaleUtilisateurs; }
    public BooleanProperty etatConnexionServeurProperty() { return etatConnexionServeur; }
    public ObjectProperty<DonneesUtilisateurDTO> notificationUtilisateurRejointProperty() { return notificationUtilisateurRejoint; }
    public ObjectProperty<DonneesUtilisateurDTO> notificationUtilisateurQuitteProperty() { return notificationUtilisateurQuitte; }
    public IntegerProperty notificationReunionClotureeIdProperty() { return notificationReunionClotureeId; }
    public ObjectProperty<DonneesUtilisateurDTO> utilisateurMisAJourParAdminProperty() { return utilisateurMisAJourParAdmin;}

}