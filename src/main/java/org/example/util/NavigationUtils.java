package org.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class NavigationUtils {

    public static <T> T loadFXML(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationUtils.class.getResource(fxmlPath));
        Parent root = loader.load();
        return loader.getController();
    }

    public static <T> T loadFXMLWithData(String fxmlPath, Object data) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationUtils.class.getResource(fxmlPath));
        Parent root = loader.load();
        T controller = loader.getController();

        if (controller instanceof DataInitializable) {
            ((DataInitializable) controller).initializeWithData(data);
        }

        return controller;
    }

    public static void showStage(Object controller) throws IOException {
        if (controller == null) return;

        FXMLLoader loader = new FXMLLoader();
        loader.setController(controller);

        // Get the FXML path based on the controller class name
        String fxmlPath = "/org/example/view/projet/" +
                controller.getClass().getSimpleName().replace("Controller", "") +
                ".fxml";

        Parent root = loader.load(NavigationUtils.class.getResource(fxmlPath));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.show();
    }

    public interface DataInitializable {
        void initializeWithData(Object data);
    }
}