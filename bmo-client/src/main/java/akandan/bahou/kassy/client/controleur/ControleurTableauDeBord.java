package akandan.bahou.kassy.client.controleur;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Optional;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.modele.ModeleObservableReunion;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurTableauDeBord implements ControleurAvecInitialisation {

    @FXML private Label etiquetteNomUtilisateur;
    @FXML private Label etiquetteRoleUtilisateur;
    @FXML private Button boutonDeconnexion;
    @FXML private Button boutonCreerReunion;
    @FXML private Button boutonAdministrationUtilisateurs;
    @FXML private Button boutonMonProfil;
    @FXML private TableView<ModeleObservableReunion> tableVueReunions;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneTitreReunion;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneDescriptionReunion;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneDateDebutReunion;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneStatutReunion;
    @FXML private TableColumn<ModeleObservableReunion, Void> colonneActionsReunion;
    @FXML private ComboBox<String> comboBoxFiltreReunions;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private final ObservableList<ModeleObservableReunion> listeObservableReunions = FXCollections.observableArrayList();
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurTableauDeBord.class);
    private final DateTimeFormatter formateurDateHeure = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String FILTRE_TOUTES_LES_REUNIONS_KEY = "filter.meetings.all";
    private static final String FILTRE_MES_REUNIONS_CREEES_KEY = "filter.meetings.mycreated";
    private static final String FILTRE_MES_PARTICIPATIONS_KEY = "filter.meetings.myparticipations";


    public ControleurTableauDeBord() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        DonneesUtilisateurDTO utilisateur = serviceSessionUtilisateur.getUtilisateurConnecte();
        if (utilisateur != null) {
            etiquetteNomUtilisateur.setText(utilisateur.getNomComplet());
            etiquetteRoleUtilisateur.setText(paquetRessourcesI18n.getString("role." + utilisateur.getRole().name().toLowerCase()));
        }

        boutonAdministrationUtilisateurs.setVisible(serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR));
        boutonAdministrationUtilisateurs.setManaged(serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR));

        comboBoxFiltreReunions.getItems().addAll(
                paquetRessourcesI18n.getString(FILTRE_TOUTES_LES_REUNIONS_KEY),
                paquetRessourcesI18n.getString(FILTRE_MES_REUNIONS_CREEES_KEY)
                // FILTRE_MES_PARTICIPATIONS_KEY sera ajouté si le service le supporte
        );
        comboBoxFiltreReunions.getSelectionModel().selectFirst();

        serviceCommunicationServeur.listeReunionsMiseAJourProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            Platform.runLater(() -> {
                listeObservableReunions.clear();
                if (nouvelleListe != null) {
                    nouvelleListe.forEach(dto -> listeObservableReunions.add(new ModeleObservableReunion(dto)));
                }
                tableVueReunions.refresh(); // Assurer le rafraîchissement visuel
            });
        });

        serviceCommunicationServeur.detailsReunionMiseAJourProperty().addListener((obs, ancienneReunion, reunionMaj) -> {
            if (reunionMaj != null) {
                Platform.runLater(() -> {
                    Optional<ModeleObservableReunion> optReunionExistante = listeObservableReunions.stream()
                            .filter(r -> r.getId() == reunionMaj.getIdReunion())
                            .findFirst();
                    if (optReunionExistante.isPresent()) {
                        optReunionExistante.get().mettreAJourAvecDTO(reunionMaj);
                    } else {
                        listeObservableReunions.add(new ModeleObservableReunion(reunionMaj));
                    }
                    tableVueReunions.refresh();
                });
            }
        });


        actualiserListeReunions();
    }

    @FXML
    private void initialize() {
        colonneTitreReunion.setCellValueFactory(cellData -> cellData.getValue().titreProperty());
        colonneDescriptionReunion.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        colonneDateDebutReunion.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(cellData.getValue().getDateHeureDebut().format(formateurDateHeure));
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        colonneStatutReunion.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(paquetRessourcesI18n.getString("statut.reunion." + cellData.getValue().getStatut().name().toLowerCase()));
            } catch (Exception e) {
                return new SimpleStringProperty(cellData.getValue().getStatut().name());
            }
        });

        configurerColonneActions();
        tableVueReunions.setItems(listeObservableReunions);
        tableVueReunions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        comboBoxFiltreReunions.setOnAction(this::actionChangerFiltreReunions);
    }

    private void configurerColonneActions() {
        Callback<TableColumn<ModeleObservableReunion, Void>, TableCell<ModeleObservableReunion, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ModeleObservableReunion, Void> call(final TableColumn<ModeleObservableReunion, Void> param) {
                final TableCell<ModeleObservableReunion, Void> cell = new TableCell<>() {
                    private final Button boutonRejoindreVoir = new Button();
                    private final Button boutonModifierDetails = new Button();
                    private final Button boutonOuvrir = new Button();
                    private final Button boutonCloturer = new Button();
                    private final HBox conteneurActions = new HBox(5);

                    {
                        conteneurActions.getChildren().addAll(boutonRejoindreVoir, boutonModifierDetails, boutonOuvrir, boutonCloturer);
                        boutonRejoindreVoir.setOnAction((ActionEvent event) -> {
                            ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                            if (reunion.getStatut() == StatutReunion.OUVERTE) {
                                gestionnaireNavigation.afficherVueSalleReunion(reunion.getDetailsDTO());
                            } else {
                                // Afficher détails ou dialogue d'info
                                AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.details.title"), reunion.getTitre() + "\n" + reunion.getDescription());
                            }
                        });
                        boutonModifierDetails.setOnAction((ActionEvent event) -> {
                            ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                            if (estCreateur(reunion) && reunion.getStatut() == StatutReunion.PLANIFIEE) {
                                gestionnaireNavigation.afficherVuePlanificationReunion(reunion.getDetailsDTO());
                            } else {
                                // Afficher détails en lecture seule
                                AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("meeting.details.title"), reunion.getTitre() + "\n" + paquetRessourcesI18n.getString("meeting.details.organizer") + ": " + reunion.getNomOrganisateur());
                            }
                        });
                        boutonOuvrir.setOnAction((ActionEvent event) -> {
                            ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                            serviceCommunicationServeur.envoyerRequeteOuvrirReunion(reunion.getId());
                        });
                        boutonCloturer.setOnAction((ActionEvent event) -> {
                            ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                            serviceCommunicationServeur.envoyerRequeteCloturerReunion(reunion.getId());
                        });
                    }

                    private boolean estCreateur(ModeleObservableReunion reunion) {
                        DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
                        return utilisateurActuel != null && reunion.getIdOrganisateur() == utilisateurActuel.getIdUtilisateur();
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                            boutonRejoindreVoir.setDisable(false);
                            boutonModifierDetails.setDisable(false);
                            boutonOuvrir.setDisable(true);
                            boutonOuvrir.setVisible(false);
                            boutonCloturer.setDisable(true);
                            boutonCloturer.setVisible(false);

                            if (reunion.getStatut() == StatutReunion.OUVERTE) {
                                boutonRejoindreVoir.setText(paquetRessourcesI18n.getString("button.join"));
                            } else {
                                boutonRejoindreVoir.setText(paquetRessourcesI18n.getString("button.view"));
                            }

                            if (estCreateur(reunion) && reunion.getStatut() == StatutReunion.PLANIFIEE) {
                                boutonModifierDetails.setText(paquetRessourcesI18n.getString("button.edit"));
                                boutonOuvrir.setText(paquetRessourcesI18n.getString("button.open.meeting"));
                                boutonOuvrir.setDisable(false);
                                boutonOuvrir.setVisible(true);
                            } else {
                                boutonModifierDetails.setText(paquetRessourcesI18n.getString("button.details"));
                            }

                            if (estCreateur(reunion) && reunion.getStatut() == StatutReunion.OUVERTE) {
                                boutonCloturer.setText(paquetRessourcesI18n.getString("button.close.meeting"));
                                boutonCloturer.setDisable(false);
                                boutonCloturer.setVisible(true);
                            }

                            if (reunion.getStatut() == StatutReunion.CLOTUREE || reunion.getStatut() == StatutReunion.ANNULEE) {
                                boutonRejoindreVoir.setDisable(true);
                                if(!estCreateur(reunion) || reunion.getStatut() != StatutReunion.PLANIFIEE) boutonModifierDetails.setDisable(true); // Peut toujours voir détails si créateur
                            }

                            setGraphic(conteneurActions);
                        }
                    }
                };
                return cell;
            }
        };
        colonneActionsReunion.setCellFactory(cellFactory);
    }

    @FXML
    private void actionCreerNouvelleReunion(ActionEvent evenement) {
        gestionnaireNavigation.afficherVuePlanificationReunion(null);
    }

    @FXML
    private void actionDeconnexion(ActionEvent evenement) {
        serviceCommunicationServeur.envoyerRequeteDeconnexion();
        // La session sera effacée et la navigation gérée par les listeners sur les propriétés de session/communication.
        // Pour forcer, on peut aussi appeler directement :
        // serviceSessionUtilisateur.effacerSession();
        // gestionnaireNavigation.afficherVueConnexion();
        journal.info("Utilisateur déconnecté.");
    }

    @FXML
    private void actionAdministrerUtilisateurs(ActionEvent evenement) {
        gestionnaireNavigation.afficherVueAdministrationUtilisateurs();
    }

    @FXML
    private void actionMonProfil(ActionEvent evenement) {
        //  gestionnaireNavigation.afficherVueMonProfil(); // A créer
        AlertesUtilisateur.afficherInformation("Mon Profil", "Fonctionnalité de gestion du profil à implémenter.");
    }

    @FXML
    private void actionChangerFiltreReunions(ActionEvent evenement) {
        actualiserListeReunions();
    }

    private void actualiserListeReunions() {
        String filtreSelectionne = comboBoxFiltreReunions.getSelectionModel().getSelectedItem();
        if (filtreSelectionne == null && comboBoxFiltreReunions.getItems().size() > 0) {
            comboBoxFiltreReunions.getSelectionModel().selectFirst(); // S'assurer qu'un filtre est tjrs sélectionné
            filtreSelectionne = comboBoxFiltreReunions.getSelectionModel().getSelectedItem();
        }


        if (filtreSelectionne != null) {
            if (filtreSelectionne.equals(paquetRessourcesI18n.getString(FILTRE_TOUTES_LES_REUNIONS_KEY))) {
                serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions();
                journal.info("Demande de toutes les réunions.");
            } else if (filtreSelectionne.equals(paquetRessourcesI18n.getString(FILTRE_MES_REUNIONS_CREEES_KEY))) {
                serviceCommunicationServeur.envoyerRequeteObtenirMesReunions(); // Supposons que cela retourne les créées
                journal.info("Demande des réunions créées par l'utilisateur.");
            } else if (filtreSelectionne.equals(paquetRessourcesI18n.getString(FILTRE_MES_PARTICIPATIONS_KEY))) {
                // serviceCommunicationServeur.envoyerRequeteObtenirMesParticipations(); // Nécessite une méthode dédiée dans le service
                journal.info("Filtre 'Mes participations' non implémenté côté service pour l'instant.");
                AlertesUtilisateur.afficherAvertissement("Filtre non disponible", "Le filtre pour 'Mes participations' n'est pas encore actif.");
                serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions(); // Fallback
            }
        } else {
            serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions(); // Par défaut
            journal.info("Aucun filtre sélectionné, demande de toutes les réunions.");
        }
    }
}