package org.example.models;

/**
 * Model class for declined teacher applications
 */
public class DeclinedApplication {
    private int id;
    private int applicationId;
    private int adminId;
    private String declineReason;
    private String adminName; // Transient field for display purposes

    public DeclinedApplication() {
    }

    public DeclinedApplication(int id, int applicationId, int adminId, String declineReason) {
        this.id = id;
        this.applicationId = applicationId;
        this.adminId = adminId;
        this.declineReason = declineReason;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    @Override
    public String toString() {
        return "DeclinedApplication{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", adminId=" + adminId +
                ", declineReason='" + declineReason + '\'' +
                ", adminName='" + adminName + '\'' +
                '}';
    }
} 