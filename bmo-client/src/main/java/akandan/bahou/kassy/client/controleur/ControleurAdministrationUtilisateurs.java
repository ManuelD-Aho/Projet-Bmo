package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.modele.ModeleObservableUtilisateur;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;

public class ControleurAdministrationUtilisateurs implements ControleurAvecInitialisation {

    @FXML private TableView<ModeleObservableUtilisateur> tableVueUtilisateurs;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneIdUtilisateur;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneNomComplet;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneIdentifiant; // Corrigé
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneRole;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneStatutCompte;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDateCreation;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDerniereConnexion;
    @FXML private TableColumn<ModeleObservableUtilisateur, Void> colonneActionsUtilisateur;

    @FXML private Button boutonRetourTableauDeBord;
    @FXML private TextField champRechercheUtilisateur;
    // @FXML private Button boutonRechercher; // La recherche sera dynamique
    @FXML private Button boutonCreerUtilisateur; // Laissé pour l'instant, même si non implémenté

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private final ObservableList<ModeleObservableUtilisateur> listePrincipaleUtilisateurs = FXCollections.observableArrayList();
    private FilteredList<ModeleObservableUtilisateur> listeFiltreeUtilisateurs;

    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurAdministrationUtilisateurs.class);
    private final DateTimeFormatter formateurDateHeureTableau = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ControleurAdministrationUtilisateurs() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        if (!serviceSessionUtilisateur.aRole(RoleUtilisateur.ADMINISTRATEUR)) {
            AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.access.denied.title"), paquetRessourcesI18n.getString("error.admin.access.required"));
            Platform.runLater(gestionnaireNavigation::afficherTableauDeBord);
            return;
        }

        configurerListenersService();
        serviceCommunicationServeur.envoyerRequeteAdminObtenirUtilisateurs(null); // Charger tous les utilisateurs initialement
    }

    private void configurerListenersService() {
        serviceCommunicationServeur.listeGlobaleUtilisateursProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            Platform.runLater(() -> {
                listePrincipaleUtilisateurs.clear();
                if (nouvelleListe != null) {
                    nouvelleListe.forEach(dto -> listePrincipaleUtilisateurs.add(new ModeleObservableUtilisateur(dto)));
                }
            });
        });

        serviceCommunicationServeur.utilisateurMisAJourParAdminProperty().addListener((obs, ancienUtilisateur, utilisateurModifie) -> {
            if (utilisateurModifie != null) {
                Platform.runLater(() -> {
                    Optional<ModeleObservableUtilisateur> optUtilisateur = listePrincipaleUtilisateurs.stream()
                            .filter(u -> u.getIdUtilisateur().equals(String.valueOf(utilisateurModifie.getIdUtilisateur())))
                            .findFirst();
                    if (optUtilisateur.isPresent()) {
                        optUtilisateur.get().mettreAJourAvecDTO(utilisateurModifie);
                    } else { // Si admin crée un user et le serveur renvoie le DTO, on l'ajoute
                        listePrincipaleUtilisateurs.add(new ModeleObservableUtilisateur(utilisateurModifie));
                    }
                    tableVueUtilisateurs.refresh();
                });
            }
        });
    }

    @FXML
    private void initialize() {
        colonneIdUtilisateur.setCellValueFactory(cellData -> cellData.getValue().idUtilisateurProperty());
        colonneNomComplet.setCellValueFactory(cellData -> cellData.getValue().nomCompletProperty());
        colonneIdentifiant.setCellValueFactory(cellData -> cellData.getValue().identifiantProperty());

        colonneRole.setCellValueFactory(cellData -> {
            RoleUtilisateur role = cellData.getValue().getRole();
            String cleI18n = "role." + (role != null ? role.name().toLowerCase() : "inconnu");
            return new SimpleStringProperty(paquetRessourcesI18n.containsKey(cleI18n) ? paquetRessourcesI18n.getString(cleI18n) : (role != null ? role.name() : ""));
        });
        colonneStatutCompte.setCellValueFactory(cellData -> {
            StatutCompteUtilisateur statut = cellData.getValue().getStatutCompte();
            String cleI18n = "statut.compte." + (statut != null ? statut.name().toLowerCase() : "inconnu");
            return new SimpleStringProperty(paquetRessourcesI18n.containsKey(cleI18n) ? paquetRessourcesI18n.getString(cleI18n) : (statut != null ? statut.name() : ""));
        });

        colonneDateCreation.setCellValueFactory(cellData -> cellData.getValue().dateCreationCompteAffichageProperty());
        colonneDerniereConnexion.setCellValueFactory(cellData -> cellData.getValue().dateDerniereConnexionAffichageProperty());

        configurerColonneActionsAdmin();

        listeFiltreeUtilisateurs = new FilteredList<>(listePrincipaleUtilisateurs, p -> true);
        SortedList<ModeleObservableUtilisateur> sortedData = new SortedList<>(listeFiltreeUtilisateurs);
        sortedData.comparatorProperty().bind(tableVueUtilisateurs.comparatorProperty());
        tableVueUtilisateurs.setItems(sortedData);
        tableVueUtilisateurs.setPlaceholder(new Label(paquetRessourcesI18n.getString("table.users.empty")));


        champRechercheUtilisateur.textProperty().addListener((observable, oldValue, newValue) -> {
            listeFiltreeUtilisateurs.setPredicate(utilisateur -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String texteFiltreMinuscules = newValue.toLowerCase();
                if (utilisateur.getNomComplet().toLowerCase().contains(texteFiltreMinuscules)) return true;
                if (utilisateur.getIdentifiant().toLowerCase().contains(texteFiltreMinuscules)) return true;
                // Ajouter d'autres champs de recherche si nécessaire
                return false;
            });
        });

        // Laisser le bouton visible mais désactivé si la fonctionnalité n'est pas prête.
        boutonCreerUtilisateur.setDisable(true);
        // boutonCreerUtilisateur.setVisible(false); // Décommenter pour cacher complètement
    }

    private void configurerColonneActionsAdmin() {
        Callback<TableColumn<ModeleObservableUtilisateur, Void>, TableCell<ModeleObservableUtilisateur, Void>> cellFactory = param -> {
            final TableCell<ModeleObservableUtilisateur, Void> cell = new TableCell<>() {
                private final Button boutonModifierRole = new Button(); // Texte dynamique (Modifier Rôle)
                private final Button boutonChangerStatut = new Button(); // Texte dynamique
                // private final Button boutonSupprimer = new Button(); // Texte: Supprimer
                private final HBox conteneurActions = new HBox(5);

                {
                    conteneurActions.getChildren().addAll(boutonModifierRole, boutonChangerStatut);
                    boutonModifierRole.setText(paquetRessourcesI18n.getString("button.edit.role"));
                    // boutonSupprimer.setText(paquetRessourcesI18n.getString("button.delete"));
                    // boutonSupprimer.setStyle("-fx-text-fill: red;"); // Pour le distinguer

                    boutonModifierRole.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        if (utilisateur != null) ouvrirDialogueModificationRoleUtilisateur(utilisateur);
                    });
                    boutonChangerStatut.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        if (utilisateur != null) changerStatutCompteUtilisateurInteractivement(utilisateur);
                    });
                    /* boutonSupprimer.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        if (utilisateur != null) actionSupprimerUtilisateur(utilisateur);
                    }); */
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        ModeleObservableUtilisateur utilisateur = getTableRow().getItem();
                        DonneesUtilisateurDTO adminActuel = serviceSessionUtilisateur.getUtilisateurConnecte();
                        boolean estSoiMeme = adminActuel != null && utilisateur.getIdUtilisateur().equals(String.valueOf(adminActuel.getIdUtilisateur()));

                        boutonModifierRole.setDisable(estSoiMeme);
                        boutonChangerStatut.setDisable(estSoiMeme);
                        // boutonSupprimer.setDisable(estSoiMeme);

                        if (utilisateur.getStatutCompte() == StatutCompteUtilisateur.ACTIF) {
                            boutonChangerStatut.setText(paquetRessourcesI18n.getString("button.deactivate"));
                        } else {
                            boutonChangerStatut.setText(paquetRessourcesI18n.getString("button.activate"));
                        }
                        setGraphic(conteneurActions);
                    }
                }
            };
            return cell;
        };
        colonneActionsUtilisateur.setCellFactory(cellFactory);
    }

    @FXML
    private void actionRetourTableauDeBord(ActionEvent evenement) {
        gestionnaireNavigation.afficherTableauDeBord();
    }

    @FXML
    private void actionCreerNouvelUtilisateur(ActionEvent evenement) {
        journal.info("Fonctionnalité de création d'utilisateur par admin non implémentée via ce bouton.");
        AlertesUtilisateur.afficherInformation(paquetRessourcesI18n.getString("feature.dev.title"), paquetRessourcesI18n.getString("feature.admin.createUser.todo"));
    }

    private void ouvrirDialogueModificationRoleUtilisateur(ModeleObservableUtilisateur utilisateur) {
        Dialog<RoleUtilisateur> dialogue = new Dialog<>();
        dialogue.setTitle(paquetRessourcesI18n.getString("admin.edit.user.role.dialog.title"));
        dialogue.setHeaderText(String.format(paquetRessourcesI18n.getString("admin.edit.user.role.dialog.header"), utilisateur.getNomComplet()));

        ButtonType boutonSauvegarderType = new ButtonType(paquetRessourcesI18n.getString("button.save"), ButtonData.OK_DONE);
        dialogue.getDialogPane().getButtonTypes().addAll(boutonSauvegarderType, ButtonType.CANCEL);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<RoleUtilisateur> comboBoxRole = new ComboBox<>(FXCollections.observableArrayList(RoleUtilisateur.values()));
        comboBoxRole.setValue(utilisateur.getRole());
        comboBoxRole.setConverter(new StringConverter<>() {
            @Override public String toString(RoleUtilisateur role) { return role == null ? "" : paquetRessourcesI18n.getString("role." + role.name().toLowerCase()); }
            @Override public RoleUtilisateur fromString(String string) { return null; }
        });

        grille.add(new Label(paquetRessourcesI18n.getString("label.role") + ":"), 0, 0);
        grille.add(comboBoxRole, 1, 0);

        dialogue.getDialogPane().setContent(grille);
        Platform.runLater(comboBoxRole::requestFocus);

        dialogue.setResultConverter(typeBouton -> {
            if (typeBouton == boutonSauvegarderType) {
                return comboBoxRole.getValue();
            }
            return null;
        });

        Optional<RoleUtilisateur> resultat = dialogue.showAndWait();
        resultat.ifPresent(nouveauRole -> {
            if (nouveauRole != utilisateur.getRole()) {
                DonneesUtilisateurDTO dtoPourModification = utilisateur.getDonneesDTOOriginal(); // Crée une copie ou utilise l'original pour modification
                dtoPourModification.setRole(nouveauRole); // Mettre à jour seulement le rôle
                // Le statut et autres infos restent inchangés pour cette opération spécifique
                serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dtoPourModification);
                journal.info("Demande de modification du rôle pour l'utilisateur ID {} à {}", utilisateur.getIdUtilisateur(), nouveauRole);
            } else {
                journal.info("Aucun changement de rôle détecté pour l'utilisateur ID {}.", utilisateur.getIdUtilisateur());
            }
        });
    }

    private void changerStatutCompteUtilisateurInteractivement(ModeleObservableUtilisateur utilisateur) {
        StatutCompteUtilisateur statutActuel = utilisateur.getStatutCompte();
        StatutCompteUtilisateur nouveauStatutPropose = (statutActuel == StatutCompteUtilisateur.ACTIF) ? StatutCompteUtilisateur.INACTIF : StatutCompteUtilisateur.ACTIF;

        String messageI18nKey = "admin.confirm.status.change." + nouveauStatutPropose.name().toLowerCase();
        String messageConfirmation = String.format(paquetRessourcesI18n.getString(messageI18nKey), utilisateur.getNomComplet());

        if (AlertesUtilisateur.afficherConfirmation(paquetRessourcesI18n.getString("admin.confirm.status.change.title"), messageConfirmation)) {
            DonneesUtilisateurDTO dtoPourModification = utilisateur.getDonneesDTOOriginal();
            dtoPourModification.setStatutCompte(nouveauStatutPropose); // Mettre à jour seulement le statut

            serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dtoPourModification);
            journal.info("Demande de changement de statut pour l'utilisateur ID {} à {}", utilisateur.getIdUtilisateur(), nouveauStatutPropose);
        }
    }

    /* private void actionSupprimerUtilisateur(ModeleObservableUtilisateur utilisateur) {
        String messageConfirmation = String.format(paquetRessourcesI18n.getString("admin.confirm.delete.user"), utilisateur.getNomComplet(), utilisateur.getIdentifiant());
        if (AlertesUtilisateur.afficherConfirmation(paquetRessourcesI18n.getString("admin.confirm.delete.title"), messageConfirmation)) {
            // serviceCommunicationServeur.envoyerRequeteAdminSupprimerUtilisateur(Long.parseLong(utilisateur.getIdUtilisateur()));
            journal.info("Fonctionnalité de suppression admin non entièrement implémentée dans ServiceCommunicationServeur.");
        }
    } */
}