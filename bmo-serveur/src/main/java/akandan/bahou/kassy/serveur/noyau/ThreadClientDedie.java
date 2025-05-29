package akandan.bahou.kassy.serveur.noyau;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import akandan.bahou.kassy.serveur.util.AnalyseurRequeteClient;
import akandan.bahou.kassy.commun.util.ConstantesProtocoleBMO;
import akandan.bahou.kassy.commun.protocole.TypeReponseServeur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadClientDedie implements Runnable {

    private final Socket socketClientCommunication;
    private final AnalyseurRequeteClient analyseurDeRequetes;
    private PrintWriter fluxSortieVersClient;
    private BufferedReader fluxEntreeDepuisClient;
    private static final Logger journal = LoggerFactory.getLogger(ThreadClientDedie.class);
    private volatile boolean clientConnecte = true;
    private Integer idUtilisateurAuthentifie = null;
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
            while (this.clientConnecte && (ligneRequeteClient = this.fluxEntreeDepuisClient.readLine()) != null) {
                journal.debug("Reçu du client {}: {}", this.socketClientCommunication.getRemoteSocketAddress(), ligneRequeteClient);
                String reponseServeur = this.analyseurDeRequetes.traiterRequete(ligneRequeteClient, this);
                if (reponseServeur != null && !reponseServeur.isEmpty()) {
                    envoyerReponseAuClient(reponseServeur);
                }
            }
        } catch (java.io.EOFException eofe) {
            journal.info("Le client {} a fermé la connexion (EOFException).", this.socketClientCommunication.getRemoteSocketAddress());
        } catch (java.net.SocketException se) {
            journal.warn("SocketException pour le client {} (connexion probablement réinitialisée ou fermée) : {}", this.socketClientCommunication.getRemoteSocketAddress(), se.getMessage());
        } catch (IOException ioe) {
            journal.error("Erreur d'E/S lors de la communication avec le client {} : {}", this.socketClientCommunication.getRemoteSocketAddress(), ioe.getMessage(), ioe);
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
        }
    }

    private void deconnecterClientProprement() {
        this.clientConnecte = false;
        journal.info("Déconnexion du client : {}", this.socketClientCommunication.getRemoteSocketAddress());
        try {
            if (this.fluxEntreeDepuisClient != null) this.fluxEntreeDepuisClient.close();
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture du flux d'entrée pour {}.", this.socketClientCommunication.getRemoteSocketAddress(), e);
        }
        try {
            if (this.fluxSortieVersClient != null) this.fluxSortieVersClient.close();
        } catch (Exception e) { // PrintWriter.close() ne lève pas IOException.
            journal.warn("Erreur lors de la fermeture du flux de sortie pour {}.", this.socketClientCommunication.getRemoteSocketAddress(), e);
        }
        try {
            if (this.socketClientCommunication != null && !this.socketClientCommunication.isClosed()) this.socketClientCommunication.close();
        } catch (IOException e) {
            journal.warn("Erreur lors de la fermeture du socket client pour {}.", this.socketClientCommunication.getRemoteSocketAddress(), e);
        }
        journal.info("Client {} déconnecté et ressources libérées.", this.socketClientCommunication.getRemoteSocketAddress());
    }

    public void definirUtilisateurAuthentifie(int idUtilisateur, String nomUtilisateur) {
        this.idUtilisateurAuthentifie = idUtilisateur;
        this.nomUtilisateurAuthentifie = nomUtilisateur;
        journal.info("Client {} authentifié comme utilisateur ID: {}, Nom: {}.", this.socketClientCommunication.getRemoteSocketAddress(), idUtilisateur, nomUtilisateur);
    }

    public Integer obtenirIdUtilisateurAuthentifie() {
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
}