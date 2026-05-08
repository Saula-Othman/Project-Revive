package pro.revive.utils.UtilsUser;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AnimationUtil {

    /**
     * Fade in a node from 0 to 1 opacity.
     */
    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Popup effect: scale from 0.85 + fade in simultaneously.
     */
    public static void popupIn(Node node, double durationMs) {
        node.setOpacity(0);
        node.setScaleX(0.85);
        node.setScaleY(0.85);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(durationMs), node);
        st.setFromX(0.85);
        st.setFromY(0.85);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
    }

    /**
     * Slide in from right: translates from +60px to 0 + fade in.
     */
    public static void slideInFromRight(Node node, double durationMs) {
        node.setOpacity(0);
        node.setTranslateX(60);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromX(60);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    /**
     * Navigate with fade transition:
     * fade out current root → swap root → fade in new root.
     */
    public static void navigateWithFade(Stage stage, Parent newRoot, Runnable onSwap) {
        Parent currentRoot = (Parent) stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), currentRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            onSwap.run();
            newRoot.setOpacity(0);
            stage.getScene().setRoot(newRoot);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), newRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }
}
