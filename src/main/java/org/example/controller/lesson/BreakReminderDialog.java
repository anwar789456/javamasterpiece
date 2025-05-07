package org.example.controller.lesson;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * Dialog for setting break reminder times
 */
public class BreakReminderDialog {
    
    /**
     * Shows a dialog for setting up break reminder
     * 
     * @return A Pair containing hours, minutes, and seconds, or null if canceled
     */
    public static Pair<Integer, Pair<Integer, Integer>> showDialog() {
        // Create dialog
        Dialog<Pair<Integer, Pair<Integer, Integer>>> dialog = new Dialog<>();
        dialog.setTitle("Set Break Reminder");
        dialog.setHeaderText("Set a reminder to take a break");
        
        // Set button types
        ButtonType setButtonType = new ButtonType("Set Reminder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(setButtonType, ButtonType.CANCEL);
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Create time input fields
        Spinner<Integer> hoursSpinner = new Spinner<>(0, 23, 0);
        hoursSpinner.setEditable(true);
        hoursSpinner.setPrefWidth(80);
        
        Spinner<Integer> minutesSpinner = new Spinner<>(0, 59, 30);
        minutesSpinner.setEditable(true);
        minutesSpinner.setPrefWidth(80);
        
        Spinner<Integer> secondsSpinner = new Spinner<>(0, 59, 0);
        secondsSpinner.setEditable(true);
        secondsSpinner.setPrefWidth(80);
        
        // Force spinner to commit edits
        hoursSpinner.focusedProperty().addListener((s, ov, nv) -> {
            if (!nv) hoursSpinner.increment(0);
        });
        
        minutesSpinner.focusedProperty().addListener((s, ov, nv) -> {
            if (!nv) minutesSpinner.increment(0);
        });
        
        secondsSpinner.focusedProperty().addListener((s, ov, nv) -> {
            if (!nv) secondsSpinner.increment(0);
        });
        
        // Add fields to layout
        grid.add(new Label("Hours:"), 0, 0);
        grid.add(hoursSpinner, 1, 0);
        grid.add(new Label("Minutes:"), 0, 1);
        grid.add(minutesSpinner, 1, 1);
        grid.add(new Label("Seconds:"), 0, 2);
        grid.add(secondsSpinner, 1, 2);
        
        // Add explanation
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Set how long you want to study before being reminded to take a break."),
            grid
        );
        
        dialog.getDialogPane().setContent(content);
        
        // Request focus on the hours field by default
        dialog.setOnShown(e -> hoursSpinner.requestFocus());
        
        // Convert the result when dialog is closed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == setButtonType) {
                // Check if at least one field has a non-zero value
                if (hoursSpinner.getValue() == 0 && 
                    minutesSpinner.getValue() == 0 && 
                    secondsSpinner.getValue() == 0) {
                    
                    // Show error message
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Time");
                    alert.setHeaderText("Timer duration cannot be zero");
                    alert.setContentText("Please set a valid time duration for the break reminder.");
                    alert.showAndWait();
                    
                    return null;
                }
                
                return new Pair<>(
                    hoursSpinner.getValue(), 
                    new Pair<>(minutesSpinner.getValue(), secondsSpinner.getValue())
                );
            }
            return null;
        });
        
        return dialog.showAndWait().orElse(null);
    }
} 