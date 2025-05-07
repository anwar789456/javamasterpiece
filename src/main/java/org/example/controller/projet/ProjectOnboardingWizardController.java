package org.example.controller.projet;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class ProjectOnboardingWizardController implements Initializable {
    @FXML private ProgressBar progressBar;
    @FXML private Text stepTitle;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private CheckBox dontShowAgain;
    
    @FXML private VBox welcomeStep;
    @FXML private VBox searchStep;
    @FXML private VBox aiStep;
    @FXML private VBox collaborationStep;
    @FXML private VBox finalStep;
    @FXML private ImageView searchDemoImage;

    private List<VBox> steps;
    private int currentStepIndex = 0;
    private static final String PREF_SHOW_ONBOARDING = "show_onboarding";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeSteps();
        updateButtons();
        updateProgress();
        loadSearchDemoImage();
    }

    private void initializeSteps() {
        steps = new ArrayList<>();
        steps.add(welcomeStep);
        steps.add(searchStep);
        steps.add(aiStep);
        steps.add(collaborationStep);
        steps.add(finalStep);

        updateStepTitle();
    }

    private void loadSearchDemoImage() {
        try {
            Image image = new Image(getClass().getResourceAsStream("/images/search-demo.png"));
            if (image != null && !image.isError()) {
                searchDemoImage.setImage(image);
            } else {
                // If image loading fails, hide the ImageView
                searchDemoImage.setVisible(false);
                searchDemoImage.setManaged(false);
            }
        } catch (Exception e) {
            System.out.println("Could not load search demo image: " + e.getMessage());
            // Hide the ImageView if image loading fails
            searchDemoImage.setVisible(false);
            searchDemoImage.setManaged(false);
        }
    }

    @FXML
    private void previousStep() {
        if (currentStepIndex > 0) {
            steps.get(currentStepIndex).setVisible(false);
            currentStepIndex--;
            steps.get(currentStepIndex).setVisible(true);
            updateButtons();
            updateProgress();
            updateStepTitle();
        }
    }

    @FXML
    private void nextStep() {
        if (currentStepIndex < steps.size() - 1) {
            steps.get(currentStepIndex).setVisible(false);
            currentStepIndex++;
            steps.get(currentStepIndex).setVisible(true);
            updateButtons();
            updateProgress();
            updateStepTitle();
        } else {
            savePreference();
            closeWizard();
        }
    }

    private void updateButtons() {
        prevButton.setDisable(currentStepIndex == 0);
        nextButton.setText(currentStepIndex == steps.size() - 1 ? "Finish" : "Next");
    }

    private void updateProgress() {
        double progress = (double) (currentStepIndex + 1) / steps.size();
        progressBar.setProgress(progress);
    }

    private void updateStepTitle() {
        String title = switch (currentStepIndex) {
            case 0 -> "Welcome";
            case 1 -> "Finding Projects";
            case 2 -> "AI Assistant";
            case 3 -> "Collaboration";
            case 4 -> "Ready to Start";
            default -> "";
        };
        stepTitle.setText(title);
    }

    private void savePreference() {
        if (dontShowAgain.isSelected()) {
            Preferences prefs = Preferences.userNodeForPackage(ProjectOnboardingWizardController.class);
            prefs.putBoolean(PREF_SHOW_ONBOARDING, false);
        }
    }

    private void closeWizard() {
        Stage stage = (Stage) nextButton.getScene().getWindow();
        stage.close();
    }

    public static boolean shouldShowOnboarding() {
        Preferences prefs = Preferences.userNodeForPackage(ProjectOnboardingWizardController.class);
        return prefs.getBoolean(PREF_SHOW_ONBOARDING, true);
    }
} 