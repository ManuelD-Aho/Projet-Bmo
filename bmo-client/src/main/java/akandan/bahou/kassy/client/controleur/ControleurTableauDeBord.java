package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.modele.ModeleObservableReunion;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList; // Ajout de l'import
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
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
    private final ObservableList<ModeleObservableReunion> listePrincipaleReunions = FXCollections.observableArrayList();
    private SortedList<ModeleObservableReunion> listeTrieeReunions; // Pour le TableView

    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurTableauDeBord.class);
    private final DateTimeFormatter formateurDateHeureTableau = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String FILTRE_TOUTES_LES_REUNIONS_KEY = "filter.meetings.all";
    private static final String FILTRE_MES_REUNIONS_CREEES_KEY = "filter.meetings.mycreated";

    public ControleurTableauDeBord() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        DonneesUtilisateurDTO utilisateur = serviceSessionUtilisateur.getUtilisateurConnecte();
        if (utilisateur == null) {
            Platform.runLater(gestionnaireNavigation::afficherVueConnexion);
            return;
        }
        etiquetteNomUtilisateur.setText(utilisateur.getNomComplet());
        try {
            etiquetteRoleUtilisateur.setText(paquetRessourcesI18n.getString("role." + utilisateur.getRole().name().toLowerCase()));
        } catch (Exception e) {
            etiquetteRoleUtilisateur.setText(utilisateur.getRole().name());
        }

        configurerVisibiliteBoutons();
        configurerComboBoxFiltres();
        configurerListenersService();

        listeTrieeReunions = new SortedList<>(listePrincipaleReunions);
        listeTrieeReunions.comparatorProperty().bind(tableVueReunions.comparatorProperty());
        tableVueReunions.setItems(listeTrieeReunions);

        actualiserListeReunions(); // Appel initial pour charger les données
    }

    private void configurerVisibiliteBoutons() {
        boolean estAdmin = serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR);
        boolean peutOrganiser = serviceSessionUtilisateur.aRole(RoleUtilisateur.ORGANISATEUR) || estAdmin;
        boutonAdministrationUtilisateurs.setVisible(estAdmin);
        boutonAdministrationUtilisateurs.setManaged(estAdmin);
        boutonCreerReunion.setVisible(peutOrganiser);
        boutonCreerReunion.setManaged(peutOrganiser);
    }

    private void configurerComboBoxFiltres() {
        comboBoxFiltreReunions.getItems().setAll(
                paquetRessourcesI18n.getString(FILTRE_TOUTES_LES_REUNIONS_KEY),
                paquetRessourcesI18n.getString(FILTRE_MES_REUNIONS_CREEES_KEY)
        );
        comboBoxFiltreReunions.getSelectionModel().selectFirst();
        comboBoxFiltreReunions.setOnAction(this::actionChangerFiltreReunions);
    }

    private void configurerListenersService() {
        serviceCommunicationServeur.listeToutesReunionsProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            if (estFiltreActif(FILTRE_TOUTES_LES_REUNIONS_KEY)) mettreAJourTableauReunions(nouvelleListe);
        });
        serviceCommunicationServeur.listeMesReunionsProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            if (estFiltreActif(FILTRE_MES_REUNIONS_CREEES_KEY)) mettreAJourTableauReunions(nouvelleListe);
        });
        serviceCommunicationServeur.detailsReunionActuelleProperty().addListener((obs, ancienneReunion, reunionMaj) -> mettreAJourOuAjouterReunionTableau(reunionMaj));
        serviceCommunicationServeur.notificationReunionClotureeIdProperty().addListener((obs, anc, idClotureObj) -> {
            if (idClotureObj != null && idClotureObj.intValue() != 0) {
                traiterNotificationReunionCloturee(idClotureObj.longValue());
            }
        });
    }

    private boolean estFiltreActif(String cleFiltreAttendue) {
        String selection = comboBoxFiltreReunions.getSelectionModel().getSelectedItem();
        return selection != null && selection.equals(paquetRessourcesI18n.getString(cleFiltreAttendue));
    }

    private void mettreAJourOuAjouterReunionTableau(DetailsReunionDTO reunionMaj) {
        if (reunionMaj == null) return;
        Platform.runLater(() -> {
            Optional<ModeleObservableReunion> optReunionExistante = listePrincipaleReunions.stream()
                    .filter(r -> r.getId() == reunionMaj.getIdReunion())
                    .findFirst();
            if (optReunionExistante.isPresent()) {
                optReunionExistante.get().mettreAJourAvecDTO(reunionMaj);
            } else {
                listePrincipaleReunions.add(new ModeleObservableReunion(reunionMaj));
            }
            tableVueReunions.refresh(); // Crucial pour rafraîchir l'affichage après modif d'un item
        });
    }

    private void traiterNotificationReunionCloturee(long idReunionCloturee) {
        Platform.runLater(() -> {
            Optional<ModeleObservableReunion> optReunion = listePrincipaleReunions.stream()
                    .filter(r -> r.getId() == idReunionCloturee)
                    .findFirst();
            optReunion.ifPresent(reunion -> {
                reunion.setStatutReunion(StatutReunion.CLOTUREE); // Mettre à jour le modèle observable
                tableVueReunions.refresh();
                AlertesUtilisateur.afficherInformation(
                        paquetRessourcesI18n.getString("meeting.status.changed.title"),
                        String.format(paquetRessourcesI18n.getString("meeting.status.now.closed"), reunion.getTitre())
                );
            });
        });
    }

    private void mettreAJourTableauReunions(List<DetailsReunionDTO> nouvelleListeDTO) {
        Platform.runLater(() -> {
            listePrincipaleReunions.clear();
            if (nouvelleListeDTO != null) {
                nouvelleListeDTO.forEach(dto -> listePrincipaleReunions.add(new ModeleObservableReunion(dto)));
            }
            // Le SortedList lié au TableView se mettra à jour automatiquement.
            // tableVueReunions.setItems(listeTrieeReunions); // Pas besoin de refaire le set si listePrincipale est la source de SortedList
            tableVueReunions.refresh();
        });
    }

    @FXML
    private void initialize() {
        colonneTitreReunion.setCellValueFactory(cellData -> cellData.getValue().titreProperty());
        colonneDescriptionReunion.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        colonneDateDebutReunion.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDateHeureDebut();
            return new SimpleStringProperty(date != null ? date.format(formateurDateHeureTableau) : "");
        });
        colonneStatutReunion.setCellValueFactory(cellData -> {
            StatutReunion statut = cellData.getValue().getStatutReunion();
            String cleI18n = "statut.reunion." + (statut != null ? statut.name().toLowerCase() : "inconnu");
            try {
                return new SimpleStringProperty(paquetRessourcesI18n.getString(cleI18n));
            } catch (Exception e) {
                return new SimpleStringProperty(statut != null ? statut.name() : "");
            }
        });

        configurerColonneActions();
        // Le SortedList est déjà configuré dans initialiserDonneesEtServices pour utiliser tableVueReunions.setItems
        tableVueReunions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableVueReunions.setPlaceholder(new Label(paquetRessourcesI18n.getString("table.reunions.empty")));
    }

    private void configurerColonneActions() {
        Callback<TableColumn<ModeleObservableReunion, Void>, TableCell<ModeleObservableReunion, Void>> cellFactory = param -> {
            final TableCell<ModeleObservableReunion, Void> cell = new TableCell<>() {
                private final Button boutonRejoindreVoir = new Button();
                private final Button boutonModifier = new Button(paquetRessourcesI18n.getString("button.edit"));
                private final Button boutonOuvrir = new Button(paquetRessourcesI18n.getString("button.open.meeting"));
                private final Button boutonCloturer = new Button(paquetRessourcesI18n.getString("button.close.meeting"));
                private final HBox conteneurActions = new HBox(5);

                {
                    conteneurActions.getChildren().addAll(boutonRejoindreVoir, boutonModifier, boutonOuvrir, boutonCloturer);
                    boutonRejoindreVoir.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        if (reunion.getStatutReunion() == StatutReunion.OUVERTE) {
                            demanderMotDePasseSiNecessaireEtRejoindre(reunion);
                        } else {
                            AlertesUtilisateur.afficherInformation(
                                    String.format(paquetRessourcesI18n.getString("meeting.details.title.specific"), reunion.getTitre()),
                                    String.format(paquetRessourcesI18n.getString("meeting.details.content.format"),
                                            reunion.getDescription(),
                                            reunion.getDateHeureDebut().format(formateurDateHeureTableau),
                                            reunion.getNomOrganisateur(),
                                            paquetRessourcesI18n.getString("statut.reunion." + reunion.getStatutReunion().name().toLowerCase())
                                    )
                            );
                        }
                    });
                    boutonModifier.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        // S'assurer que getDetailsDTOOriginal() retourne un DTO avec tous les champs nécessaires
                        gestionnaireNavigation.afficherVuePlanificationReunion(reunion.getDetailsDTOOriginal());
                    });
                    boutonOuvrir.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        serviceCommunicationServeur.envoyerRequeteOuvrirReunion(reunion.getId());
                    });
                    boutonCloturer.setOnAction(event -> {
                        ModeleObservableReunion reunion = getTableView().getItems().get(getIndex());
                        serviceCommunicationServeur.envoyerRequeteCloturerReunion(reunion.getId());
                    });
                }

                private void demanderMotDePasseSiNecessaireEtRejoindre(ModeleObservableReunion reunion) {
                    DetailsReunionDTO dtoOriginal = reunion.getDetailsDTOOriginal();
                    if (dtoOriginal.getTypeReunion() == TypeReunion.PRIVEE && dtoOriginal.getMotDePasseOptionnelValeur() != null && !dtoOriginal.getMotDePasseOptionnelValeur().isEmpty()) {
                        TextInputDialog dialogueMotDePasse = new TextInputDialog();
                        dialogueMotDePasse.setTitle(paquetRessourcesI18n.getString("meeting.password.dialog.title"));
                        dialogueMotDePasse.setHeaderText(String.format(paquetRessourcesI18n.getString("meeting.password.dialog.header"), reunion.getTitre()));
                        dialogueMotDePasse.setContentText(paquetRessourcesI18n.getString("meeting.password.dialog.content"));

                        Optional<String> resultat = dialogueMotDePasse.showAndWait();
                        resultat.ifPresent(mdp -> {
                            serviceCommunicationServeur.envoyerRequeteRejoindreReunion(reunion.getId(), mdp);
                        });
                    } else { // Soit non privée, soit privée sans mot de passe (si autorisé par la logique serveur)
                        serviceCommunicationServeur.envoyerRequeteRejoindreReunion(reunion.getId(), null);
                    }
                }

                private boolean estOrganisateur(ModeleObservableReunion reunion) {
                    DonneesUtilisateurDTO utilisateurActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
                    return utilisateurActuel != null && reunion.getIdOrganisateur() == utilisateurActuel.getIdUtilisateur();
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        ModeleObservableReunion reunion = getTableRow().getItem();
                        boolean peutGerer = estOrganisateur(reunion) || serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR);

                        boutonRejoindreVoir.setText(reunion.getStatutReunion() == StatutReunion.OUVERTE ? paquetRessourcesI18n.getString("button.join") : paquetRessourcesI18n.getString("button.view.details"));
                        boutonRejoindreVoir.setDisable(reunion.getStatutReunion() == StatutReunion.CLOTUREE || reunion.getStatutReunion() == StatutReunion.ANNULEE);

                        boutonModifier.setVisible(peutGerer && reunion.getStatutReunion() == StatutReunion.PLANIFIEE);
                        boutonModifier.setManaged(boutonModifier.isVisible());

                        boutonOuvrir.setVisible(peutGerer && reunion.getStatutReunion() == StatutReunion.PLANIFIEE);
                        boutonOuvrir.setManaged(boutonOuvrir.isVisible());

                        boutonCloturer.setVisible(peutGerer && reunion.getStatutReunion() == StatutReunion.OUVERTE);
                        boutonCloturer.setManaged(boutonCloturer.isVisible());

                        setGraphic(conteneurActions);
                    }
                }
            };
            return cell;
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
        journal.info("Demande de déconnexion envoyée.");
        // La navigation vers la vue de connexion sera gérée par le listener sur estUtilisateurConnecteProperty
        // dans ServiceSessionUtilisateur, qui est écouté par AppPrincipale ou ControleurFenetrePrincipale
        // pour effectuer la redirection. Si FenetrePrincipale est toujours là, elle reviendra à la vue de connexion.
    }

    @FXML
    private void actionAdministrerUtilisateurs(ActionEvent evenement) {
        gestionnaireNavigation.afficherVueAdministrationUtilisateurs();
    }

    @FXML
    private void actionMonProfil(ActionEvent evenement) {
        AlertesUtilisateur.afficherInformation(
                paquetRessourcesI18n.getString("profile.my.title"),
                paquetRessourcesI18n.getString("feature.tobeimplemented")
        );
    }

    @FXML
    private void actionChangerFiltreReunions(ActionEvent evenement) {
        actualiserListeReunions();
    }

    private void actualiserListeReunions() {
        String filtreSelectionne = comboBoxFiltreReunions.getSelectionModel().getSelectedItem();

        if (filtreSelectionne == null && !comboBoxFiltreReunions.getItems().isEmpty()) {
            comboBoxFiltreReunions.getSelectionModel().selectFirst();
            filtreSelectionne = comboBoxFiltreReunions.getSelectionModel().getSelectedItem();
        }

        if (filtreSelectionne != null) {
            if (filtreSelectionne.equals(paquetRessourcesI18n.getString(FILTRE_TOUTES_LES_REUNIONS_KEY))) {
                serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions();
            } else if (filtreSelectionne.equals(paquetRessourcesI18n.getString(FILTRE_MES_REUNIONS_CREEES_KEY))) {
                serviceCommunicationServeur.envoyerRequeteObtenirMesReunions();
            }
        } else {
            serviceCommunicationServeur.envoyerRequeteObtenirToutesLesReunions();
        }
    }
}