package org.example.models.lesson;

public class Lesson {
    private int id;
    private Integer idcours_id; // Can be null according to schema
    private String titre;
    private String description;
    private String videourl; // Can be null according to schema
    private int ordre;
    
    // Default constructor
    public Lesson() {
    }
    
    // Constructor with all fields except id (for creation)
    public Lesson(Integer idcours_id, String titre, String description, String videourl, int ordre) {
        this.idcours_id = idcours_id;
        this.titre = titre;
        this.description = description;
        this.videourl = videourl;
        this.ordre = ordre;
    }
    
    // Constructor with all fields (for retrieval from database)
    public Lesson(int id, Integer idcours_id, String titre, String description, String videourl, int ordre) {
        this.id = id;
        this.idcours_id = idcours_id;
        this.titre = titre;
        this.description = description;
        this.videourl = videourl;
        this.ordre = ordre;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Integer getIdcours_id() {
        return idcours_id;
    }
    
    public void setIdcours_id(Integer idcours_id) {
        this.idcours_id = idcours_id;
    }
    
    public String getTitre() {
        return titre;
    }
    
    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVideourl() {
        return videourl;
    }
    
    public void setVideourl(String videourl) {
        this.videourl = videourl;
    }
    
    // Method to get the video path for the learning interface
    public String getVideoPath() {
        return videourl;
    }
    
    public int getOrdre() {
        return ordre;
    }
    
    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }
    
    @Override
    public String toString() {
        return "Lesson{" +
                "id=" + id +
                ", idcours_id=" + idcours_id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", videourl='" + videourl + '\'' +
                ", ordre=" + ordre +
                '}';
    }
} 