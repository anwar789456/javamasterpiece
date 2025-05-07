package org.example.models.interest;

/**
 * Model class representing an interest category from the interests table
 */
public class Interest {
    private int id;
    private String name;
    private boolean selected; // Used for UI selection, not persisted in database
    
    // Default constructor
    public Interest() {
    }
    
    // Constructor with id and name
    public Interest(int id, String name) {
        this.id = id;
        this.name = name;
        this.selected = false;
    }
    
    // Constructor with all fields
    public Interest(int id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Interest interest = (Interest) obj;
        return id == interest.id;
    }
    
    @Override
    public int hashCode() {
        return id;
    }
} 