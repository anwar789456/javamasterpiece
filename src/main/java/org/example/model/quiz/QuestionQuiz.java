package org.example.model.quiz;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class QuestionQuiz {
    private final IntegerProperty id;
    private final IntegerProperty idQuiz;
    private final StringProperty question;
    private final StringProperty option1;
    private final StringProperty option2;
    private final StringProperty option3;
    private final StringProperty option4;
    private final IntegerProperty bonneReponse;
    private final StringProperty explication;
    private final ObjectProperty<LocalDateTime> dateCreation;
    private final StringProperty typeQuestion;
    private final StringProperty type;

    public QuestionQuiz(int id, int idQuiz, String question, String option1, String option2,
                       String option3, String option4, int bonneReponse, String explication,
                       LocalDateTime dateCreation, String typeQuestion) {
        this.id = new SimpleIntegerProperty(id);
        this.idQuiz = new SimpleIntegerProperty(idQuiz);
        this.question = new SimpleStringProperty(question);
        this.option1 = new SimpleStringProperty(option1);
        this.option2 = new SimpleStringProperty(option2);
        this.option3 = new SimpleStringProperty(option3);
        this.option4 = new SimpleStringProperty(option4);
        this.bonneReponse = new SimpleIntegerProperty(bonneReponse);
        this.explication = new SimpleStringProperty(explication);
        this.dateCreation = new SimpleObjectProperty<>(dateCreation);
        this.typeQuestion = new SimpleStringProperty(typeQuestion);
        this.type = new SimpleStringProperty(typeQuestion);
    }

    // Getters
    public int getId() { return id.get(); }
    public int getIdQuiz() { return idQuiz.get(); }
    public String getQuestion() { return question.get(); }
    public String getOption1() { return option1.get(); }
    public String getOption2() { return option2.get(); }
    public String getOption3() { return option3.get(); }
    public String getOption4() { return option4.get(); }
    public int getBonneReponse() { return bonneReponse.get(); }
    public String getExplication() { return explication.get(); }
    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public String getTypeQuestion() { return typeQuestion.get(); }
    public String getType() { return type.get(); }

    // Méthode pour obtenir toutes les options
    public List<String> getOptions() {
        return Arrays.asList(
            getOption1(),
            getOption2(),
            getOption3(),
            getOption4()
        );
    }

    // Méthode pour obtenir la réponse correcte
    public String getReponseCorrecte() {
        int index = getBonneReponse() - 1; // Convertir de 1-based à 0-based
        if (index >= 0 && index < 4) {
            return getOptions().get(index);
        }
        return null;
    }

    // Property getters
    public IntegerProperty idProperty() { return id; }
    public IntegerProperty idQuizProperty() { return idQuiz; }
    public StringProperty questionProperty() { return question; }
    public StringProperty option1Property() { return option1; }
    public StringProperty option2Property() { return option2; }
    public StringProperty option3Property() { return option3; }
    public StringProperty option4Property() { return option4; }
    public IntegerProperty bonneReponseProperty() { return bonneReponse; }
    public StringProperty explicationProperty() { return explication; }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }
    public StringProperty typeQuestionProperty() { return typeQuestion; }
    public StringProperty typeProperty() { return type; }
} 