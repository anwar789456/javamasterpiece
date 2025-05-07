package org.example.models.course;

import java.time.LocalDateTime;
import org.example.services.SentimentAnalysisService.Sentiment;

/**
 * Model class representing a course review from the review table
 */
public class Review {
    private int id;
    private int idcours_id;
    private int user_id;
    private boolean thumbsup;
    private String review;
    private LocalDateTime datecreation;
    private Sentiment sentiment;
    
    // Default constructor
    public Review() {
        this.datecreation = LocalDateTime.now();
        this.sentiment = Sentiment.NEUTRAL;
    }
    
    // Constructor with all fields except id (for creation)
    public Review(int idcours_id, int user_id, boolean thumbsup, String review) {
        this.idcours_id = idcours_id;
        this.user_id = user_id;
        this.thumbsup = thumbsup;
        this.review = review;
        this.datecreation = LocalDateTime.now();
        this.sentiment = Sentiment.NEUTRAL;
    }
    
    // Constructor with all fields (for retrieval from database)
    public Review(int id, int idcours_id, int user_id, boolean thumbsup, String review, LocalDateTime datecreation) {
        this.id = id;
        this.idcours_id = idcours_id;
        this.user_id = user_id;
        this.thumbsup = thumbsup;
        this.review = review;
        this.datecreation = datecreation;
        this.sentiment = Sentiment.NEUTRAL;
    }
    
    // Constructor with all fields including sentiment
    public Review(int id, int idcours_id, int user_id, boolean thumbsup, String review, LocalDateTime datecreation, Sentiment sentiment) {
        this.id = id;
        this.idcours_id = idcours_id;
        this.user_id = user_id;
        this.thumbsup = thumbsup;
        this.review = review;
        this.datecreation = datecreation;
        this.sentiment = sentiment;
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
    
    public int getUser_id() {
        return user_id;
    }
    
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
    
    public boolean isThumbsup() {
        return thumbsup;
    }
    
    public void setThumbsup(boolean thumbsup) {
        this.thumbsup = thumbsup;
    }
    
    public String getReview() {
        return review;
    }
    
    public void setReview(String review) {
        this.review = review;
    }
    
    public LocalDateTime getDatecreation() {
        return datecreation;
    }
    
    public void setDatecreation(LocalDateTime datecreation) {
        this.datecreation = datecreation;
    }
    
    public Sentiment getSentiment() {
        return sentiment;
    }
    
    public void setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
    }
    
    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", idcours_id=" + idcours_id +
                ", user_id=" + user_id +
                ", thumbsup=" + thumbsup +
                ", review='" + review + '\'' +
                ", datecreation=" + datecreation +
                ", sentiment=" + sentiment +
                '}';
    }
} 