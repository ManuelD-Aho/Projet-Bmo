package akandan.bahou.kassy.client.modele;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import akandan.bahou.kassy.commun.dto.DetailsReunionDTO;
import akandan.bahou.kassy.commun.modele.StatutReunion;
import akandan.bahou.kassy.commun.modele.TypeReunion;

public class ModeleObservableReunion {

    private final IntegerProperty id;
    private final StringProperty titre;
    private final StringProperty description; // Le DTO utilise 'description', pas 'ordreDuJour' selon la dernière version.
    private final ObjectProperty<LocalDateTime> dateHeureDebut;
    private final IntegerProperty dureeEstimeeMinutes; // Corrigé: était dureeMinutes
    private final ObjectProperty<TypeReunion> typeReunion;
    private final ObjectProperty<StatutReunion> statutReunion;
    private final IntegerProperty idOrganisateur;
    private final StringProperty nomOrganisateur;
    private final IntegerProperty nombreParticipants;

    private DetailsReunionDTO detailsDTOOriginal;
    // Pas besoin de formateur ici si les dates sont ObjectProperty<LocalDateTime>
    // Le formatage se fera dans le CellValueFactory de la TableView

    public ModeleObservableReunion(DetailsReunionDTO dto) {
        this.id = new SimpleIntegerProperty((int)dto.getIdReunion()); // Cast en int si l'ID est petit, sinon StringProperty
        this.titre = new SimpleStringProperty(dto.getTitre());
        this.description = new SimpleStringProperty(dto.getDescription()); // Utilise getDescription()
        this.dateHeureDebut = new SimpleObjectProperty<>(dto.getDateHeureDebut()); // Supposant que le DTO a LocalDateTime
        this.dureeEstimeeMinutes = new SimpleIntegerProperty(dto.getDureeEstimeeMinutes());
        this.typeReunion = new SimpleObjectProperty<>(dto.getTypeReunion());
        this.statutReunion = new SimpleObjectProperty<>(dto.getStatutReunion());
        this.idOrganisateur = new SimpleIntegerProperty((int)dto.getIdOrganisateur());
        this.nomOrganisateur = new SimpleStringProperty(dto.getNomOrganisateur());
        this.nombreParticipants = new SimpleIntegerProperty(dto.getNombreParticipants()); // Utilise le champ du DTO
        this.detailsDTOOriginal = dto;
    }

    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

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

    public ObjectProperty<StatutReunion> statutReunionProperty() { return statutReunion; }
    public StatutReunion getStatutReunion() { return statutReunion.get(); }
    public void setStatutReunion(StatutReunion statutReunion) { this.statutReunion.set(statutReunion); }

    public IntegerProperty idOrganisateurProperty() { return idOrganisateur; }
    public int getIdOrganisateur() { return idOrganisateur.get(); }
    public void setIdOrganisateur(int idOrganisateur) { this.idOrganisateur.set(idOrganisateur); }

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
        this.id.set((int)dto.getIdReunion());
        this.titre.set(dto.getTitre());
        this.description.set(dto.getDescription());
        this.dateHeureDebut.set(dto.getDateHeureDebut());
        this.dureeEstimeeMinutes.set(dto.getDureeEstimeeMinutes());
        this.typeReunion.set(dto.getTypeReunion());
        this.statutReunion.set(dto.getStatutReunion());
        this.idOrganisateur.set((int)dto.getIdOrganisateur());
        this.nomOrganisateur.set(dto.getNomOrganisateur());
        this.nombreParticipants.set(dto.getNombreParticipants());
        this.detailsDTOOriginal = dto;
    }
}