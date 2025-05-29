package akandan.bahou.kassy.serveur.noyau;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import akandan.bahou.kassy.serveur.configuration.ConfigurateurServeur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolThreadsServeur {

    private final ExecutorService executeurDeTaches;
    private static final Logger journal = LoggerFactory.getLogger(PoolThreadsServeur.class);

    public PoolThreadsServeur(ConfigurateurServeur configurateur) {
        int tailleMaximaleDuPool = configurateur.recupererProprieteEntier("bmo.poolthreads.taillemaximale", 10);
        this.executeurDeTaches = Executors.newFixedThreadPool(tailleMaximaleDuPool);
        journal.info("Pool de threads serveur initialisé avec une taille maximale de {}.", tailleMaximaleDuPool);
    }

    public void soumettreTache(Runnable tache) {
        if (estActif()) {
            this.executeurDeTaches.submit(tache);
            journal.debug("Nouvelle tâche soumise au pool de threads.");
        } else {
            journal.warn("Tentative de soumission d'une tâche à un pool de threads arrêté ou en cours d'arrêt. Tâche ignorée.");
        }
    }

    public void arreterPoolProprement() {
        journal.info("Initiation de l'arrêt ordonné du pool de threads serveur...");
        this.executeurDeTaches.shutdown();
        try {
            if (!this.executeurDeTaches.awaitTermination(60, TimeUnit.SECONDS)) {
                journal.warn("Le pool de threads n'a pas terminé toutes ses tâches dans le délai imparti. Forçage de l'arrêt...");
                this.executeurDeTaches.shutdownNow();
                if (!this.executeurDeTaches.awaitTermination(60, TimeUnit.SECONDS)) {
                    journal.error("Le pool de threads n'a pas pu être arrêté proprement.");
                }
            }
        } catch (InterruptedException ie) {
            journal.error("Attente de terminaison du pool de threads interrompue. Forçage de l'arrêt...", ie);
            this.executeurDeTaches.shutdownNow();
            Thread.currentThread().interrupt();
        }
        journal.info("Pool de threads serveur arrêté.");
    }

    public boolean estActif() {
        return !this.executeurDeTaches.isShutdown() && !this.executeurDeTaches.isTerminated();
    }
}