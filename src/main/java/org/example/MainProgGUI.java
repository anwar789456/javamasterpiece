package org.example;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainProgGUI extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Esprit Login");
        
        // Force database connection if needed
        try {
            utils.dataSource.getInstance();
        } catch (Exception e) {
            System.err.println("Warning: Database connection could not be established: " + e.getMessage());
            // Continue anyway for testing the UI
        }
        
        primaryStage.show();
    }
}