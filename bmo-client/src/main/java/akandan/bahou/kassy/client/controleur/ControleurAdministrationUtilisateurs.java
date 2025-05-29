package akandan.bahou.kassy.client.controleur;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.Arrays;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.modele.ModeleObservableUtilisateur;
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
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneIdentifiantConnexion;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneRole;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneStatutCompte;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDateCreation;
    @FXML private TableColumn<ModeleObservableUtilisateur, String> colonneDerniereConnexion;
    @FXML private TableColumn<ModeleObservableUtilisateur, Void> colonneActionsUtilisateur;
    @FXML private Button boutonRetourTableauDeBord;
    @FXML private TextField champRechercheUtilisateur;
    @FXML private Button boutonRechercher;
    @FXML private Button boutonCreerUtilisateur;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private final ObservableList<ModeleObservableUtilisateur> listeObservableUtilisateurs = FXCollections.observableArrayList();
    private FilteredList<ModeleObservableUtilisateur> listeFiltreeUtilisateurs;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurAdministrationUtilisateurs.class);
    private final DateTimeFormatter formateurDateHeure = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ControleurAdministrationUtilisateurs() {
        this.listeFiltreeUtilisateurs = new FilteredList<>(listeObservableUtilisateurs, p -> true);
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

        serviceComm.listeUtilisateursMiseAJourProperty().addListener((obs, ancienneListe, nouvelleListe) -> {
            Platform.runLater(() -> {
                listeObservableUtilisateurs.clear();
                if (nouvelleListe != null) {
                    nouvelleListe.forEach(dto -> listeObservableUtilisateurs.add(new ModeleObservableUtilisateur(dto)));
                }
                tableVueUtilisateurs.refresh();
            });
        });

        // Écouteur pour les mises à jour d'un utilisateur unique (après modification par exemple)
        serviceComm.profilUtilisateurMisAJourProperty().addListener((obs, ancienProfil, nouveauProfil) -> {
            if (nouveauProfil != null) {
                Platform.runLater(() -> {
                    Optional<ModeleObservableUtilisateur> utilisateurExistant = listeObservableUtilisateurs.stream()
                            .filter(u -> u.getIdUtilisateur().equals(String.valueOf(nouveauProfil.getIdUtilisateur())))
                            .findFirst();
                    if (utilisateurExistant.isPresent()) {
                        utilisateurExistant.get().mettreAJourAvecDTO(nouveauProfil);
                    } else {
                        // Si l'utilisateur n'était pas dans la liste (cas peu probable après une modif, mais possible)
                        listeObservableUtilisateurs.add(new ModeleObservableUtilisateur(nouveauProfil));
                    }
                    tableVueUtilisateurs.refresh();
                });
            }
        });


        serviceCommunicationServeur.envoyerRequeteAdminObtenirUtilisateurs("");
    }

    @FXML
    private void initialize() {
        colonneIdUtilisateur.setCellValueFactory(cellData -> cellData.getValue().idUtilisateurProperty());
        colonneNomComplet.setCellValueFactory(cellData -> cellData.getValue().nomCompletProperty());
        colonneIdentifiantConnexion.setCellValueFactory(cellData -> cellData.getValue().identifiantProperty());

        colonneRole.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(paquetRessourcesI18n.getString("role." + cellData.getValue().getRole().name().toLowerCase()));
            } catch (Exception e) {
                return new SimpleStringProperty(cellData.getValue().getRole().name());
            }
        });
        colonneStatutCompte.setCellValueFactory(cellData -> {
            try {
                return new SimpleStringProperty(paquetRessourcesI18n.getString("statut.compte." + cellData.getValue().getStatutCompte().name().toLowerCase()));
            } catch (Exception e) {
                return new SimpleStringProperty(cellData.getValue().getStatutCompte().name());
            }
        });

        colonneDateCreation.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateCreationCompte() != null ? cellData.getValue().getDateCreationCompte().format(formateurDateHeure) : ""));
        colonneDerniereConnexion.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateDerniereConnexion() != null ? cellData.getValue().getDateDerniereConnexion().format(formateurDateHeure) : ""));

        configurerColonneActionsAdmin();
        tableVueUtilisateurs.setItems(listeFiltreeUtilisateurs);

        champRechercheUtilisateur.textProperty().addListener((observable, oldValue, newValue) -> {
            listeFiltreeUtilisateurs.setPredicate(utilisateur -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (utilisateur.getNomComplet().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (utilisateur.getIdentifiant().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        boutonCreerUtilisateur.setDisable(true); // Fonctionnalité non implémentée pour l'instant
        boutonCreerUtilisateur.setVisible(false);
    }

    private void configurerColonneActionsAdmin() {
        Callback<TableColumn<ModeleObservableUtilisateur, Void>, TableCell<ModeleObservableUtilisateur, Void>> cellFactory = param -> {
            final TableCell<ModeleObservableUtilisateur, Void> cell = new TableCell<>() {
                private final Button boutonModifier = new Button(paquetRessourcesI18n.getString("button.edit"));
                private final Button boutonChangerStatut = new Button(); // Texte dynamique
                private final HBox conteneurActions = new HBox(5, boutonModifier, boutonChangerStatut);

                {
                    boutonModifier.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        if (utilisateur != null) {
                            ouvrirDialogueModificationUtilisateur(utilisateur);
                        }
                    });
                    boutonChangerStatut.setOnAction(event -> {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        if (utilisateur != null) {
                            changerStatutCompteUtilisateurInteractivement(utilisateur);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        ModeleObservableUtilisateur utilisateur = getTableView().getItems().get(getIndex());
                        DonneesUtilisateurDTO adminActuel = serviceSessionUtilisateur.getUtilisateurConnecte();

                        boolean estSoiMeme = adminActuel != null && String.valueOf(adminActuel.getIdUtilisateur()).equals(utilisateur.getIdUtilisateur());
                        boutonModifier.setDisable(estSoiMeme);
                        boutonChangerStatut.setDisable(estSoiMeme);

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
    private void actionRechercherUtilisateurs(ActionEvent evenement) {
        String termeRecherche = champRechercheUtilisateur.getText();
        // Le filtrage est déjà dynamique grâce au listener.
        // Si une recherche serveur est voulue, elle serait déclenchée ici.
        journal.debug("Action de recherche cliquée, filtre actuel : {}", termeRecherche);
        serviceCommunicationServeur.envoyerRequeteAdminObtenirUtilisateurs(termeRecherche);
    }

    @FXML
    private void actionCreerNouvelUtilisateur(ActionEvent evenement) {
        journal.info("Fonctionnalité de création d'utilisateur par admin non implémentée.");
        AlertesUtilisateur.afficherInformation("Fonctionnalité en développement", "La création d'utilisateurs depuis ce panneau n'est pas encore disponible.");
    }

    private void ouvrirDialogueModificationUtilisateur(ModeleObservableUtilisateur utilisateur) {
        Dialog<DonneesUtilisateurDTO> dialogue = new Dialog<>();
        dialogue.setTitle(paquetRessourcesI18n.getString("admin.edit.user.dialog.title"));
        dialogue.setHeaderText(paquetRessourcesI18n.getString("admin.edit.user.dialog.header") + " " + utilisateur.getNomComplet());

        ButtonType boutonSauvegarderType = new ButtonType(paquetRessourcesI18n.getString("button.save"), ButtonData.OK_DONE);
        dialogue.getDialogPane().getButtonTypes().addAll(boutonSauvegarderType, ButtonType.CANCEL);

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);

        ComboBox<RoleUtilisateur> comboBoxRole = new ComboBox<>();
        comboBoxRole.getItems().setAll(RoleUtilisateur.values());
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
                DonneesUtilisateurDTO dtoModifie = new DonneesUtilisateurDTO();
                // On ne modifie que le rôle ici, le statut est géré par un autre bouton/dialogue.
                // L'ID est crucial pour que le serveur sache quel utilisateur modifier.
                dtoModifie.setIdUtilisateur(Long.parseLong(utilisateur.getIdUtilisateur()));
                dtoModifie.setRole(comboBoxRole.getValue());
                dtoModifie.setStatutCompte(utilisateur.getStatutCompte()); // Conserver le statut actuel
                return dtoModifie;
            }
            return null;
        });

        Optional<DonneesUtilisateurDTO> resultat = dialogue.showAndWait();
        resultat.ifPresent(dto -> {
            if (dto.getRole() != utilisateur.getRole()) { // Vérifier s'il y a eu un changement
                serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dto);
                journal.info("Demande de modification du rôle pour l'utilisateur ID {} à {}", dto.getIdUtilisateur(), dto.getRole());
            } else {
                journal.info("Aucun changement de rôle détecté pour l'utilisateur ID {}.", dto.getIdUtilisateur());
            }
        });
    }

    private void changerStatutCompteUtilisateurInteractivement(ModeleObservableUtilisateur utilisateur) {
        StatutCompteUtilisateur statutActuel = utilisateur.getStatutCompte();
        StatutCompteUtilisateur nouveauStatutPropose = (statutActuel == StatutCompteUtilisateur.ACTIF) ? StatutCompteUtilisateur.INACTIF : StatutCompteUtilisateur.ACTIF;

        String messageConfirmation = String.format(
                paquetRessourcesI18n.getString("admin.confirm.status.change." + nouveauStatutPropose.name().toLowerCase()),
                utilisateur.getNomComplet()
        );

        if (AlertesUtilisateur.afficherConfirmation(paquetRessourcesI18n.getString("admin.confirm.status.change.title"), messageConfirmation)) {
            DonneesUtilisateurDTO dtoPourModification = new DonneesUtilisateurDTO();
            dtoPourModification.setIdUtilisateur(Long.parseLong(utilisateur.getIdUtilisateur()));
            dtoPourModification.setRole(utilisateur.getRole()); // Conserver le rôle actuel
            dtoPourModification.setStatutCompte(nouveauStatutPropose);

            serviceCommunicationServeur.envoyerRequeteAdminModifierUtilisateur(dtoPourModification);
            journal.info("Demande de changement de statut pour l'utilisateur ID {} à {}", utilisateur.getIdUtilisateur(), nouveauStatutPropose);
        }
    }
}