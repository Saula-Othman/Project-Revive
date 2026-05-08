package pro.revive;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("REVIVE — Module 2 : Triage");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        Navigator.goTo("Dashboard");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
