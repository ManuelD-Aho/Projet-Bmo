package akandan.bahou.kassy.serveur.noyau;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import akandan.bahou.kassy.serveur.configuration.ConfigurateurServeur;
import akandan.bahou.kassy.serveur.dao.GestionnaireConnexionBaseDeDonnees;
import akandan.bahou.kassy.serveur.dao.InterfaceUtilisateurDAO;
import akandan.bahou.kassy.serveur.dao.UtilisateurDAOImpl;
import akandan.bahou.kassy.serveur.dao.InterfaceReunionDAO;
import akandan.bahou.kassy.serveur.dao.ReunionDAOImpl;
import akandan.bahou.kassy.serveur.dao.InterfaceMessageChatDAO;
import akandan.bahou.kassy.serveur.dao.MessageChatDAOImpl;
import akandan.bahou.kassy.serveur.dao.InterfaceParticipationReunionDAO;
import akandan.bahou.kassy.serveur.dao.ParticipationReunionDAOImpl;
import akandan.bahou.kassy.serveur.service.ServiceAuthentification;
import akandan.bahou.kassy.serveur.service.ServiceGestionUtilisateurs;
import akandan.bahou.kassy.serveur.service.ServiceGestionReunions;
import akandan.bahou.kassy.serveur.service.ServiceCommunicationReunion;
import akandan.bahou.kassy.serveur.service.ServiceAdministration;
import akandan.bahou.kassy.serveur.util.AnalyseurRequeteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServeurPrincipalBMO {

    private static final Logger journal = LoggerFactory.getLogger(ServeurPrincipalBMO.class);
    private final int portEcoute;
    private final ConfigurateurServeur configurateurServeur;
    private final PoolThreadsServeur poolDeThreads;
    private final GestionnaireConnexionBaseDeDonnees gestionnaireDeConnexionsBD;
    private final InterfaceUtilisateurDAO utilisateurDAO;
    private final InterfaceReunionDAO reunionDAO;
    private final InterfaceMessageChatDAO messageChatDAO;
    private final InterfaceParticipationReunionDAO participationReunionDAO;
    private final ServiceAuthentification serviceAuthentification;
    private final ServiceGestionUtilisateurs serviceGestionUtilisateurs;
    private final ServiceGestionReunions serviceGestionReunions;
    private final ServiceCommunicationReunion serviceCommunicationReunion;
    private final ServiceAdministration serviceAdministration;
    private final AnalyseurRequeteClient analyseurDeRequetesClient;
    private volatile boolean serveurActif = true;
    private ServerSocket socketServeurEcoute;

    public ServeurPrincipalBMO() throws IOException {
        this.configurateurServeur = ConfigurateurServeur.obtenirInstance();
        this.portEcoute = this.configurateurServeur.recupererProprieteEntier("bmo.serveur.port", 5000);
        this.poolDeThreads = new PoolThreadsServeur(this.configurateurServeur);
        this.gestionnaireDeConnexionsBD = new GestionnaireConnexionBaseDeDonnees(this.configurateurServeur);

        this.utilisateurDAO = new UtilisateurDAOImpl(this.gestionnaireDeConnexionsBD);
        this.reunionDAO = new ReunionDAOImpl(this.gestionnaireDeConnexionsBD);
        this.messageChatDAO = new MessageChatDAOImpl(this.gestionnaireDeConnexionsBD);
        this.participationReunionDAO = new ParticipationReunionDAOImpl(this.gestionnaireDeConnexionsBD);

        this.serviceAuthentification = new ServiceAuthentification(this.utilisateurDAO);
        this.serviceGestionUtilisateurs = new ServiceGestionUtilisateurs(this.utilisateurDAO);
        this.serviceGestionReunions = new ServiceGestionReunions(this.reunionDAO, this.participationReunionDAO, this.utilisateurDAO);
        this.serviceCommunicationReunion = new ServiceCommunicationReunion(this.messageChatDAO, this.participationReunionDAO);
        this.serviceAdministration = new ServiceAdministration(this.utilisateurDAO, this.configurateurServeur);

        this.analyseurDeRequetesClient = new AnalyseurRequeteClient(this.serviceAuthentification, this.serviceGestionUtilisateurs, this.serviceGestionReunions, this.serviceCommunicationReunion, this.serviceAdministration);
        journal.info("Serveur BMO initialisé. Prêt à démarrer sur le port {}.", this.portEcoute);
    }

    public static void main(String[] args) {
        journal.info("Démarrage du Serveur Principal BMO...");
        try {
            ServeurPrincipalBMO serveur = new ServeurPrincipalBMO();
            serveur.enregistrerHookArret();
            serveur.demarrerEcoute();
        } catch (IOException e) {
            journal.error("Impossible de démarrer le serveur BMO en raison d'une erreur d'E/S.", e);
        } catch (Exception e) {
            journal.error("Une erreur critique est survenue lors de l'initialisation du serveur BMO.", e);
        }
    }

    public void demarrerEcoute() throws IOException {
        this.socketServeurEcoute = new ServerSocket(this.portEcoute);
        journal.info("Serveur BMO démarré et en écoute sur le port {}.", this.portEcoute);
        while (this.serveurActif && !this.socketServeurEcoute.isClosed()) {
            try {
                journal.debug("En attente d'une nouvelle connexion client...");
                Socket socketClient = this.socketServeurEcoute.accept();
                journal.info("Nouvelle connexion client acceptée de : {}:{}", socketClient.getInetAddress().getHostAddress(), socketClient.getPort());
                ThreadClientDedie gestionnaireClient = new ThreadClientDedie(socketClient, this.analyseurDeRequetesClient);
                this.poolDeThreads.soumettreTache(gestionnaireClient);
            } catch (java.net.SocketException se) {
                if (this.serveurActif) {
                    journal.error("SocketException lors de l'acceptation d'une connexion client (le socket serveur a peut-être été fermé).", se);
                } else {
                    journal.info("ServerSocket fermé, arrêt de la boucle d'acceptation.");
                }
            } catch (IOException ioe) {
                journal.error("Erreur d'E/S lors de l'acceptation d'une connexion client.", ioe);
            }
        }
        journal.info("La boucle d'écoute du serveur BMO est terminée.");
    }

    private void enregistrerHookArret() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            journal.info("Hook d'arrêt déclenché. Arrêt du serveur BMO...");
            this.arreterServeur();
        }));
    }

    public void arreterServeur() {
        this.serveurActif = false;
        journal.info("Arrêt du pool de threads...");
        if (this.poolDeThreads != null) {
            this.poolDeThreads.arreterPoolProprement();
        }
        journal.info("Fermeture du socket serveur d'écoute...");
        try {
            if (this.socketServeurEcoute != null && !this.socketServeurEcoute.isClosed()) {
                this.socketServeurEcoute.close();
            }
        } catch (IOException e) {
            journal.error("Erreur lors de la fermeture du socket serveur d'écoute.", e);
        }
        journal.info("Serveur BMO arrêté.");
    }
}