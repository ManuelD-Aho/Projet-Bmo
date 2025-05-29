package akandan.bahou.kassy.client.service;

import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.protocole.TypeRequeteClient;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
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
    private final ObjectProperty<DetailsReunionDTO> notificationReunionMiseAJour = new SimpleObjectProperty<>();
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
        this.doitEcouter = false;
        Platform.runLater(() -> etatConnexionServeur.set(false));

        if (serviceEcouteServeur != null && !serviceEcouteServeur.isShutdown()) {
            serviceEcouteServeur.shutdown();
            try {
                if (!serviceEcouteServeur.awaitTermination(1, TimeUnit.SECONDS)) {
                    serviceEcouteServeur.shutdownNow(); // Tente d'arrêter plus agressivement
                }
            } catch (InterruptedException e) {
                serviceEcouteServeur.shutdownNow();
                Thread.currentThread().interrupt();
                journal.warn("Thread d'écoute interrompu pendant l'attente de terminaison lors de la déconnexion.");
            }
        }
        initialiserServiceEcoute(); // Prépare pour une reconnexion future

        try {
            if (fluxSortie != null) fluxSortie.close();
        } catch (Exception e) { journal.warn("Erreur fermeture flux sortie: {}", e.getMessage());}
        try {
            if (fluxEntree != null) fluxEntree.close();
        } catch (IOException e) { journal.warn("Erreur fermeture flux entrée: {}", e.getMessage());}
        try {
            if (socketClient != null && !socketClient.isClosed()) socketClient.close();
        } catch (IOException e) { journal.warn("Erreur fermeture socket: {}", e.getMessage());}

        fluxSortie = null;
        fluxEntree = null;
        socketClient = null;
        if (serviceSessionUtilisateur != null) {
            serviceSessionUtilisateur.viderSession();
        }
        journal.info("Déconnecté du serveur BMO.");
    }

    public boolean estActuellementConnecte() {
        return etatConnexionServeur.get();
    }

    private void envoyerMessageAuServeur(String message) {
        if (estActuellementConnecte() && fluxSortie != null) {
            fluxSortie.println(message);
            journal.debug("Envoyé au serveur : {}", message);
        } else {
            journal.error("Impossible d'envoyer le message, non connecté ou flux de sortie nul. Message: {}", message);
            Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                    paquetRessourcesI18n.getString("error.send.message.title"),
                    paquetRessourcesI18n.getString("error.send.message.notconnected.content")
            ));
            if (estActuellementConnecte()) { // Si l'état est incohérent
                deconnecterDuServeur();
            }
        }
    }

    private void envoyerRequeteAvecPayloadJson(TypeRequeteClient typeRequete, String payloadJson) {
        String requete = typeRequete.name() + (payloadJson == null || payloadJson.isEmpty() ? "" : ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + payloadJson);
        envoyerMessageAuServeur(requete);
    }

    private void envoyerRequeteSimple(TypeRequeteClient typeRequete) {
        envoyerRequeteAvecPayloadJson(typeRequete, null);
    }


    private void lancerEcouteServeur() {
        if (serviceEcouteServeur.isShutdown() || serviceEcouteServeur.isTerminated()) {
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
                if (doitEcouter) {
                    journal.error("SocketException (serveur potentiellement arrêté ou connexion perdue): {}. Déconnexion.", e.getMessage());
                    deconnecterDuServeur(); // Assure une déconnexion propre côté client
                    Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                            paquetRessourcesI18n.getString("error.connection.lost.title"),
                            paquetRessourcesI18n.getString("error.connection.lost.content.socket")
                    ));
                } else {
                    journal.info("SocketException reçue pendant l'arrêt contrôlé du thread d'écoute.");
                }
            } catch (IOException e) {
                if (doitEcouter) {
                    journal.error("IOException lors de la lecture du serveur : {}. Déconnexion.", e.getMessage());
                    deconnecterDuServeur();
                    Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                            paquetRessourcesI18n.getString("error.connection.lost.title"),
                            paquetRessourcesI18n.getString("error.connection.lost.content.io")
                    ));
                }
            } finally {
                journal.info("Thread d'écoute serveur terminé.");
                if (doitEcouter) { // Si la boucle s'est terminée mais qu'on est censé écouter
                    deconnecterDuServeur(); // Assure que l'état est mis à jour
                }
            }
        });
    }

    private void traiterReponseServeur(String reponseBrute) {
        if (reponseBrute == null || reponseBrute.trim().isEmpty()) {
            journal.warn("Réponse vide reçue du serveur.");
            return;
        }
        String[] tokens = reponseBrute.split(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE, 2);
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
                        AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("signup.success.title"), donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("signup.success.content"));
                        break;
                    case INSCRIPTION_ECHEC:
                        dernierMessageErreurAuth.set(donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("error.signup.generic"));
                        break;
                    case LISTE_REUNIONS:
                    case LISTE_MES_REUNIONS: // Les deux mettent à jour des listes différentes ou la même
                        if (donneesJsonString != null) {
                            JSONArray reunionsJson = new JSONArray(donneesJsonString);
                            List<DetailsReunionDTO> reunions = new ArrayList<>();
                            for (int i = 0; i < reunionsJson.length(); i++) {
                                reunions.add(DetailsReunionDTO.fromJson(reunionsJson.getJSONObject(i).toString()));
                            }
                            if (typeReponse == TypeReponseServeur.LISTE_REUNIONS) {
                                listeToutesReunions.setAll(reunions);
                            } else {
                                listeMesReunions.setAll(reunions);
                            }
                        }
                        break;
                    case DETAILS_REUNION:
                    case REUNION_CREEE:
                    case REUNION_OUVERTE: // Le serveur peut renvoyer les détails mis à jour
                        if (donneesJsonString != null) {
                            detailsReunionActuelle.set(DetailsReunionDTO.fromJson(donneesJsonString));
                            if(typeReponse == TypeReponseServeur.REUNION_CREEE){
                                AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.creation.success.title"), paquetRessourcesI18n.getString("meeting.creation.success.content"));
                            }
                        }
                        break;
                    case REJOINDRE_OK:
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
                    case DONNEES_UTILISATEUR: // Pour OBTENIR_MON_PROFIL
                        if (donneesJsonString != null) {
                            profilUtilisateurActuel.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case LISTE_UTILISATEURS: // Pour ADMIN_LISTER_UTILISATEURS
                        if (donneesJsonString != null) {
                            JSONArray utilisateursArr = new JSONArray(donneesJsonString);
                            List<DonneesUtilisateurDTO> listeU = new ArrayList<>();
                            for (int i = 0; i < utilisateursArr.length(); i++) {
                                listeU.add(DonneesUtilisateurDTO.fromJson(utilisateursArr.getJSONObject(i).toString()));
                            }
                            listeGlobaleUtilisateurs.setAll(listeU);
                        }
                        break;
                    case ADMIN_UTILISATEUR_MODIFIE:
                        if (donneesJsonString != null) {
                            utilisateurMisAJourParAdmin.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                            AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("admin.user.update.success.title"), paquetRessourcesI18n.getString("admin.user.update.success.content"));
                        }
                        break;
                    case UTILISATEUR_REJOINT_REUNION:
                        if (donneesJsonString != null) {
                            // Le payload devrait être { "idReunion": X, "utilisateur": DonneesUtilisateurDTO_JSON }
                            JSONObject joinPayload = new JSONObject(donneesJsonString);
                            // long idReunionJoin = joinPayload.getLong("idReunion"); // Pour vérifier si c'est la réunion actuelle
                            notificationUtilisateurRejoint.set(DonneesUtilisateurDTO.fromJson(joinPayload.getJSONObject("utilisateur").toString()));
                        }
                        break;
                    case UTILISATEUR_QUITTE_REUNION:
                        if (donneesJsonString != null) {
                            JSONObject quitPayload = new JSONObject(donneesJsonString);
                            // long idReunionQuit = quitPayload.getLong("idReunion");
                            notificationUtilisateurQuitte.set(DonneesUtilisateurDTO.fromJson(quitPayload.getJSONObject("utilisateur").toString()));
                        }
                        break;
                    case NOTIFICATION_REUNION_CLOTUREE:
                    case REUNION_CLOTUREE: // Le serveur envoie l'ID de la réunion clôturée
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
                                codeErreur = errJson.optString("codeErreur", "");
                            } catch (JSONException e) { messageErreur = donneesJsonString; }
                        }
                        AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.server.title") + (codeErreur.isEmpty() ? "" : " ("+codeErreur+")"), messageErreur);
                        break;
                    case OPERATION_OK: // Pour déconnexion, etc.
                        // Pas besoin d'alerte spécifique souvent, l'UI réagira à la déconnexion de session.
                        break;
                    default:
                        journal.warn("Traitement UI non implémenté pour le type de réponse serveur : {}", typeReponse);
                }
            } catch (JSONException e) {
                journal.error("Erreur de parsing JSON pour la réponse {} avec payload '{}': {}", typeReponse, donneesJsonString, e.getMessage(), e);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.json.parsing.title"), paquetRessourcesI18n.getString("error.json.parsing.content") + "\n" + reponseBrute);
            } catch (Exception e) {
                journal.error("Erreur inattendue lors du traitement de la réponse UI {} : {}", typeReponse, e.getMessage(), e);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.response.processing.title"), paquetRessourcesI18n.getString("error.response.processing.content") + "\n" + e.getMessage());
            }
        });
    }

    public void envoyerRequeteConnexion(String identifiant, String motDePasse) {
        JSONObject json = new JSONObject();
        try {
            json.put("identifiant", identifiant);
            json.put("motDePasse", motDePasse);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.CONNEXION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Connexion: {}", e.getMessage());}
    }

    public void envoyerRequeteInscription(String identifiant, String motDePasse, String nomComplet) {
        JSONObject json = new JSONObject();
        try {
            json.put("identifiant", identifiant);
            json.put("motDePasse", motDePasse);
            json.put("nomComplet", nomComplet);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.INSCRIPTION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Inscription: {}", e.getMessage());}
    }

    public void envoyerRequeteDeconnexion() {
        envoyerRequeteSimple(TypeRequeteClient.DECONNEXION);
    }

    public void envoyerRequeteCreationReunion(DetailsReunionDTO details) {
        try {
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.NOUVELLE_REUNION, details.toJsonString());
        } catch (JSONException e) {journal.error("Erreur JSON Création Réunion: {}", e.getMessage());}
    }

    public void envoyerRequeteModificationReunion(DetailsReunionDTO details) {
        try {
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.MODIFIER_REUNION, details.toJsonString());
        } catch (JSONException e) {journal.error("Erreur JSON Modification Réunion: {}", e.getMessage());}
    }


    public void envoyerRequeteObtenirToutesLesReunions() {
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_REUNIONS);
    }

    public void envoyerRequeteObtenirMesReunions() {
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_MES_REUNIONS);
    }

    public void envoyerRequeteObtenirDetailsReunion(long idReunion) {
        JSONObject json = new JSONObject();
        try { json.put("idReunion", idReunion);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.OBTENIR_DETAILS_REUNION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Obtenir Détails Réunion: {}", e.getMessage());}
    }

    public void envoyerRequeteRejoindreReunion(long idReunion, String motDePasseFourni) {
        JSONObject json = new JSONObject();
        try {
            json.put("idReunion", idReunion);
            if (motDePasseFourni != null) {
                json.put("motDePasseFourni", motDePasseFourni);
            }
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.REJOINDRE_REUNION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Rejoindre Réunion: {}", e.getMessage());}
    }

    public void envoyerRequeteQuitterReunion(long idReunion) {
        JSONObject json = new JSONObject();
        try { json.put("idReunion", idReunion);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.QUITTER_REUNION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Quitter Réunion: {}", e.getMessage());}
    }

    public void envoyerMessageChat(MessageChatDTO message) {
        try {
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.MESSAGE_CHAT, message.toJsonString());
        } catch (JSONException e) {journal.error("Erreur JSON Message Chat: {}", e.getMessage());}
    }

    public void envoyerRequeteOuvrirReunion(long idReunion) {
        JSONObject json = new JSONObject();
        try { json.put("idReunion", idReunion);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.OUVRIR_REUNION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Ouvrir Réunion: {}", e.getMessage());}
    }

    public void envoyerRequeteCloturerReunion(long idReunion) {
        JSONObject json = new JSONObject();
        try { json.put("idReunion", idReunion);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.CLOTURER_REUNION, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Clore Réunion: {}", e.getMessage());}
    }

    public void envoyerRequeteObtenirHistoriqueMessages(long idReunion) {
        JSONObject json = new JSONObject();
        try { json.put("idReunion", idReunion);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.OBTENIR_HISTORIQUE_MESSAGES, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Historique Messages: {}", e.getMessage());}
    }

    public void envoyerRequeteObtenirMonProfil() {
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_MON_PROFIL);
    }

    public void envoyerRequeteModifierProfil(String nomComplet, String identifiant) {
        JSONObject json = new JSONObject();
        try {
            if (nomComplet != null) json.put("nomComplet", nomComplet);
            if (identifiant != null) json.put("identifiant", identifiant);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.MODIFIER_PROFIL, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Modifier Profil: {}", e.getMessage());}
    }

    public void envoyerRequeteChangerMotDePasse(String ancienMotDePasse, String nouveauMotDePasse) {
        JSONObject json = new JSONObject();
        try {
            json.put("motDePasseActuel", ancienMotDePasse);
            json.put("nouveauMotDePasse", nouveauMotDePasse);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.CHANGER_MOT_DE_PASSE, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Changer MDP: {}", e.getMessage());}
    }

    public void envoyerRequeteAdminObtenirUtilisateurs(String filtre) {
        JSONObject json = new JSONObject();
        try {
            if(filtre != null && !filtre.isEmpty()) json.put("filtre", filtre);
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.ADMIN_OBTENIR_UTILISATEURS, json.toString());
        } catch (JSONException e) {journal.error("Erreur JSON Admin Lister Utilisateurs: {}", e.getMessage());}
    }

    public void envoyerRequeteAdminModifierUtilisateur(DonneesUtilisateurDTO dto) {
        try {
            envoyerRequeteAvecPayloadJson(TypeRequeteClient.ADMIN_MODIFIER_UTILISATEUR, dto.toJsonString());
        } catch (JSONException e) {journal.error("Erreur JSON Admin Modifier Utilisateur: {}", e.getMessage());}
    }


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
    public ObjectProperty<DetailsReunionDTO> notificationReunionMiseAJourProperty() { return notificationReunionMiseAJour; }
    public IntegerProperty notificationReunionClotureeIdProperty() { return notificationReunionClotureeId; }
    public ObjectProperty<DonneesUtilisateurDTO> utilisateurMisAJourParAdminProperty() { return utilisateurMisAJourParAdmin;}
}