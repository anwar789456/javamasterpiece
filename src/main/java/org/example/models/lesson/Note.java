package org.example.models.lesson;

/**
 * Model class representing a note from the notes table
 */
public class Note {
    private int id;
    private int user_id;
    private int lesson_id;
    private String note_text;
    
    // Default constructor
    public Note() {
    }
    
    // Constructor with all fields except id (for creation)
    public Note(int user_id, int lesson_id, String note_text) {
        this.user_id = user_id;
        this.lesson_id = lesson_id;
        this.note_text = note_text;
    }
    
    // Constructor with all fields (for retrieval from database)
    public Note(int id, int user_id, int lesson_id, String note_text) {
        this.id = id;
        this.user_id = user_id;
        this.lesson_id = lesson_id;
        this.note_text = note_text;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUser_id() {
        return user_id;
    }
    
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
    
    public int getLesson_id() {
        return lesson_id;
    }
    
    public void setLesson_id(int lesson_id) {
        this.lesson_id = lesson_id;
    }
    
    public String getNote_text() {
        return note_text;
    }
    
    public void setNote_text(String note_text) {
        this.note_text = note_text;
    }
    
    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", lesson_id=" + lesson_id +
                ", note_text='" + note_text + '\'' +
                '}';
    }
} 