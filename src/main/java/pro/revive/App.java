package pro.revive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Load the Login screen (Personnel module entry point).
        // After a successful login, the LoginController calls Navigator.login(name, id)
        // which sets the current user and navigates to the Triage Dashboard.
        URL fxmlUrl = getClass().getResource("/ResourcesUser/images/fxml/Login.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("Cannot find /ResourcesUser/images/fxml/Login.fxml in resources!");
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        URL cssUrl = getClass().getResource("/ResourcesUser/images/css/user.css");
        Scene scene = new Scene(root, 1100, 750);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("REVIVE");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
