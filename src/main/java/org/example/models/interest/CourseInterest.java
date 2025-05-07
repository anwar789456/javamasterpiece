package org.example.models.interest;

/**
 * Model class representing the course_interests join table for many-to-many relationship
 * between courses and interests
 */
public class CourseInterest {
    private int courseId;
    private int interestId;
    
    // Default constructor
    public CourseInterest() {
    }
    
    // Constructor with all fields
    public CourseInterest(int courseId, int interestId) {
        this.courseId = courseId;
        this.interestId = interestId;
    }
    
    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }
    
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
    
    public int getInterestId() {
        return interestId;
    }
    
    public void setInterestId(int interestId) {
        this.interestId = interestId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CourseInterest that = (CourseInterest) obj;
        return courseId == that.courseId && interestId == that.interestId;
    }
    
    @Override
    public int hashCode() {
        int result = courseId;
        result = 31 * result + interestId;
        return result;
    }
} 