package org.example.models.course;

import java.time.LocalDateTime;

public class Course {
    private int id;
    private int idowner_id;
    private String titre;
    private double price;
    private String img;
    private String description;
    private boolean is_free;
    private LocalDateTime datecreation;
    private int likes;
    private int dislikes;
    private int minimal_hours;
    private int maximal_hours;
    // Transient field not stored in database
    private int learnerCount;

    // Default constructor
    public Course() {
        this.datecreation = LocalDateTime.now();
        this.likes = 0;
        this.dislikes = 0;
        this.minimal_hours = 0;
        this.maximal_hours = 0;
        this.learnerCount = 0;
    }

    // Constructor with all fields except id (for creation)
    public Course(int idowner_id, String titre, double price, String img, String description, boolean is_free) {
        this.idowner_id = idowner_id;
        this.titre = titre;
        this.price = price;
        this.img = img;
        this.description = description;
        this.is_free = is_free;
        this.datecreation = LocalDateTime.now();
        this.likes = 0;
        this.dislikes = 0;
        this.minimal_hours = 0;
        this.maximal_hours = 0;
        this.learnerCount = 0;
    }

    // Constructor with all fields (for retrieval from database)
    public Course(int id, int idowner_id, String titre, double price, String img, String description, boolean is_free, LocalDateTime datecreation) {
        this.id = id;
        this.idowner_id = idowner_id;
        this.titre = titre;
        this.price = price;
        this.img = img;
        this.description = description;
        this.is_free = is_free;
        this.datecreation = datecreation;
        this.likes = 0;
        this.dislikes = 0;
        this.minimal_hours = 0;
        this.maximal_hours = 0;
        this.learnerCount = 0;
    }
    
    // Constructor with all fields including likes and dislikes
    public Course(int id, int idowner_id, String titre, double price, String img, String description, boolean is_free, LocalDateTime datecreation, int likes, int dislikes) {
        this.id = id;
        this.idowner_id = idowner_id;
        this.titre = titre;
        this.price = price;
        this.img = img;
        this.description = description;
        this.is_free = is_free;
        this.datecreation = datecreation;
        this.likes = likes;
        this.dislikes = dislikes;
        this.minimal_hours = 0;
        this.maximal_hours = 0;
        this.learnerCount = 0;
    }
    
    // Constructor with all fields including hours
    public Course(int id, int idowner_id, String titre, double price, String img, String description, boolean is_free, LocalDateTime datecreation, int likes, int dislikes, int minimal_hours, int maximal_hours) {
        this.id = id;
        this.idowner_id = idowner_id;
        this.titre = titre;
        this.price = price;
        this.img = img;
        this.description = description;
        this.is_free = is_free;
        this.datecreation = datecreation;
        this.likes = likes;
        this.dislikes = dislikes;
        this.minimal_hours = minimal_hours;
        this.maximal_hours = maximal_hours;
        this.learnerCount = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdowner_id() {
        return idowner_id;
    }

    public void setIdowner_id(int idowner_id) {
        this.idowner_id = idowner_id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isIs_free() {
        return is_free;
    }

    public void setIs_free(boolean is_free) {
        this.is_free = is_free;
    }

    public LocalDateTime getDatecreation() {
        return datecreation;
    }

    public void setDatecreation(LocalDateTime datecreation) {
        this.datecreation = datecreation;
    }
    
    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }
    
    public int getMinimal_hours() {
        return minimal_hours;
    }

    public void setMinimal_hours(int minimal_hours) {
        this.minimal_hours = minimal_hours;
    }

    public int getMaximal_hours() {
        return maximal_hours;
    }

    public void setMaximal_hours(int maximal_hours) {
        this.maximal_hours = maximal_hours;
    }
    
    public int getLearnerCount() {
        return learnerCount;
    }

    public void setLearnerCount(int learnerCount) {
        this.learnerCount = learnerCount;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", idowner_id=" + idowner_id +
                ", titre='" + titre + '\'' +
                ", price=" + price +
                ", img='" + img + '\'' +
                ", description='" + description + '\'' +
                ", is_free=" + is_free +
                ", datecreation=" + datecreation +
                ", likes=" + likes +
                ", dislikes=" + dislikes +
                ", minimal_hours=" + minimal_hours +
                ", maximal_hours=" + maximal_hours +
                ", learnerCount=" + learnerCount +
                '}';
    }
} 