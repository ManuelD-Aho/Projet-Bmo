package akandan.bahou.kassy.client.modele;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;

public class ModeleObservableReunion {

    private final StringProperty id;
    private final StringProperty titre;
    private final StringProperty description;
    private final ObjectProperty<LocalDateTime> dateHeureDebut;
    private final IntegerProperty dureeEstimeeMinutes;
    private final ObjectProperty<TypeReunion> typeReunion;
    private final ObjectProperty<StatutReunion> statut;
    private final StringProperty idOrganisateur; // Changé de idCreateur pour correspondre au DTO probable
    private final StringProperty nomOrganisateur; // Changé de nomCreateur
    private final IntegerProperty nombreParticipants;

    private DetailsReunionDTO detailsDTOOriginal;

    public ModeleObservableReunion(DetailsReunionDTO dto) {
        this.id = new SimpleStringProperty(String.valueOf(dto.getIdReunion())); // Supposant getIdReunion() et conversion en String
        this.titre = new SimpleStringProperty(dto.getTitre());
        this.description = new SimpleStringProperty(dto.getDescription());
        this.dateHeureDebut = new SimpleObjectProperty<>(dto.getDateHeureDebut());
        this.dureeEstimeeMinutes = new SimpleIntegerProperty(dto.getDureeEstimeeMinutes());
        this.typeReunion = new SimpleObjectProperty<>(dto.getTypeReunion());
        this.statut = new SimpleObjectProperty<>(dto.getStatutReunion());
        this.idOrganisateur = new SimpleStringProperty(String.valueOf(dto.getIdOrganisateur()));
        this.nomOrganisateur = new SimpleStringProperty(dto.getNomOrganisateur());
        this.nombreParticipants = new SimpleIntegerProperty(dto.getNombreParticipants());
        this.detailsDTOOriginal = dto;
    }

    public StringProperty idProperty() { return id; }
    public String getId() { return id.get(); }
    public void setId(String id) { this.id.set(id); }

    public StringProperty titreProperty() { return titre; }
    public String getTitre() { return titre.get(); }
    public void setTitre(String titre) { this.titre.set(titre); }

    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }

    public ObjectProperty<LocalDateTime> dateHeureDebutProperty() { return dateHeureDebut; }
    public LocalDateTime getDateHeureDebut() { return dateHeureDebut.get(); }
    public void setDateHeureDebut(LocalDateTime dateHeureDebut) { this.dateHeureDebut.set(dateHeureDebut); }

    public IntegerProperty dureeEstimeeMinutesProperty() { return dureeEstimeeMinutes; }
    public int getDureeEstimeeMinutes() { return dureeEstimeeMinutes.get(); }
    public void setDureeEstimeeMinutes(int dureeEstimeeMinutes) { this.dureeEstimeeMinutes.set(dureeEstimeeMinutes); }

    public ObjectProperty<TypeReunion> typeReunionProperty() { return typeReunion; }
    public TypeReunion getTypeReunion() { return typeReunion.get(); }
    public void setTypeReunion(TypeReunion typeReunion) { this.typeReunion.set(typeReunion); }

    public ObjectProperty<StatutReunion> statutProperty() { return statut; }
    public StatutReunion getStatut() { return statut.get(); }
    public void setStatut(StatutReunion statut) { this.statut.set(statut); }

    public StringProperty idOrganisateurProperty() { return idOrganisateur; }
    public String getIdOrganisateur() { return idOrganisateur.get(); }
    public void setIdOrganisateur(String idOrganisateur) { this.idOrganisateur.set(idOrganisateur); }

    public StringProperty nomOrganisateurProperty() { return nomOrganisateur; }
    public String getNomOrganisateur() { return nomOrganisateur.get(); }
    public void setNomOrganisateur(String nomOrganisateur) { this.nomOrganisateur.set(nomOrganisateur); }

    public IntegerProperty nombreParticipantsProperty() { return nombreParticipants; }
    public int getNombreParticipants() { return nombreParticipants.get(); }
    public void setNombreParticipants(int nombreParticipants) { this.nombreParticipants.set(nombreParticipants); }

    public DetailsReunionDTO getDetailsDTOOriginal() {
        return detailsDTOOriginal;
    }

    public void mettreAJourAvecDTO(DetailsReunionDTO dto) {
        this.id.set(String.valueOf(dto.getIdReunion()));
        this.titre.set(dto.getTitre());
        this.description.set(dto.getDescription());
        this.dateHeureDebut.set(dto.getDateHeureDebut());
        this.dureeEstimeeMinutes.set(dto.getDureeEstimeeMinutes());
        this.typeReunion.set(dto.getTypeReunion());
        this.statut.set(dto.getStatutReunion());
        this.idOrganisateur.set(String.valueOf(dto.getIdOrganisateur()));
        this.nomOrganisateur.set(dto.getNomOrganisateur());
        this.nombreParticipants.set(dto.getNombreParticipants());
        this.detailsDTOOriginal = dto;
    }
}