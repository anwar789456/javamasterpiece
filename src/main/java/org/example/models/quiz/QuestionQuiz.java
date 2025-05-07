package org.example.models.quiz;

import java.time.LocalDateTime;

public class QuestionQuiz {
    private int id;
    private int idQuiz;
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private int bonneReponse;
    private String explication;
    private LocalDateTime dateCreation;
    private String typeQuestion;

    // Constructeur par défaut
    public QuestionQuiz() {
    }

    // Constructeur avec tous les champs
    public QuestionQuiz(int id, int idQuiz, String question, String option1, String option2,
                       String option3, String option4, int bonneReponse, String explication,
                       LocalDateTime dateCreation, String typeQuestion) {
        this.id = id;
        this.idQuiz = idQuiz;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.bonneReponse = bonneReponse;
        this.explication = explication;
        this.dateCreation = dateCreation;
        this.typeQuestion = typeQuestion;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdQuiz() {
        return idQuiz;
    }

    public void setIdQuiz(int idQuiz) {
        this.idQuiz = idQuiz;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption4() {
        return option4;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public int getBonneReponse() {
        return bonneReponse;
    }

    public void setBonneReponse(int bonneReponse) {
        this.bonneReponse = bonneReponse;
    }

    public String getExplication() {
        return explication;
    }

    public void setExplication(String explication) {
        this.explication = explication;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getTypeQuestion() {
        return typeQuestion;
    }

    public void setTypeQuestion(String typeQuestion) {
        this.typeQuestion = typeQuestion;
    }

    @Override
    public String toString() {
        return "QuestionQuiz{" +
                "id=" + id +
                ", idQuiz=" + idQuiz +
                ", question='" + question + '\'' +
                ", option1='" + option1 + '\'' +
                ", option2='" + option2 + '\'' +
                ", option3='" + option3 + '\'' +
                ", option4='" + option4 + '\'' +
                ", bonneReponse=" + bonneReponse +
                ", explication='" + explication + '\'' +
                ", dateCreation=" + dateCreation +
                ", typeQuestion='" + typeQuestion + '\'' +
                '}';
    }
} 