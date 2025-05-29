package akandan.bahou.kassy.serveur.noyau;

import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.serveur.util.AnalyseurRequeteClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadClientDedie implements Runnable {

    private final Socket socketClientCommunication;
    private final AnalyseurRequeteClient analyseurDeRequetes;
    private PrintWriter fluxSortieVersClient;
    private BufferedReader fluxEntreeDepuisClient;
    private static final Logger journal = LoggerFactory.getLogger(ThreadClientDedie.class);
    private volatile boolean clientConnecte = true;
    private Long idUtilisateurAuthentifie = null; // Corrigé en Long
    private String nomUtilisateurAuthentifie = null;

    public ThreadClientDedie(Socket socketClient, AnalyseurRequeteClient analyseur) {
        this.socketClientCommunication = socketClient;
        this.analyseurDeRequetes = analyseur;
        journal.debug("ThreadClientDedie créé pour le client : {}", socketClient.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        try {
            this.fluxEntreeDepuisClient = new BufferedReader(new InputStreamReader(this.socketClientCommunication.getInputStream(), StandardCharsets.UTF_8));
            this.fluxSortieVersClient = new PrintWriter(this.socketClientCommunication.getOutputStream(), true, StandardCharsets.UTF_8);
            journal.info("Flux de communication établis pour le client : {}", this.socketClientCommunication.getRemoteSocketAddress());

            String ligneRequeteClient;
            while (this.clientConnecte && this.socketClientCommunication.isConnected() && !this.socketClientCommunication.isClosed() && (ligneRequeteClient = this.fluxEntreeDepuisClient.readLine()) != null) {
                journal.debug("Reçu du client {}: {}", this.socketClientCommunication.getRemoteSocketAddress(), ligneRequeteClient);
                String reponseServeur = this.analyseurDeRequetes.traiterRequete(ligneRequeteClient, this);
                if (reponseServeur != null && !reponseServeur.isEmpty()) {
                    envoyerReponseAuClient(reponseServeur);
                }
            }
        } catch (java.io.EOFException eofe) {
            journal.info("Le client {} a fermé la connexion (EOFException).", this.socketClientCommunication.getRemoteSocketAddress());
        } catch (java.net.SocketException se) {
            if (this.clientConnecte) { // N'afficher l'erreur que si la déconnexion n'était pas attendue
                journal.warn("SocketException pour le client {} (connexion probablement réinitialisée ou fermée) : {}", this.socketClientCommunication.getRemoteSocketAddress(), se.getMessage());
            } else {
                journal.info("Socket fermé comme prévu pour le client {}.", this.socketClientCommunication.getRemoteSocketAddress());
            }
        } catch (IOException ioe) {
            if (this.clientConnecte) {
                journal.error("Erreur d'E/S lors de la communication avec le client {} : {}", this.socketClientCommunication.getRemoteSocketAddress(), ioe.getMessage(), ioe);
            }
        } catch (Exception e) {
            journal.error("Erreur inattendue lors du traitement du client {} : {}", this.socketClientCommunication.getRemoteSocketAddress(), e.getMessage(), e);
            envoyerReponseAuClient(TypeReponseServeur.ERREUR.getValeurProtocole() + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + "ERREUR_INTERNE" + ConstantesProtocoleBMO.DELIMITEUR_COMMANDE + "Une erreur interne est survenue.");
        } finally {
            deconnecterClientProprement();
        }
    }

    public void envoyerReponseAuClient(String messageReponse) {
        if (this.fluxSortieVersClient != null && !this.fluxSortieVersClient.checkError()) {
            this.fluxSortieVersClient.println(messageReponse);
            journal.debug("Envoyé au client {}: {}", this.socketClientCommunication.getRemoteSocketAddress(), messageReponse);
        } else {
            journal.warn("Impossible d'envoyer la réponse au client {}, le flux de sortie n'est pas disponible ou en erreur.", this.socketClientCommunication.getRemoteSocketAddress());
            this.clientConnecte = false; // Marquer comme déconnecté si on ne peut plus écrire
        }
    }

    private void deconnecterClientProprement() {
        if (!this.clientConnecte && this.fluxEntreeDepuisClient == null && this.fluxSortieVersClient == null && (this.socketClientCommunication == null || this.socketClientCommunication.isClosed())) {
            // Déjà nettoyé
            return;
        }
        this.clientConnecte = false;
        journal.info("Déconnexion propre du client : {}", this.socketClientCommunication != null ? this.socketClientCommunication.getRemoteSocketAddress() : "Socket Inconnu");
        try {
            if (this.fluxEntreeDepuisClient != null) this.fluxEntreeDepuisClient.close();
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture du flux d'entrée pour {}.", this.socketClientCommunication != null ? this.socketClientCommunication.getRemoteSocketAddress() : "Socket Inconnu", e);
        }
        if (this.fluxSortieVersClient != null) this.fluxSortieVersClient.close();
        try {
            if (this.socketClientCommunication != null && !this.socketClientCommunication.isClosed()) this.socketClientCommunication.close();
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture du socket client pour {}.", this.socketClientCommunication != null ? this.socketClientCommunication.getRemoteSocketAddress() : "Socket Inconnu", e);
        }
        this.fluxEntreeDepuisClient = null;
        this.fluxSortieVersClient = null;
        // this.socketClientCommunication = null; // Ne pas mettre à null car final

        journal.info("Client {} déconnecté et ressources libérées.", this.socketClientCommunication != null ? this.socketClientCommunication.getRemoteSocketAddress() : "Socket Inconnu");
    }

    public void definirUtilisateurAuthentifie(long idUtilisateur, String nomUtilisateur) { // Corrigé en long
        this.idUtilisateurAuthentifie = idUtilisateur;
        this.nomUtilisateurAuthentifie = nomUtilisateur;
        journal.info("Client {} authentifié comme utilisateur ID: {}, Nom: {}.", this.socketClientCommunication.getRemoteSocketAddress(), idUtilisateur, nomUtilisateur);
    }

    public Long obtenirIdUtilisateurAuthentifie() { // Corrigé en Long
        return this.idUtilisateurAuthentifie;
    }

    public String obtenirNomUtilisateurAuthentifie() {
        return this.nomUtilisateurAuthentifie;
    }

    public void reinitialiserAuthentification() {
        journal.info("Réinitialisation de l'authentification pour le client {}.", this.socketClientCommunication.getRemoteSocketAddress());
        this.idUtilisateurAuthentifie = null;
        this.nomUtilisateurAuthentifie = null;
    }

    public Socket getSocketClientCommunication() {
        return this.socketClientCommunication;
    }

    public void setClientConnecte(boolean clientConnecte) {
        this.clientConnecte = clientConnecte;
        if(!clientConnecte){
            deconnecterClientProprement();
        }
    }
}