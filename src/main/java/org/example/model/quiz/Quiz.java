package org.example.model.quiz;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Quiz {
    private final IntegerProperty id;
    private final StringProperty titre;
    private final StringProperty description;
    private final ObjectProperty<LocalDateTime> dateCreation;
    private final ObjectProperty<LocalDateTime> dateDebut;
    private final ObjectProperty<LocalDateTime> dateFin;
    private final StringProperty statut;
    private List<QuestionQuiz> questions;

    public Quiz(int id, String titre, String description, LocalDateTime dateCreation,
               LocalDateTime dateDebut, LocalDateTime dateFin, String statut) {
        this.id = new SimpleIntegerProperty(id);
        this.titre = new SimpleStringProperty(titre);
        this.description = new SimpleStringProperty(description);
        this.dateCreation = new SimpleObjectProperty<>(dateCreation);
        this.dateDebut = new SimpleObjectProperty<>(dateDebut);
        this.dateFin = new SimpleObjectProperty<>(dateFin);
        this.statut = new SimpleStringProperty(statut);
        this.questions = new ArrayList<>();
    }

    // Getters
    public int getId() { return id.get(); }
    public String getTitre() { return titre.get(); }
    public String getDescription() { return description.get(); }
    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public LocalDateTime getDateDebut() { return dateDebut.get(); }
    public LocalDateTime getDateFin() { return dateFin.get(); }
    public String getStatut() { return statut.get(); }
    public List<QuestionQuiz> getQuestions() { return questions; }

    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty titreProperty() { return titre; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }
    public ObjectProperty<LocalDateTime> dateDebutProperty() { return dateDebut; }
    public ObjectProperty<LocalDateTime> dateFinProperty() { return dateFin; }
    public StringProperty statutProperty() { return statut; }

    // Setter pour les questions
    public void setQuestions(List<QuestionQuiz> questions) {
        this.questions = questions;
    }
} 