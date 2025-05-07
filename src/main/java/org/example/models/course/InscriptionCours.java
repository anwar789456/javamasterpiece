package org.example.models.course;

import java.time.LocalDateTime;

/**
 * Model class representing a course enrollment from the inscriptioncours table
 */
public class InscriptionCours {
    private int id;
    private int idcours_id;
    private int iduser_id;
    private LocalDateTime dateinscription;
    
    // Default constructor
    public InscriptionCours() {
        this.dateinscription = LocalDateTime.now();
    }
    
    // Constructor with all fields except id (for creation)
    public InscriptionCours(int idcours_id, int iduser_id) {
        this.idcours_id = idcours_id;
        this.iduser_id = iduser_id;
        this.dateinscription = LocalDateTime.now();
    }
    
    // Constructor with all fields (for retrieval from database)
    public InscriptionCours(int id, int idcours_id, int iduser_id, LocalDateTime dateinscription) {
        this.id = id;
        this.idcours_id = idcours_id;
        this.iduser_id = iduser_id;
        this.dateinscription = dateinscription;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getIdcours_id() {
        return idcours_id;
    }
    
    public void setIdcours_id(int idcours_id) {
        this.idcours_id = idcours_id;
    }
    
    public int getIduser_id() {
        return iduser_id;
    }
    
    public void setIduser_id(int iduser_id) {
        this.iduser_id = iduser_id;
    }
    
    public LocalDateTime getDateinscription() {
        return dateinscription;
    }
    
    public void setDateinscription(LocalDateTime dateinscription) {
        this.dateinscription = dateinscription;
    }
    
    @Override
    public String toString() {
        return "InscriptionCours{" +
                "id=" + id +
                ", idcours_id=" + idcours_id +
                ", iduser_id=" + iduser_id +
                ", dateinscription=" + dateinscription +
                '}';
    }
} 