package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.modele.ModeleObservableUtilisateur;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DonneesUtilisateurDTO;
import akandan.bahou.kassy.commun.modele.RoleUtilisateur;
import akandan.bahou.kassy.commun.modele.StatutCompteUtilisateur;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurAdministrationUtilisateurs implements ControleurAvecInitialisation {

    @FXML private TableView<ModeleObservableUtilisateur> tableVueUtilisateurs;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneIdUtilisateur;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneNomComplet;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneIdentifiant;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneRole;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneStatutCompte;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDateCreation;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDerniereConnexion;
    @FXML private TableColumn<ModeleObservableUtilisateur, Void> colonneActionsUtilisateur;
    @FXML private Button boutonRetourTableauDeBordAdmin;
    @FXML private TextField champRechercheUtilisateurAdmin;
    @FXML private Button boutonCreerUtilisateurAdmin;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private final ObservableList<ModeleObservableUtilisateur> listeObservableUtilisateursAdmin = FXCollections.observableArrayList();
    private FilteredList<ModeleObservableUtilisateur> listeFiltreeUtilisateursAdmin;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurAdministrationUtilisateurs.class);
    private final DateTimeFormatter formateurDateHeureAffichage = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ControleurAdministrationUtilisateurs() {
        this.listeFiltreeUtilisateursAdmin = new FilteredList<>(listeObservableUtilisateursAdmin, p -> true);
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        if (serviceSessionUtilisateur.getUtilisateurConnecte() == null || serviceSessionUtilisateur.getUtilisateurConnecte().getRole() != RoleUtilisateur.ADMINISTRATEUR) {
            AlertesUtilisateur.afficherErreur(paquetRessourcesI18n.getString("error.access.denied.title"), paquetRessourcesI18n.getString("error.admin.access.required"));
            Platform.runLater(gestionnaireNavigation::afficherTableauDeBord);
            return;
        }

        serviceCommunicationServeur.listeGlobaleUtilisateursProperty().addListener((obs, ancienneListe, nouvelleListe) ->
                Platform.runLater(() -> {
                    listeObservableUtilisateursAdmin.clear();
                    if (nouvelleListe != null) {
                        nouvelleListe.forEach(dto -> listeObservableUtilisateursAdmin.add(new ModeleObservableUtilisateur(dto)));
                    }
                })
        );

        serviceCommunicationServeur.utilisateurMisAJourParAdminProperty().addListener((obs, ancienUtil, nouveauUtil) -> {
            if (nouveauUtil != null) { // Un utilisateur a été mis à jour
                // Rafraîchir toute la liste pour la simplicité, ou mettre à jour l'élément spécifique
                serviceCommunicationServeur.envoyerRequeteAdminObtenirUtilisateurs(champRechercheUtilisateurAdmin.getText());
            }
        });

        serviceCommunicationServeur.envoyerRequeteAdminObtenirUtilisateurs("");
    }

    @FXML
    private void initialize() {
        // listeFiltreeUtilisateursAdmin est déjà initialisée

        colonneIdUtilisateur.setCellValueFactory(cellData -> cellData.getValue().idUtilisateurProperty());
        colonneNomComplet.setCellValueFactory(cellData -> cellData.getValue().nomCompletProperty());
        colonneIdentifiant.setCellValueFactory(cellData -> cellData.getValue().identifiantProperty());
        colonneRole.setCellValueFactory(cellData -> {
            RoleUtilisateur role = cellData.getValue().getRole();
            return new SimpleStringProperty(role != null ? paquetRessourcesI18n.getString("role." + role.name().toLowerCase()) : "");
        });
        colonneStatutCompte.setCellValueFactory(cellData -> {
            StatutCompteUtilisateur statut = cellData.getValue().getStatutCompte();
            return new SimpleStringProperty(statut != null ? paquetRessourcesI18n.getString("user.status." + statut.name().toLowerCase()) : "");
        });
        colonneDateCreation.setCellValueFactory(cellData -> {
            // ModeleObservableUtilisateur stocke déjà la date formatée
            return cellData.getValue().dateCreationCompteAffichageProperty();
        });
        colonneDerniereConnexion.setCellValueFactory(cellData -> {
            return cellData.getValue().dateDerniereConnexionAffichageProperty();
        });

        configurerColonneActionsAdmin();
        tableVueUtilisateurs.setItems(listeFiltreeUtilisateursAdmin);
        tableVueUtilisateurs.setPlaceholder(new Label(paquetRessourcesI18n.getString("admin.users.table.noUsers")));


        champRechercheUtilisateurAdmin.textProperty().addListener((observable, oldValue, newValue) -> {
            String filtre = newValue == null ? "" : newValue.toLowerCase().trim();
            listeFiltreeUtilisateursAdmin.setPredicate(utilisateur -> {
                if (filtre.isEmpty()) return true;
                return utilisateur.getNomComplet().toLowerCase().contains(filtre) ||
                        utilisateur.getIdentifiant().toLowerCase().contains(filtre);
            });
        });

        if (paquetRessourcesI18n != null) {
            boutonRetourTableauDeBordAdmin.setText(paquetRessourcesI18n.getString("common.button.back"));
            boutonCreerUtilisateurAdmin.setText(paquetRessourcesI18n.getString("admin.users.button.createUser"));
            champRechercheUtilisateurAdmin.setPromptText(paquetRessourcesI18n.getString("admin.users.textfield.searchUser"));
        }
        boutonCreerUtilisateurAdmin.setDisable(true); // Fonctionnalité non prioritaire
        boutonCreerUtilisateurAdmin.setManaged(false);
        boutonCreerUtilisateurAdmin.setVisible(false);
    }

    private void configurerColonneActionsAdmin() {
        Callback<TableColumn<ModeleObservableUtilisateur, Void>, TableCell<ModeleObservableUtilisateur, Void>> cellFactory = param -> {
            final TableCell<ModeleObservableUtilisateur, Void> cell = new TableCell<>() {
                private final Button btnModifierRole = new Button(paquetRessourcesI18n.getString("admin.users.action.editRole"));
                private final Button btnChangerStatut = new Button(); // Texte dynamique
                private final HBox conteneurActions = new HBox(5, btnModifierRole, btnChangerStatut);

                {
                    btnModifierRole.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        ouvrirDialogueModificationRole(utilisateur);
                    });
                    btnChangerStatut.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        changerStatutCompteUtilisateur(utilisateur);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        DonneesUtilisateurDTO adminConnecte = serviceSessionUtilisateur.getUtilisateurConnecte();

                        boolean estSoiMeme = adminConnecte != null && utilisateur.getIdUtilisateur().equals(String.valueOf(adminConnecte.getIdUtilisateur()));

                        btnModifierRole.setDisable(estSoiMeme);
                        btnChangerStatut.setDisable(estSoiMeme);

                        if (utilisateur.getStatutCompte() == StatutCompteUtilisateur.ACTIF) {
                            btnChangerStatut.setText(paquetRessourcesI18n.getString("admin.users.action.deactivate"));
                        } else {
                            btnChangerStatut.setText(paquetRessourcesI18n.getString("admin.users.action.activate"));
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
    private void actionRetourTableauDeBordAdmin(ActionEvent evenement) {
        gestionnaireNavigation.afficherTableauDeBord();
    }

    private void ouvrirDialogueModificationRole(ModeleObservableUtilisateur utilisateur) {
        Dialog<RoleUtilisateur> dialogue = new Dialog<>();
        dialogue.setTitle(paquetRessourcesI18n.getString("admin.users.dialog.editRole.title"));
        dialogue.setHeaderText(paquetRessourcesI18n.getString("admin.users.dialog.editRole.header") + " " + utilisateur.getNomComplet());

        ButtonType boutonSauvegarderType = new ButtonType(paquetRessourcesI18n.getString("common.button.save"), ButtonData.OK_DONE);
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

        grille.add(new Label(paquetRessourcesI18n.getString("admin.users.label.role") + ":"), 0, 0);
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
                DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
                dto.setIdUtilisateur(Long.parseLong(utilisateur.getIdUtilisateur()));
                dto.setRole(nouveauRole);
                dto.setStatutCompte(utilisateur.getStatutCompte()); // Conserver le statut actuel
                // Les autres champs ne sont pas nécessaires pour cette requête spécifique côté serveur
                serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dto);
            }
        });
    }

    private void changerStatutCompteUtilisateur(ModeleObservableUtilisateur utilisateur) {
        StatutCompteUtilisateur statutActuel = utilisateur.getStatutCompte();
        StatutCompteUtilisateur nouveauStatut = (statutActuel == StatutCompteUtilisateur.ACTIF) ? StatutCompteUtilisateur.INACTIF : StatutCompteUtilisateur.ACTIF;

        String cleConfirmation = nouveauStatut == StatutCompteUtilisateur.ACTIF ? "admin.users.confirm.activate" : "admin.users.confirm.deactivate";
        String messageConfirmation = String.format(paquetRessourcesI18n.getString(cleConfirmation), utilisateur.getNomComplet());

        if (AlertesUtilisateur.afficherConfirmation(paquetRessourcesI18n.getString("admin.users.confirm.statusChange.title"), messageConfirmation)) {
            DonneesUtilisateurDTO dto = new DonneesUtilisateurDTO();
            dto.setIdUtilisateur(Long.parseLong(utilisateur.getIdUtilisateur()));
            dto.setRole(utilisateur.getRole()); // Conserver le rôle actuel
            dto.setStatutCompte(nouveauStatut);
            serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dto);
        }
    }

    @FXML
    private void actionCreerNouvelUtilisateurAdmin(ActionEvent evenement) {
        // Non implémenté, bouton désactivé
        AlertesUtilisateur.afficherInformation("Fonctionnalité non disponible", "La création d'utilisateurs depuis ce panneau n'est pas encore implémentée.");
    }
}