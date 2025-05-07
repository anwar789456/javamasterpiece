package org.example.models.projet;

public class Postuler {
    private int idProjet;
    private String firstName;
    private String email;
    private String joiningReason;
    private String numTel;

    public Postuler(int idProjet, String firstName, String email, String joiningReason, String numTel) {
        this.idProjet = idProjet;
        this.firstName = firstName;
        this.email = email;
        this.joiningReason = joiningReason;
        this.numTel = numTel;
    }

    // Getters
    public int getIdProjet() {
        return idProjet;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmail() {
        return email;
    }

    public String getJoiningReason() {
        return joiningReason;
    }

    public String getNumTel() {
        return numTel;
    }

    // Setters
    public void setIdProjet(int idProjet) {
        this.idProjet = idProjet;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setJoiningReason(String joiningReason) {
        this.joiningReason = joiningReason;
    }

    public void setNumTel(String numTel) {
        this.numTel = numTel;
    }
}