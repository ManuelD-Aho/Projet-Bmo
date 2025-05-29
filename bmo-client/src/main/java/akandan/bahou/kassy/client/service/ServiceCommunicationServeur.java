package akandan.bahou.kassy.client.service;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.protocole.TypeRequeteClient;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.dto.*;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;

import org.slf4j.Logger;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class ServiceCommunicationServeur {

    private Socket socketClient;
    private PrintWriter fluxSortie;
    private BufferedReader fluxEntree;
    private ExecutorService serviceEcouteServeur;
    private volatile boolean estConnecte = false;
    private final ServiceSessionUtilisateur serviceSessionUtilisateur;
    private final ResourceBundle paquetRessourcesI18n;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ServiceCommunicationServeur.class);

    private final StringProperty dernierMessageErreurServeur = new SimpleStringProperty();
    private final ObjectProperty<MessageChatDTO> dernierMessageChatRecu = new SimpleObjectProperty<>();
    private final ObjectProperty<DetailsReunionDTO> detailsReunionMiseAJour = new SimpleObjectProperty<>();
    private final ListProperty<DetailsReunionDTO> listeReunionsMiseAJour = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<DonneesUtilisateurDTO> profilUtilisateurMisAJour = new SimpleObjectProperty<>();
    private final ListProperty<DonneesUtilisateurDTO> listeUtilisateursMiseAJour = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Boolean> etatConnexionServeur = new SimpleObjectProperty<>(false);


    public ServiceCommunicationServeur(ServiceSessionUtilisateur serviceSession, ResourceBundle paquetRessources) {
        this.serviceSessionUtilisateur = serviceSession;
        this.paquetRessourcesI18n = paquetRessources;
        initialiserServiceEcoute();
    }

    private void initialiserServiceEcoute() {
        this.serviceEcouteServeur = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("ThreadEcouteServeurBMO");
            t.setDaemon(true);
            return t;
        });
    }

    public boolean connecterAuServeur(String adresseServeur, int portServeur) {
        if (estConnecte) {
            journal.info("Déjà connecté au serveur.");
            return true;
        }
        try {
            this.socketClient = new Socket(adresseServeur, portServeur);
            this.fluxSortie = new PrintWriter(this.socketClient.getOutputStream(), true, StandardCharsets.UTF_8);
            this.fluxEntree = new BufferedReader(new InputStreamReader(this.socketClient.getInputStream(), StandardCharsets.UTF_8));
            this.estConnecte = true;
            Platform.runLater(() -> etatConnexionServeur.set(true));
            lancerEcouteServeur();
            journal.info("Connecté au serveur BMO sur {}:{}", adresseServeur, portServeur);
            return true;
        } catch (IOException e) {
            journal.error("Impossible de se connecter au serveur BMO {}:{}. Raison : {}", adresseServeur, portServeur, e.getMessage());
            this.estConnecte = false;
            Platform.runLater(() -> etatConnexionServeur.set(false));
            return false;
        }
    }

    public void deconnecterDuServeur() {
        if (!estConnecte && (serviceEcouteServeur == null || serviceEcouteServeur.isShutdown())) {
            journal.info("Déjà déconnecté ou jamais connecté.");
            return;
        }

        this.estConnecte = false; // Arrête la boucle d'écoute
        Platform.runLater(() -> etatConnexionServeur.set(false));

        if (serviceEcouteServeur != null && !serviceEcouteServeur.isShutdown()) {
            serviceEcouteServeur.shutdown();
            try {
                if (!serviceEcouteServeur.awaitTermination(2, TimeUnit.SECONDS)) {
                    serviceEcouteServeur.shutdownNow();
                }
            } catch (InterruptedException ie) {
                serviceEcouteServeur.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        initialiserServiceEcoute(); // Prépare pour une reconnexion future

        try {
            if (fluxSortie != null) fluxSortie.close();
            if (fluxEntree != null) fluxEntree.close();
            if (socketClient != null && !socketClient.isClosed()) socketClient.close();
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture des flux ou du socket : {}", e.getMessage());
        } finally {
            fluxSortie = null;
            fluxEntree = null;
            socketClient = null;
            serviceSessionUtilisateur.viderSession();
            journal.info("Déconnecté du serveur BMO.");
        }
    }

    public boolean estActuellementConnecte() {
        return estConnecte && socketClient != null && socketClient.isConnected() && !socketClient.isClosed();
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
            deconnecterDuServeur(); // Peut-être un peu radical, mais si on essaie d'envoyer sans être co, c'est un problème.
        }
    }

    private void lancerEcouteServeur() {
        if (serviceEcouteServeur.isShutdown()) {
            initialiserServiceEcoute();
        }
        serviceEcouteServeur.submit(() -> {
            try {
                String ligneDuServeur;
                while (estConnecte && fluxEntree != null && (ligneDuServeur = fluxEntree.readLine()) != null) {
                    journal.debug("Reçu du serveur : {}", ligneDuServeur);
                    traiterReponseServeur(ligneDuServeur);
                }
            } catch (IOException e) {
                if (estConnecte) {
                    journal.error("Erreur de lecture du serveur : {}. Déconnexion.", e.getMessage());
                    Platform.runLater(() -> AlertesUtilisateur.afficherErreur(
                            paquetRessourcesI18n.getString("error.connection.lost.title"),
                            paquetRessourcesI18n.getString("error.connection.lost.content") + "\n" + e.getMessage()
                    ));
                    deconnecterDuServeur();
                }
            } finally {
                if (estConnecte) { // Si la boucle s'est terminée mais qu'on est tjrs "connecté" logiquement
                    journal.info("Thread d'écoute serveur terminé, déconnexion inattendue possible.");
                    deconnecterDuServeur();
                }
            }
        });
    }

    private void traiterReponseServeur(String reponseBrute) {
        if (reponseBrute == null || reponseBrute.trim().isEmpty()) {
            journal.warn("Réponse vide reçue du serveur.");
            return;
        }
        String[] tokens = reponseBrute.split(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE, -1);
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
                            JSONObject authOkJson = new JSONObject(donneesJsonString);
                            serviceSessionUtilisateur.definirSession(
                                    authOkJson.getLong("idUtilisateur"),
                                    authOkJson.getString("nomUtilisateur"),
                                    RoleUtilisateur.valueOf(authOkJson.getString("roleUtilisateur"))
                            );
                            dernierMessageErreurServeur.set(""); // Effacer les erreurs précédentes
                        } else {
                            journal.error("Données manquantes pour AUTH_OK");
                        }
                        break;
                    case AUTH_ECHEC:
                        dernierMessageErreurServeur.set(donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("error.auth.generic"));
                        break;
                    case INSCRIPTION_OK:
                        // Peut-être un message pour l'UI, ou rien si la navigation se fait
                        break;
                    case INSCRIPTION_ECHEC:
                        dernierMessageErreurServeur.set(donneesJsonString != null ? donneesJsonString : paquetRessourcesI18n.getString("error.signup.generic"));
                        break;
                    case LISTE_REUNIONS:
                    case MES_REUNIONS:
                        if (donneesJsonString != null) {
                            JSONArray reunionsJson = new JSONArray(donneesJsonString);
                            List<DetailsReunionDTO> reunions = new ArrayList<>();
                            for (int i = 0; i < reunionsJson.length(); i++) {
                                reunions.add(DetailsReunionDTO.fromJson(reunionsJson.getJSONObject(i).toString()));
                            }
                            listeReunionsMiseAJour.setAll(reunions);
                        }
                        break;
                    case DETAILS_REUNION:
                        if (donneesJsonString != null) {
                            detailsReunionMiseAJour.set(DetailsReunionDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case REUNION_CREEE:
                        if (donneesJsonString != null) {
                            detailsReunionMiseAJour.set(DetailsReunionDTO.fromJson(donneesJsonString)); // Le serveur renvoie les détails de la réunion créée
                            AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.creation.success.title"), paquetRessourcesI18n.getString("meeting.creation.success.content"));
                        }
                        break;
                    case REUNION_MODIFIEE:
                        if (donneesJsonString != null) {
                            detailsReunionMiseAJour.set(DetailsReunionDTO.fromJson(donneesJsonString));
                            AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.update.success.title"), paquetRessourcesI18n.getString("meeting.update.success.content"));
                        }
                        break;
                    case REUNION_REJOINTE:
                        if (donneesJsonString != null) {
                            detailsReunionMiseAJour.set(DetailsReunionDTO.fromJson(donneesJsonString)); // Détails de la réunion rejointe
                        }
                        break;
                    case NOUVEAU_MESSAGE_CHAT:
                        if (donneesJsonString != null) {
                            dernierMessageChatRecu.set(MessageChatDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case HISTORIQUE_MESSAGES:
                        // Similaire à LISTE_REUNIONS, mais pour une liste de MessageChatDTO
                        // Vous aurez besoin d'une ListProperty<MessageChatDTO>
                        break;
                    case MON_PROFIL:
                        if (donneesJsonString != null) {
                            profilUtilisateurMisAJour.set(DonneesUtilisateurDTO.fromJson(donneesJsonString));
                        }
                        break;
                    case ERREUR:
                        String messageErreur = paquetRessourcesI18n.getString("error.server.generic");
                        if (donneesJsonString != null) {
                            try {
                                JSONObject erreurJson = new JSONObject(donneesJsonString);
                                messageErreur = erreurJson.optString("message", messageErreur);
                                String codeErreur = erreurJson.optString("code", "");
                                if (!codeErreur.isEmpty()) {
                                    messageErreur = "(" + codeErreur + ") " + messageErreur;
                                }
                            } catch (JSONException e) {
                                messageErreur = donneesJsonString; // Si ce n'est pas du JSON, afficher tel quel
                            }
                        }
                        AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.server.title"), messageErreur);
                        break;

                    // ... Autres cas pour TypeReponseServeur
                    default:
                        journal.warn("Traitement non implémenté pour le type de réponse serveur : {}", typeReponse);
                }
            } catch (JSONException e) {
                journal.error("Erreur de parsing JSON pour la réponse {}: {}", typeReponse, e.getMessage());
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.json.parsing.title"), paquetRessourcesI18n.getString("error.json.parsing.content") + "\n" + reponseBrute);
            } catch (Exception e) {
                journal.error("Erreur inattendue lors du traitement de la réponse {} : {}", typeReponse, e.getMessage(), e);
                AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.response.processing.title"), paquetRessourcesI18n.getString("error.response.processing.content") + "\n" + e.getMessage());
            }
        });
    }

    private void envoyerRequeteAvecDonneesJson(TypeRequeteClient typeRequete, Object dto) {
        if (!estActuellementConnecte()) {
            journal.warn("Tentative d'envoi de requête {} alors que non connecté.", typeRequete.name());
            return;
        }
        try {
            String dtoEnJson = dto != null ? new JSONObject(dto).toString() : "";
            String requete = typeRequete.name() + (dtoEnJson.isEmpty() ? "" : ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + dtoEnJson);
            envoyerMessageAuServeur(requete);
        } catch (JSONException e) {
            journal.error("Erreur de conversion DTO en JSON pour la requête {} : {}", typeRequete.name(), e.getMessage());
        }
    }

    private void envoyerRequeteSimple(TypeRequeteClient typeRequete) {
        envoyerRequeteAvecDonneesJson(typeRequete, null);
    }

    private void envoyerRequeteAvecParametres(TypeRequeteClient typeRequete, String... parametres) {
        if (!estActuellementConnecte()) {
            journal.warn("Tentative d'envoi de requête {} alors que non connecté.", typeRequete.name());
            return;
        }
        StringBuilder sb = new StringBuilder(typeRequete.name());
        for (String param : parametres) {
            sb.append(ConstantesProtocoleBMO.DELIMITEUR_COMMANDE).append(param == null ? "" : param);
        }
        envoyerMessageAuServeur(sb.toString());
    }


    public void envoyerRequeteConnexion(String identifiant, String motDePasse) {
        JSONObject json = new JSONObject();
        json.put("identifiant", identifiant);
        json.put("motDePasse", motDePasse);
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.CONNEXION, json);
    }

    public void envoyerRequeteInscription(String identifiant, String motDePasse, String nomComplet) {
        JSONObject json = new JSONObject();
        json.put("identifiant", identifiant);
        json.put("motDePasse", motDePasse);
        json.put("nomComplet", nomComplet);
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.INSCRIPTION, json);
    }

    public void envoyerRequeteDeconnexion() {
        envoyerRequeteSimple(TypeRequeteClient.DECONNEXION);
        // La déconnexion locale se fera via deconnecterDuServeur() qui devrait être appelée par le thread d'écoute
        // ou par l'UI après avoir reçu confirmation du serveur ou en cas de fermeture.
    }

    public void envoyerRequeteCreationReunion(DetailsReunionDTO details) {
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.NOUVELLE_REUNION, details);
    }

    public void envoyerRequeteModificationReunion(DetailsReunionDTO details) {
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.MODIFIER_REUNION, details);
    }

    public void envoyerRequeteObtenirToutesLesReunions() {
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_REUNIONS);
    }

    public void envoyerRequeteObtenirMesReunions() {
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_MES_REUNIONS);
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

    public void envoyerMessageChat(MessageChatDTO message) {
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.MESSAGE_CHAT, message);
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
        envoyerRequeteSimple(TypeRequeteClient.OBTENIR_MON_PROFIL);
    }

    public void envoyerRequeteModifierProfil(DonneesUtilisateurDTO donneesUtilisateur) {
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.MODIFIER_PROFIL, donneesUtilisateur);
    }

    public void envoyerRequeteChangerMotDePasse(String ancienMotDePasse, String nouveauMotDePasse) {
        JSONObject json = new JSONObject();
        json.put("ancienMotDePasse", ancienMotDePasse);
        json.put("nouveauMotDePasse", nouveauMotDePasse);
        envoyerRequeteAvecDonneesJson(TypeRequeteClient.CHANGER_MOT_DE_PASSE, json);
    }

    // Getters pour les Propriétés Observables
    public StringProperty dernierMessageErreurServeurProperty() { return dernierMessageErreurServeur; }
    public ObjectProperty<MessageChatDTO> dernierMessageChatRecuProperty() { return dernierMessageChatRecu; }
    public ObjectProperty<DetailsReunionDTO> detailsReunionMiseAJourProperty() { return detailsReunionMiseAJour; }
    public ListProperty<DetailsReunionDTO> listeReunionsMiseAJourProperty() { return listeReunionsMiseAJour; }
    public ObjectProperty<DonneesUtilisateurDTO> profilUtilisateurMisAJourProperty() { return profilUtilisateurMisAJour; }
    public ListProperty<DonneesUtilisateurDTO> listeUtilisateursMiseAJourProperty() { return listeUtilisateursMiseAJour; }
    public ObjectProperty<Boolean> etatConnexionServeurProperty() { return etatConnexionServeur; }
}