package akandan.bahou.kassy.client.controleur;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.client.util.ValidateurInterfaceGraphique;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.modele.StatutReunion; // Requis pour créer le DTO
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import org.slf4j.Logger;

public class ControleurPlanificationReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreFenetre;
    @FXML private TextField champTitreReunion;
    @FXML private TextArea champDescriptionReunion;
    @FXML private DatePicker datePickerDateDebut;
    @FXML private ComboBox<LocalTime> comboBoxHeureDebut;
    @FXML private Spinner<Integer> spinnerDureeMinutes;
    @FXML private ComboBox<TypeReunion> comboBoxTypeReunion;
    @FXML private Button boutonSauvegarderReunion;
    @FXML private Button boutonAnnuler;
    @FXML private Label etiquetteMessageErreurPlanification;

    private GestionnaireNavigation gestionnaireNavigation;
    private ServiceCommunicationServeur serviceCommunicationServeur;
    private ServiceSessionUtilisateur serviceSessionUtilisateur;
    private ResourceBundle paquetRessourcesI18n;
    private DetailsReunionDTO reunionEnCoursModification;
    private static final Logger journal = EnregistreurEvenementsBMO.getLogger(ControleurPlanificationReunion.class);
    private final DateTimeFormatter formateurHeure = DateTimeFormatter.ofPattern("HH:mm");

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

        serviceComm.detailsReunionMiseAJourProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (reunionEnCoursModification == null || newVal.getIdReunion() == reunionEnCoursModification.getIdReunion())) {
                Platform.runLater(() -> {
                    AlertesUtilisateur.afficherInformation(
                            paquetRessourcesI18n.getString(reunionEnCoursModification == null ? "meeting.creation.success.title" : "meeting.update.success.title"),
                            paquetRessourcesI18n.getString(reunionEnCoursModification == null ? "meeting.creation.success.content" : "meeting.update.success.content")
                    );
                    gestionnaireNavigation.afficherTableauDeBord();
                });
            }
        });

        // Écouter les erreurs spécifiques à la création/modification si le service les expose séparément,
        // sinon, une alerte d'erreur générique du service sera affichée.

        initialiserChampsInterface();
    }

    @FXML
    private void initialize() {
        comboBoxTypeReunion.getItems().setAll(TypeReunion.values());
        comboBoxTypeReunion.setConverter(new StringConverter<TypeReunion>() {
            @Override
            public String toString(TypeReunion type) {
                if (type == null) return null;
                try {
                    return paquetRessourcesI18n.getString("type.reunion." + type.name().toLowerCase());
                } catch (Exception e) {
                    return type.name();
                }
            }
            @Override
            public TypeReunion fromString(String string) { return null; } // Non utilisé pour la sélection
        });

        IntStream.range(0, 48).forEach(i -> { // Toutes les 30 minutes sur 24h
            comboBoxHeureDebut.getItems().add(LocalTime.of(i / 2, (i % 2) * 30));
        });
        comboBoxHeureDebut.setConverter(new StringConverter<LocalTime>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? null : time.format(formateurHeure);
            }
            @Override
            public LocalTime fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalTime.parse(string, formateurHeure);
            }
        });

        SpinnerValueFactory.IntegerSpinnerValueFactory fabriqueDuree = new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60, 15); // min 15m, max 8h, initial 1h, step 15m
        spinnerDureeMinutes.setValueFactory(fabriqueDuree);
        spinnerDureeMinutes.setEditable(true); // Permettre la saisie directe

        etiquetteMessageErreurPlanification.setText("");
    }

    private void initialiserChampsInterface() {
        if (reunionEnCoursModification != null) {
            etiquetteTitreFenetre.setText(paquetRessourcesI18n.getString("meeting.edit.title"));
            champTitreReunion.setText(reunionEnCoursModification.getTitre());
            champDescriptionReunion.setText(reunionEnCoursModification.getDescription());
            if (reunionEnCoursModification.getDateHeureDebut() != null) {
                datePickerDateDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalDate());
                comboBoxHeureDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalTime());
            } else {
                datePickerDateDebut.setValue(LocalDate.now().plusDays(1));
                comboBoxHeureDebut.getSelectionModel().select(LocalTime.of(9,0)); // Default
            }
            spinnerDureeMinutes.getValueFactory().setValue(reunionEnCoursModification.getDureeEstimeeMinutes());
            comboBoxTypeReunion.setValue(reunionEnCoursModification.getTypeReunion());
        } else {
            etiquetteTitreFenetre.setText(paquetRessourcesI18n.getString("meeting.create.title"));
            datePickerDateDebut.setValue(LocalDate.now().plusDays(1)); // Valeur par défaut
            comboBoxHeureDebut.getSelectionModel().select(LocalTime.of(10,0));
            comboBoxTypeReunion.getSelectionModel().selectFirst();
            spinnerDureeMinutes.getValueFactory().setValue(60);
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

        if (!ValidateurInterfaceGraphique.estChampNonVide(titre)) {
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("validation.meeting.title.required"));
            return;
        }
        if (dateDebut == null) {
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("validation.meeting.date.required"));
            return;
        }
        if (heureDebut == null) {
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("validation.meeting.time.required"));
            return;
        }
        if (type == null) {
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("validation.meeting.type.required"));
            return;
        }

        LocalDateTime dateHeureDebutComplete = LocalDateTime.of(dateDebut, heureDebut);
        if (dateHeureDebutComplete.isBefore(LocalDateTime.now().plusMinutes(5))) { // Au moins 5 min dans le futur
            etiquetteMessageErreurPlanification.setText(paquetRessourcesI18n.getString("validation.meeting.datetime.future"));
            return;
        }

        DetailsReunionDTO dto = new DetailsReunionDTO();
        dto.setTitre(titre);
        dto.setDescription(description);
        dto.setDateHeureDebut(dateHeureDebutComplete);
        dto.setDureeEstimeeMinutes(duree);
        dto.setTypeReunion(type);
        // L'organisateur sera défini côté serveur basé sur la session
        // Le statut sera PLANIFIEE par défaut côté serveur pour une nouvelle réunion

        boutonSauvegarderReunion.setDisable(true);
        boutonAnnuler.setDisable(true);

        if (reunionEnCoursModification != null) {
            dto.setIdReunion(reunionEnCoursModification.getIdReunion());
            dto.setStatutReunion(reunionEnCoursModification.getStatutReunion()); // Conserver le statut actuel pour modification
            // Les autres champs comme idOrganisateur sont déjà dans reunionEnCoursModification et ne devraient pas être modifiables ici
            serviceCommunicationServeur.envoyerRequeteModificationReunion(dto);
            journal.info("Tentative de modification de la réunion : {}", titre);
        } else {
            // Pour une nouvelle réunion, le statut sera géré par le serveur.
            serviceCommunicationServeur.envoyerRequeteCreationReunion(dto);
            journal.info("Tentative de création de la réunion : {}", titre);
        }
        // La réponse (succès/erreur) et la navigation sont gérées par le listener sur detailsReunionMiseAJourProperty
        // ou par des alertes d'erreur génériques du service de communication.
        // Si une erreur spécifique de cette opération est reçue, il faudrait un moyen de la réafficher ici et réactiver les boutons.
        // Pour l'instant, on suppose que les erreurs génériques suffisent ou que le succès navigue.
        // Si l'opération échoue et qu'il n'y a pas de navigation, il faut réactiver les boutons :
        // Platform.runLater(() -> { boutonSauvegarderReunion.setDisable(false); boutonAnnuler.setDisable(false); });
        // Cela pourrait être fait dans un listener d'erreur spécifique si ServiceCommunicationServeur le fournit.
    }

    @FXML
    private void actionAnnuler(ActionEvent evenement) {
        gestionnaireNavigation.afficherTableauDeBord();
    }
}