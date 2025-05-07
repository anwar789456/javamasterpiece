package org.example.model;

public class ApplicationTableItem {
    private final int projectId;
    private final String firstName;
    private final String email;
    private final String joiningReason;
    private final String numTel;
    private final String status;

    public ApplicationTableItem(int projectId, String firstName, String email,
                                String joiningReason, String numTel, String status) {
        this.projectId = projectId;
        this.firstName = firstName;
        this.email = email;
        this.joiningReason = joiningReason;
        this.numTel = numTel;
        this.status = status != null ? status : "Pending";
    }

    public int getProjectId() { return projectId; }
    public String getFirstName() { return firstName; }
    public String getEmail() { return email; }
    public String getJoiningReason() { return joiningReason; }
    public String getNumTel() { return numTel; }
    public String getStatus() { return status; }
}