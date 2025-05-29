package akandan.bahou.kassy.client.controleur;

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
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import akandan.bahou.kassy.client.coeur.GestionnaireNavigation;
import akandan.bahou.kassy.client.service.ServiceCommunicationServeur;
import akandan.bahou.kassy.client.service.ServiceSessionUtilisateur;
import akandan.bahou.kassy.client.util.AlertesUtilisateur;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;
import akandan.bahou.kassy.commun.util.EnregistreurEvenementsBMO;
import akandan.bahou.kassy.commun.util.ValidateurEntreeUtilisateur;
import org.slf4j.Logger;

public class ControleurPlanificationReunion implements ControleurAvecInitialisation {

    @FXML private Label etiquetteTitreFenetrePlanification;
    @FXML private TextField champTitreReunion;
    @FXML private TextArea champDescriptionReunion;
    @FXML private DatePicker datePickerDateDebut;
    @FXML private ComboBox<LocalTime> comboBoxHeureDebut;
    @FXML private Spinner<Integer> spinnerDureeMinutes;
    @FXML private ComboBox<TypeReunion> comboBoxTypeReunion;
    @FXML private PasswordField champMotDePasseReunionOptionnel;
    @FXML private Button boutonSauvegarderReunion;
    @FXML private Button boutonAnnulerPlanification;
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

        initialiserChampsInterface();

        serviceCommunicationServeur.detailsReunionActuelleProperty().addListener((obs, ancienneValeur, nouvelleValeur) -> {
            if (nouvelleValeur != null) {
                // Si c'est la réunion qu'on vient de créer/modifier
                if (reunionEnCoursModification == null || // Nouvelle réunion
                        (reunionEnCoursModification != null && nouvelleValeur.getIdReunion() == reunionEnCoursModification.getIdReunion())) { // Modification
                    Platform.runLater(() -> {
                        AlertesUtilisateur.afficherInformation(
                                paquetRessourcesI18n.getString("meeting.planning.save.success.title"),
                                paquetRessourcesI18n.getString("meeting.planning.save.success.content")
                        );
                        gestionnaireNavigation.afficherTableauDeBord();
                    });
                }
            }
        });
    }

    @FXML
    private void initialize() {
        comboBoxTypeReunion.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeReunion type) {
                return type == null ? "" : paquetRessourcesI18n.getString("meeting.type." + type.name().toLowerCase());
            }
            @Override
            public TypeReunion fromString(String string) { return null; } // Non nécessaire pour ComboBox non éditable
        });
        comboBoxTypeReunion.setItems(FXCollections.observableArrayList(TypeReunion.values()));

        List<LocalTime> heures = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            heures.add(LocalTime.of(i, 0));
            heures.add(LocalTime.of(i, 30));
        }
        comboBoxHeureDebut.setItems(FXCollections.observableArrayList(heures));
        comboBoxHeureDebut.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.format(formateurHeure);
            }
            @Override
            public LocalTime fromString(String string) {
                return string == null || string.isEmpty() ? null : LocalTime.parse(string, formateurHeure);
            }
        });

        SpinnerValueFactory<Integer> fabriqueValeurDuree = new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60, 15); // min, max, initial, step
        spinnerDureeMinutes.setValueFactory(fabriqueValeurDuree);
        spinnerDureeMinutes.setEditable(true);

        if (paquetRessourcesI18n != null) {
            boutonSauvegarderReunion.setText(paquetRessourcesI18n.getString("common.button.save"));
            boutonAnnulerPlanification.setText(paquetRessourcesI18n.getString("common.button.cancel"));
        }
        etiquetteMessageErreurPlanification.setText("");
    }

    private void initialiserChampsInterface() {
        if (reunionEnCoursModification != null) {
            etiquetteTitreFenetrePlanification.setText(paquetRessourcesI18n.getString("meeting.planning.title.edit"));
            champTitreReunion.setText(reunionEnCoursModification.getTitre());
            champDescriptionReunion.setText(reunionEnCoursModification.getDescription());
            if (reunionEnCoursModification.getDateHeureDebut() != null) {
                datePickerDateDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalDate());
                comboBoxHeureDebut.setValue(reunionEnCoursModification.getDateHeureDebut().toLocalTime());
            }
            spinnerDureeMinutes.getValueFactory().setValue(reunionEnCoursModification.getDureeEstimeeMinutes());
            comboBoxTypeReunion.setValue(reunionEnCoursModification.getTypeReunion());
            champMotDePasseReunionOptionnel.setText(reunionEnCoursModification.getMotDePasseOptionnelValeur());
        } else {
            etiquetteTitreFenetrePlanification.setText(paquetRessourcesI18n.getString("meeting.planning.title.new"));
            datePickerDateDebut.setValue(LocalDate.now());
            comboBoxHeureDebut.setValue(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)); // Prochaine heure ronde
            comboBoxTypeReunion.getSelectionModel().selectFirst();
            spinnerDureeMinutes.getValueFactory().setValue(60);
        }
    }

    @FXML
    private void actionSauvegarderReunion(ActionEvent evenement) {
        String titre = champTitreReunion.getText();
        String description = champDescriptionReunion.getText();
        LocalDate dateDebut = datePickerDateDebut.getValue();
        LocalTime heureDebut = comboBoxHeureDebut.getValue();
        Integer dureeMinutes = spinnerDureeMinutes.getValue();
        TypeReunion typeReunion = comboBoxTypeReunion.getValue();
        String motDePasseOptionnel = champMotDePasseReunionOptionnel.getText();

        if (!ValidateurEntreeUtilisateur.estChaineNonVide(titre)) {
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.title.required"));
            return;
        }
        if (dateDebut == null) {
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.date.required"));
            return;
        }
        if (heureDebut == null) {
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.time.required"));
            return;
        }
        LocalDateTime dateHeureDebut = LocalDateTime.of(dateDebut, heureDebut);
        if (dateHeureDebut.isBefore(LocalDateTime.now().plusMinutes(5))) { // Au moins 5 min dans le futur
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.datetime.future"));
            return;
        }
        if (dureeMinutes == null || dureeMinutes <= 0) {
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.duration.positive"));
            return;
        }
        if (typeReunion == null) {
            afficherErreurPlanification(paquetRessourcesI18n.getString("validation.meeting.type.required"));
            return;
        }

        etiquetteMessageErreurPlanification.setText("");
        activerDesactiverControles(true);

        DetailsReunionDTO dto = new DetailsReunionDTO();
        if (reunionEnCoursModification != null) {
            dto.setIdReunion(reunionEnCoursModification.getIdReunion());
            // Conserver le statut si c'est une modification, le serveur le gérera
            dto.setStatutReunion(reunionEnCoursModification.getStatutReunion());
        } else {
            dto.setStatutReunion(StatutReunion.PLANIFIEE); // Nouveau
        }
        dto.setTitre(titre);
        dto.setDescription(description);
        dto.setDateHeureDebut(dateHeureDebut);
        dto.setDureeEstimeeMinutes(dureeMinutes);
        dto.setTypeReunion(typeReunion);
        dto.setMotDePasseOptionnelValeur((motDePasseOptionnel != null && motDePasseOptionnel.isEmpty()) ? null : motDePasseOptionnel);

        DonneesUtilisateurDTO organisateur = serviceSessionUtilisateur.getUtilisateurConnecte();
        if (organisateur != null) {
            dto.setIdOrganisateur(organisateur.getIdUtilisateur());
            dto.setNomOrganisateur(organisateur.getNomComplet());
        } else {
            journal.error("Impossible de sauvegarder la réunion, utilisateur non connecté.");
            afficherErreurPlanification(paquetRessourcesI18n.getString("error.not.logged.in.for.action"));
            activerDesactiverControles(false);
            return;
        }


        if (reunionEnCoursModification != null) {
            journal.info("Modification de la réunion : {}", titre);
            serviceCommunicationServeur.envoyerRequeteModificationReunion(dto);
        } else {
            journal.info("Création d'une nouvelle réunion : {}", titre);
            serviceCommunicationServeur.envoyerRequeteCreationReunion(dto);
        }
    }

    private void afficherErreurPlanification(String message) {
        etiquetteMessageErreurPlanification.setText(message);
        etiquetteMessageErreurPlanification.setVisible(true);
    }

    @FXML
    private void actionAnnulerPlanification(ActionEvent evenement) {
        gestionnaireNavigation.afficherTableauDeBord();
    }

    private void activerDesactiverControles(boolean desactiver) {
        champTitreReunion.setDisable(desactiver);
        champDescriptionReunion.setDisable(desactiver);
        datePickerDateDebut.setDisable(desactiver);
        comboBoxHeureDebut.setDisable(desactiver);
        spinnerDureeMinutes.setDisable(desactiver);
        comboBoxTypeReunion.setDisable(desactiver);
        champMotDePasseReunionOptionnel.setDisable(desactiver);
        boutonSauvegarderReunion.setDisable(desactiver);
        boutonAnnulerPlanification.setDisable(desactiver);
    }
}