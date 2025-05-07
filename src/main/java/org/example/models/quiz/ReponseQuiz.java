package org.example.models.quiz;

import java.time.LocalDateTime;

public class ReponseQuiz {
    private int id_rep;
    private int id_question;
    private String reponse_choisie;
    private LocalDateTime date_reponse;
    private int points;
    private int id_utilisateur;

    // Constructeur par défaut
    public ReponseQuiz() {
    }

    // Constructeur avec tous les champs
    public ReponseQuiz(int id_rep, int id_question, String reponse_choisie, 
                      LocalDateTime date_reponse, int points, int id_utilisateur) {
        this.id_rep = id_rep;
        this.id_question = id_question;
        this.reponse_choisie = reponse_choisie;
        this.date_reponse = date_reponse;
        this.points = points;
        this.id_utilisateur = id_utilisateur;
    }

    // Getters
    public int getId_rep() {
        return id_rep;
    }

    public int getId_question() {
        return id_question;
    }

    public String getReponse_choisie() {
        return reponse_choisie;
    }

    public LocalDateTime getDate_reponse() {
        return date_reponse;
    }

    public int getPoints() {
        return points;
    }

    public int getId_utilisateur() {
        return id_utilisateur;
    }

    // Setters
    public void setId_rep(int id_rep) {
        this.id_rep = id_rep;
    }

    public void setId_question(int id_question) {
        this.id_question = id_question;
    }

    public void setReponse_choisie(String reponse_choisie) {
        this.reponse_choisie = reponse_choisie;
    }

    public void setDate_reponse(LocalDateTime date_reponse) {
        this.date_reponse = date_reponse;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setId_utilisateur(int id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    @Override
    public String toString() {
        return "ReponseQuiz{" +
                "id_rep=" + id_rep +
                ", id_question=" + id_question +
                ", reponse_choisie='" + reponse_choisie + '\'' +
                ", date_reponse=" + date_reponse +
                ", points=" + points +
                ", id_utilisateur=" + id_utilisateur +
                '}';
    }
} 