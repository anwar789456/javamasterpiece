package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import org.example.model.ProjectData;
import org.example.util.NavigationUtils;

import java.io.IOException;

public class ProjectCardController {
    @FXML
    private ImageView imgProject;
    @FXML
    private Text lblProjectName;
    @FXML
    private Text lblDescription;
    @FXML
    private Text lblDates;
    @FXML
    private Text lblStatus;
    @FXML
    private Button btnPostuler;

    private ProjectData project;

    public void setProject(ProjectData project) {
        this.project = project;
        updateUI();
    }

    private void updateUI() {
        if (project == null) return;

        // Set project name
        lblProjectName.setText(project.getName());

        // Set description (truncate if too long)
        String description = project.getDescription();
        if (description.length() > 150) {
            description = description.substring(0, 147) + "...";
        }
        lblDescription.setText(description);

        // Format dates
        String dates = String.format("%s - %s", 
            project.getStartDate().toString(),
            project.getEndDate().toString());
        lblDates.setText(dates);

        // Set status with appropriate color
        String status = project.getStatus();
        lblStatus.setText(status);
        switch (status.toLowerCase()) {
            case "open":
                lblStatus.setStyle("-fx-fill: #4CAF50;"); // Green
                break;
            case "closed":
                lblStatus.setStyle("-fx-fill: #F44336;"); // Red
                break;
            case "pending":
                lblStatus.setStyle("-fx-fill: #FFC107;"); // Yellow
                break;
            default:
                lblStatus.setStyle("-fx-fill: #2196F3;"); // Blue
        }

        // Set project image if available
        if (project.getImageUrl() != null && !project.getImageUrl().isEmpty()) {
            imgProject.setImage(new javafx.scene.image.Image(project.getImageUrl()));
        }

        // Enable/disable apply button based on project status
        btnPostuler.setDisable(!status.equalsIgnoreCase("open"));
    }

    @FXML
    private void onPostuler() {
        try {
            PostulerController controller = NavigationUtils.loadFXMLWithData("/org/example/view/projet/Postuler.fxml", project.getId());
            NavigationUtils.showStage(controller);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 