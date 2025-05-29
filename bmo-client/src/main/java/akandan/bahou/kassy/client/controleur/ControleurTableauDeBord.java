package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.modele.ModeleObservableReunion;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurTableauDeBord implements ControleurAvecInitialisation {

    @FXML private Label etiquetteBienvenueUtilisateur;
    @FXML private Button boutonDeconnexion;
    @FXML private Button boutonCreerReunion;
    @FXML private Button boutonAdministrationUtilisateurs;
    @FXML private TabPane tabPaneReunions;
    @FXML private Tab tabToutesLesReunions;
    @FXML private TableView<ModeleObservableReunion> tableViewToutesLesReunions;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneTitreToutes;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneDateDebutToutes;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneStatutToutes;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneOrganisateurToutes;
    @FXML private TableColumn<ModeleObservableReunion, Void> colonneActionsToutes;
    @FXML private Tab tabMesReunions;
    @FXML private TableView<ModeleObservableReunion> tableViewMesReunions;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneTitreMes;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneDateDebutMes;
    @FXML private TableColumn<ModeleObservableReunion, String> colonneStatutMes;
    @FXML private TableColumn<ModeleObservableReunion, Void> colonneActionsMes;
    @FXML private TextField champRechercheReunion;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private final ObservableList<ModeleObservableReunion> listeObservableToutesReunions = FXCollections.observableArrayList();
    private final ObservableList<ModeleObservableReunion> listeObservableMesReunions = FXCollections.observableArrayList();
    private FilteredList<ModeleObservableReunion> listeFiltreeToutesReunions;
    private FilteredList<ModeleObservableReunion> listeFiltreeMesReunions;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurTableauDeBord.class);
    private final DateTimeFormatter formateurDateHeureAffichage = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    public ControleurTableauDeBord() {
        this.listeFiltreeToutesReunions = new FilteredList<>(listeObservableToutesReunions, p -> true);
        this.listeFiltreeMesReunions = new FilteredList<>(listeObservableMesReunions, p -> true);
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        DonneesUtilisateurDTO utilisateur = serviceSessionUtilisateur.getUtilisateurConnecte();
        if (utilisateur != null) {
            etiquetteBienvenueUtilisateur.setText(paquetRessourcesI18n.getString("dashboard.welcome") + " " + utilisateur.getNomComplet());
            boutonAdministrationUtilisateurs.setVisible(utilisateur.getRole() == RoleUtilisateur.ADMINISTRATEUR);
            boutonAdministrationUtilisateurs.setManaged(utilisateur.getRole() == RoleUtilisateur.ADMINISTRATEUR);
        } else {
            etiquetteBienvenueUtilisateur.setText(paquetRessourcesI18n.getString("dashboard.welcome.guest"));
            boutonAdministrationUtilisateurs.setVisible(false);
            boutonAdministrationUtilisateurs.setManaged(false);
        }

        serviceCommunicationServeur.listeToutesReunionsProperty().addListener((obs, ancienneListe, nouvelleListe) ->
                Platform.runLater(() -> {
                    listeObservableToutesReunions.clear();
                    if (nouvelleListe != null) {
                        nouvelleListe.forEach(dto -> listeObservableToutesReunions.add(new ModeleObservableReunion(dto)));
                    }
                })
        );

        serviceCommunicationServeur.listeMesReunionsProperty().addListener((obs, ancienneListe, nouvelleListe) ->
                Platform.runLater(() -> {
                    listeObservableMesReunions.clear();
                    if (nouvelleListe != null) {
                        nouvelleListe.forEach(dto -> listeObservableMesReunions.add(new ModeleObservableReunion(dto)));
                    }
                })
        );

        serviceCommunicationServeur.detailsReunionActuelleProperty().addListener((obs, ancienneValeur, nouvelleValeur) -> {
            if (nouvelleValeur != null) { // Une réunion a été créée, modifiée, rejointe
                // Si la réunion rejointe est celle qui était détaillée, cela peut déclencher la navigation
                // si le GestionnaireNavigation écoute cette propriété pour naviguer vers la salle.
                // Pour l'instant, on rafraîchit juste les listes.
                if (nouvelleValeur.getStatutReunion() == StatutReunion.OUVERTE && gestionnaireNavigation != null &&
                        serviceSessionUtilisateur.getUtilisateurConnecte() != null &&
                        nouvelleValeur.getParticipantsDTO().stream().anyMatch(p -> p.getIdUtilisateur() == serviceSessionUtilisateur.getUtilisateurConnecte().getIdUtilisateur())) {
                    // Naviguer vers la salle si l'utilisateur actuel est un participant et la réunion est ouverte
                    // (Ceci est une supposition sur le flux, la navigation peut être gérée ailleurs)
                    // gestionnaireNavigation.afficherVueSalleReunion(nouvelleValeur);
                }
                rafraichirListesReunions();
            }
        });

        serviceCommunicationServeur.notificationReunionClotureeIdProperty().addListener((obs, ancienId, nouveauId) -> {
            if (nouveauId != null && nouveauId.intValue() != 0) {
                rafraichirListesReunions();
            }
        });

        rafraichirListesReunions();
    }

    @FXML
    private void initialize() {
        // Les FilteredList sont déjà initialisées dans le constructeur

        configurerTableau(tableViewToutesLesReunions, colonneTitreToutes, colonneDateDebutToutes, colonneStatutToutes, colonneOrganisateurToutes, colonneActionsToutes, listeFiltreeToutesReunions);
        configurerTableau(tableViewMesReunions, colonneTitreMes, colonneDateDebutMes, colonneStatutMes, null, colonneActionsMes, listeFiltreeMesReunions);


        champRechercheReunion.textProperty().addListener((observable, oldValue, newValue) -> {
            String filtre = newValue == null ? "" : newValue.toLowerCase().trim();
            listeFiltreeToutesReunions.setPredicate(reunion -> {
                if (filtre.isEmpty()) return true;
                return reunion.getTitre().toLowerCase().contains(filtre) ||
                        (reunion.getNomOrganisateur() != null && reunion.getNomOrganisateur().toLowerCase().contains(filtre));
            });
            listeFiltreeMesReunions.setPredicate(reunion -> {
                if (filtre.isEmpty()) return true;
                return reunion.getTitre().toLowerCase().contains(filtre);
            });
        });

        // Internationalisation
        if (paquetRessourcesI18n != null) {
            boutonDeconnexion.setText(paquetRessourcesI18n.getString("dashboard.button.logout"));
            boutonCreerReunion.setText(paquetRessourcesI18n.getString("dashboard.button.createMeeting"));
            boutonAdministrationUtilisateurs.setText(paquetRessourcesI18n.getString("dashboard.button.adminUsers"));
            tabToutesLesReunions.setText(paquetRessourcesI18n.getString("dashboard.tab.allMeetings"));
            tabMesReunions.setText(paquetRessourcesI18n.getString("dashboard.tab.myMeetings"));
            champRechercheReunion.setPromptText(paquetRessourcesI18n.getString("dashboard.textfield.searchMeeting"));
        }
    }

    private void configurerTableau(
            TableView<ModeleObservableReunion> tableau,
            TableColumn<ModeleObservableReunion, String> colTitre,
            TableColumn<ModeleObservableReunion, String> colDate,
            TableColumn<ModeleObservableReunion, String> colStatut,
            TableColumn<ModeleObservableReunion, String> colOrga, // Peut être null pour "Mes Réunions"
            TableColumn<ModeleObservableReunion, Void> colActions,
            FilteredList<ModeleObservableReunion> listeSource) {

        colTitre.setCellValueFactory(cellData -> cellData.getValue().titreProperty());
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDateHeureDebut();
            return new SimpleStringProperty(date != null ? date.format(formateurDateHeureAffichage) : "");
        });
        colStatut.setCellValueFactory(cellData -> {
            StatutReunion statut = cellData.getValue().getStatutReunion();
            return new SimpleStringProperty(statut != null ? paquetRessourcesI18n.getString("meeting.status." + statut.name().toLowerCase()) : "");
        });
        if (colOrga != null) {
            colOrga.setCellValueFactory(cellData -> cellData.getValue().nomOrganisateurProperty());
        }

        configurerColonneActions(colActions);
        tableau.setItems(listeSource);
        tableau.setPlaceholder(new Label(paquetRessourcesI18n.getString("dashboard.table.noMeetings")));
    }


    private void configurerColonneActions(TableColumn<ModeleObservableReunion, Void> colonneActions) {
        Callback<TableColumn<ModeleObservableReunion, Void>, TableCell<ModeleObservableReunion, Void>> cellFactory = param -> {
            final TableCell<ModeleObservableReunion, Void> cell = new TableCell<>() {
                private final Button btnRejoindre = new Button(paquetRessourcesI18n.getString("dashboard.action.join"));
                private final Button btnModifier = new Button(paquetRessourcesI18n.getString("dashboard.action.edit"));
                private final Button btnOuvrirCloturer = new Button(); // Texte dynamique
                private final HBox conteneurActions = new HBox(5);

                {
                    btnRejoindre.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        actionRejoindreReunion(reunion);
                    });
                    btnModifier.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        actionModifierReunion(reunion);
                    });
                    btnOuvrirCloturer.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        if (reunion.getStatutReunion() == StatutReunion.PLANIFIEE) {
                            actionOuvrirReunion(reunion);
                        } else if (reunion.getStatutReunion() == StatutReunion.OUVERTE) {
                            actionCloturerReunion(reunion);
                        }
                    });
                    conteneurActions.getChildren().addAll(btnRejoindre, btnModifier, btnOuvrirCloturer);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
                        boolean estOrganisateur = utilisateurActuel != null && reunion.getIdOrganisateur() == utilisateurActuel.getIdUtilisateur();

                        btnRejoindre.setVisible(reunion.getStatutReunion() == StatutReunion.OUVERTE);
                        btnRejoindre.setManaged(reunion.getStatutReunion() == StatutReunion.OUVERTE);

                        btnModifier.setVisible(estOrganisateur && (reunion.getStatutReunion() == StatutReunion.PLANIFIEE || reunion.getStatutReunion() == StatutReunion.OUVERTE));
                        btnModifier.setManaged(btnModifier.isVisible());

                        btnOuvrirCloturer.setVisible(estOrganisateur && (reunion.getStatutReunion() == StatutReunion.PLANIFIEE || reunion.getStatutReunion() == StatutReunion.OUVERTE));
                        btnOuvrirCloturer.setManaged(btnOuvrirCloturer.isVisible());

                        if (reunion.getStatutReunion() == StatutReunion.PLANIFIEE) {
                            btnOuvrirCloturer.setText(paquetRessourcesI18n.getString("dashboard.action.open"));
                        } else if (reunion.getStatutReunion() == StatutReunion.OUVERTE) {
                            btnOuvrirCloturer.setText(paquetRessourcesI18n.getString("dashboard.action.close"));
                        }
                        setGraphic(conteneurActions);
                    }
                }
            };
            return cell;
        };
        colonneActions.setCellFactory(cellFactory);
    }

    private void actionRejoindreReunion(ModeleObservableReunion reunion) {
        if (reunion != null && reunion.getStatutReunion() == StatutReunion.OUVERTE) {
            journal.info("Tentative de rejoindre la réunion : {}", reunion.getTitre());
            serviceCommunicationServeur.envoyerRequeteRejoindreReunion(reunion.getId());
            // La navigation vers la salle est attendue après confirmation du serveur
            // via la mise à jour de detailsReunionActuelleProperty et son listener,
            // qui pourrait appeler gestionnaireNavigation.afficherVueSalleReunion(detailsDTO)
        } else {
            AlertesUtilisateur.afficherAvertissement(paquetRessourcesI18n.getString("error.meeting.join.title"), paquetRessourcesI18n.getString("error.meeting.join.notOpen"));
        }
    }

    private void actionModifierReunion(ModeleObservableReunion reunion) {
        if (reunion != null) {
            journal.info("Modification de la réunion : {}", reunion.getTitre());
            gestionnaireNavigation.afficherVuePlanificationReunion(reunion.getDetailsDTOOriginal());
        }
    }

    private void actionOuvrirReunion(ModeleObservableReunion reunion) {
        if (reunion != null && reunion.getStatutReunion() == StatutReunion.PLANIFIEE) {
            journal.info("Ouverture de la réunion : {}", reunion.getTitre());
            serviceCommunicationServeur.envoyerRequeteOuvrirReunion(reunion.getId());
        }
    }

    private void actionCloturerReunion(ModeleObservableReunion reunion) {
        if (reunion != null && reunion.getStatutReunion() == StatutReunion.OUVERTE) {
            journal.info("Clôture de la réunion : {}", reunion.getTitre());
            serviceCommunicationServeur.envoyerRequeteCloturerReunion(reunion.getId());
        }
    }

    @FXML
    private void actionCreerNouvelleReunion(ActionEvent evenement) {
        gestionnaireNavigation.afficherVuePlanificationReunion(null);
    }

    @FXML
    private void actionDeconnexion(ActionEvent evenement) {
        serviceCommunicationServeur.envoyerRequeteDeconnexion();
        serviceSessionUtilisateur.viderSession(); // Vider la session localement immédiatement
        gestionnaireNavigation.afficherVueConnexion();
    }

    @FXML
    private void actionGestionUtilisateurs(ActionEvent evenement) {
        gestionnaireNavigation.afficherVueAdministrationUtilisateurs();
    }

    private void rafraichirListesReunions() {
        serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions();
        serviceCommunicationServeur.envoyerRequeteObtenirMesReunions();
    }
}