package akandan.bahou.kassy.client.controleur;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import akandan.bahou.kassy.commun.util.ExceptionValidation;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;

public class ControleurPlanificationReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreFenetre;
    @FXML private TextField champTitreReunion;
    @FXML private TextArea champDescriptionReunion;
    @FXML private DatePicker datePickerDateDebut;
    @FXML private ComboBox<LocalTime> comboBoxHeureDebut;
    @FXML private Spinner<Integer> spinnerDureeMinutes;
    @FXML private ComboBox<TypeReunion> comboBoxTypeReunion;
    @FXML private GridPane grilleMotDePasse;
    @FXML private Label etiquetteMotDePasseReunion;
    @FXML private PasswordField champMotDePasseReunion;
    @FXML private Button boutonSauvegarderReunion;
    @FXML private Button boutonAnnuler;
    @FXML private Label etiquetteMessageErreurPlanification;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private DetailsReunionDTO reunionEnCoursModification;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurPlanificationReunion.class);
    private final DateTimeFormatter formateurHeureCombo = DateTimeFormatter.ofPattern("HH:mm");

    public ControleurPlanificationReunion() {
    }

    @Override
    public void initialiserDonneesEtServices(GestionnaireNavigation gestionnaireNav, ServiceCommunicationServeur serviceComm, ServiceSessionUtilisateur serviceSess, ResourceBundle paquetRessources, Object... donnees) {
        this.gestionnaireNavigation = gestionnaireNav;
        this.serviceCommunicationServeur = serviceComm;
        this.serviceSessionUtilisateur = serviceSess;
        this.paquetRessourcesI18n = paquetRessources;

        if (donnees != null && donnees.length > 0 && donnees[0] instanceof DetailsReunionDTO) {
            this.reunionEnCoursModification = (DetailsReunionDTO) donnees[0];
        } else {
            this.reunionEnCoursModification = null;
        }

        serviceComm.detailsReunionActuelleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                boolean estCreation = (reunionEnCoursModification == null);
                boolean idCorrespondSiModif = (!estCreation && newVal.getIdReunion() == reunionEnCoursModification.getIdReunion());
                boolean titreCorrespondSiCreation = (estCreation && newVal.getTitre().equals(champTitreReunion.getText()));

                if (titreCorrespondSiCreation || idCorrespondSiModif) {
                    Platform.runLater(() -> {
                        AlertesUtilisateur.afficherInformation(
                                paquetRessourcesI18n.getString(estCreation ? "meeting.creation.success.title" : "meeting.update.success.title"),
                                paquetRessourcesI18n.getString(estCreation ? "meeting.creation.success.content" : "meeting.update.success.content")
                        );
                        gestionnaireNavigation.afficherTableauDeBord();
                    });
                }
            }
        });
        initialiserChampsInterface();
    }

    @FXML
    private void initialize() {
        comboBoxTypeReunion.getItems().setAll(TypeReunion.values());
        comboBoxTypeReunion.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeReunion type) {
                if (type == null) return null;
                try {
                    return paquetRessourcesI18n.getString("type.reunion." + type.name().toLowerCase());
                } catch (Exception e) { return type.name(); }
            }
            @Override
            public TypeReunion fromString(String string) { return null; }
        });
        comboBoxTypeReunion.setOnAction(event -> gererVisibiliteChampMotDePasse());

        List<LocalTime> heures = IntStream.range(0, 48)
                .mapToObj(i -> LocalTime.of(i / 2, (i % 2) * 30))
                .collect(Collectors.toList());
        comboBoxHeureDebut.setItems(FXCollections.observableArrayList(heures));
        comboBoxHeureDebut.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? null : time.format(formateurHeureCombo);
            }
            @Override
            public LocalTime fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalTime.parse(string, formateurHeureCombo);
            }
        });

        SpinnerValueFactory.IntegerSpinnerValueFactory fabriqueDuree = new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60, 15);
        spinnerDureeMinutes.setValueFactory(fabriqueDuree);
        spinnerDureeMinutes.setEditable(true);
        etiquetteMessageErreurPlanification.setText("");
    }

    private void initialiserChampsInterface() {
        if (reunionEnCoursModification != null) {
            etiquetteTitreFenetre.setText(paquetRessourcesI18n.getString("meeting.edit.title"));
            boutonSauvegarderReunion.setText(paquetRessourcesI18n.getString("button.save.changes"));
            champTitreReunion.setText(reunionEnCoursModification.getTitre());
            champDescriptionReunion.setText(reunionEnCoursModification.getDescription());
            if (reunionEnCoursModification.getDateHeureDebut() != null) {
                datePickerDateDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalDate());
                comboBoxHeureDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalTime());
            } else {
                datePickerDateDebut.setValue(LocalDate.now().plusDays(1));
                comboBoxHeureDebut.setValue(LocalTime.of(9,0));
            }
            spinnerDureeMinutes.getValueFactory().setValue(reunionEnCoursModification.getDureeEstimeeMinutes());
            comboBoxTypeReunion.setValue(reunionEnCoursModification.getTypeReunion());
            if (reunionEnCoursModification.getTypeReunion() == TypeReunion.PRIVEE) {
                champMotDePasseReunion.setText(reunionEnCoursModification.getMotDePasseOptionnelValeur());
            }
        } else {
            etiquetteTitreFenetre.setText(paquetRessourcesI18n.getString("meeting.create.title"));
            boutonSauvegarderReunion.setText(paquetRessourcesI18n.getString("button.createMeeting.short"));
            datePickerDateDebut.setValue(LocalDate.now().plusDays(1));
            comboBoxHeureDebut.setValue(LocalTime.of(10,0));
            comboBoxTypeReunion.getSelectionModel().selectFirst();
            spinnerDureeMinutes.getValueFactory().setValue(60);
            champTitreReunion.clear();
            champDescriptionReunion.clear();
            champMotDePasseReunion.clear();
        }
        gererVisibiliteChampMotDePasse();
    }

    private void gererVisibiliteChampMotDePasse() {
        boolean estPrivee = comboBoxTypeReunion.getValue() == TypeReunion.PRIVEE;
        grilleMotDePasse.setVisible(estPrivee);
        grilleMotDePasse.setManaged(estPrivee);
        if (!estPrivee) {
            champMotDePasseReunion.clear();
        }
    }

    @FXML
    private void actionSauvegarderReunion(ActionEvent evenement) {
        etiquetteMessageErreurPlanification.setText("");
        String titre = champTitreReunion.getText();
        String description = champDescriptionReunion.getText();
        LocalDate dateDebut = datePickerDateDebut.getValue();
        LocalTime heureDebut = comboBoxHeureDebut.getValue();
        Integer duree = spinnerDureeMinutes.getValue();
        TypeReunion type = comboBoxTypeReunion.getValue();
        String motDePasse = (type == TypeReunion.PRIVEE) ? champMotDePasseReunion.getText() : null;

        try {
            ValidateurEntreeUtilisateur.validerNonNulOuVide(titre, paquetRessourcesI18n.getString("meeting.title.label"));
            if (dateDebut == null) throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.date.required"));
            if (heureDebut == null) throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.time.required"));
            if (duree == null || duree <= 0) throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.duration.positive"));
            if (type == null) throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.type.required"));
            if (type == TypeReunion.PRIVEE && (motDePasse == null || motDePasse.trim().isEmpty())) {
                throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.password.requiredforprivate"));
            }

            LocalDateTime dateHeureDebutComplete = LocalDateTime.of(dateDebut, heureDebut);
            if (dateHeureDebutComplete.isBefore(LocalDateTime.now().plusMinutes(1))) {
                throw new ExceptionValidation(paquetRessourcesI18n.getString("validation.meeting.datetime.future"));
            }

            DetailsReunionDTO dto = new DetailsReunionDTO();
            dto.setTitre(titre);
            dto.setDescription(description);
            dto.setDateHeureDebut(dateHeureDebutComplete);
            dto.setDureeEstimeeMinutes(duree);
            dto.setTypeReunion(type);
            if (type == TypeReunion.PRIVEE) {
                dto.setMotDePasseOptionnelValeur(motDePasse);
            }

            activerControles(false);

            if (reunionEnCoursModification != null) {
                dto.setIdReunion(reunionEnCoursModification.getIdReunion());
                dto.setStatutReunion(reunionEnCoursModification.getStatutReunion());
                dto.setIdOrganisateur(reunionEnCoursModification.getIdOrganisateur());
                dto.setDateCreationReunion(reunionEnCoursModification.getDateCreationReunion());
                serviceCommunicationServeur.envoyerRequeteModificationReunion(dto);
                journal.info("Tentative de modification de la réunion : {}", titre);
            } else {
                dto.setDateCreationReunion(LocalDateTime.now());
                serviceCommunicationServeur.envoyerRequeteCreationReunion(dto);
                journal.info("Tentative de création de la réunion : {}", titre);
            }
        } catch (ExceptionValidation e) {
            etiquetteMessageErreurPlanification.setText(e.getMessage());
            activerControles(true);
        } catch (Exception e) {
            journal.error("Erreur inattendue lors de la sauvegarde de la réunion", e);
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("error.generic.saving.meeting"));
            activerControles(true);
        }
    }

    private void activerControles(boolean actif) {
        champTitreReunion.setDisable(!actif);
        champDescriptionReunion.setDisable(!actif);
        datePickerDateDebut.setDisable(!actif);
        comboBoxHeureDebut.setDisable(!actif);
        spinnerDureeMinutes.setDisable(!actif);
        comboBoxTypeReunion.setDisable(!actif);
        champMotDePasseReunion.setDisable(!actif);
        boutonSauvegarderReunion.setDisable(!actif);
        boutonAnnuler.setDisable(!actif);
    }

    @FXML
    private void actionAnnuler(ActionEvent evenement) {
        gestionnaireNavigation.afficherTableauDeBord();
    }
}