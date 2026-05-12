package pro.revive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    /** Exposed so Navigator and any module can swap scenes on the main window. */
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesUser/images/fxml/Login.fxml");
            if (fxmlUrl == null) {
                throw new RuntimeException("Cannot find Login.fxml in resources!");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            URL cssUrl = getClass().getResource("/ResourcesUser/images/css/user.css");
            Scene scene = new Scene(root, 1100, 750);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle("REVIVE — Connexion");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Throwable e) {
            System.err.println("=== STARTUP ERROR ===");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
