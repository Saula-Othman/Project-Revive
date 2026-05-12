package pro.revive.utils.UtilsUser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Navigation utility — swaps the scene root with a fade animation,
 * so the window stays maximized at all times.
 */
public class NavUtil {

    public static FXMLLoader navigateTo(Stage stage, String fxmlPath) throws Exception {
        URL fxmlUrl = NavUtil.class.getResource(fxmlPath);
        if (fxmlUrl == null) throw new RuntimeException("FXML not found: " + fxmlPath);
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent newRoot = loader.load();

        // Navigate with fade animation
        AnimationUtil.navigateWithFade(stage, newRoot, () -> {
            URL css = NavUtil.class.getResource("/ResourcesUser/images/css/user.css");
            if (css != null && !stage.getScene().getStylesheets().contains(css.toExternalForm())) {
                stage.getScene().getStylesheets().add(css.toExternalForm());
            }
        });

        return loader;
    }
}
