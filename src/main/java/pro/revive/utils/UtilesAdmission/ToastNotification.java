package pro.revive.utils.UtilesAdmission;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class ToastNotification {

    public enum ToastType { SUCCESS, ERROR, WARNING, INFO }

    public static void show(Pane container, String message, ToastType type) {
        if (container == null) return;

        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(12, 18, 12, 14));
        toast.setMaxWidth(400);
        toast.setMinHeight(48);

        String icon, bgColor, borderColor;
        switch (type) {
            case SUCCESS: icon = "\u2713"; bgColor = "#f0fdf4"; borderColor = "#16a34a"; break;
            case ERROR:   icon = "\u2717"; bgColor = "#fef2f2"; borderColor = "#dc2626"; break;
            case WARNING: icon = "\u26a0"; bgColor = "#fffbeb"; borderColor = "#d97706"; break;
            default:      icon = "\u2139"; bgColor = "#eff6ff"; borderColor = "#2563eb"; break;
        }

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + borderColor + ";");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#1e293b;-fx-wrap-text:true;");
        msgLabel.setMaxWidth(340);
        HBox.setHgrow(msgLabel, Priority.ALWAYS);

        toast.getChildren().addAll(iconLabel, msgLabel);
        toast.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:4;" +
            "-fx-background-radius:6;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),10,0,0,3);"
        );

        // Position bottom-right using layout coords
        container.widthProperty().addListener((obs, ov, nv) ->
            toast.setLayoutX(nv.doubleValue() - toast.prefWidth(-1) - 20));
        container.heightProperty().addListener((obs, ov, nv) ->
            toast.setLayoutY(nv.doubleValue() - toast.prefHeight(-1) - 20));

        // Initial position (best guess)
        double w = container.getWidth() > 0 ? container.getWidth() : 800;
        double h = container.getHeight() > 0 ? container.getHeight() : 600;
        toast.setLayoutX(w - 430);
        toast.setLayoutY(h - 80);

        container.getChildren().add(toast);
        toast.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(toast));
        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }

    public static void success(Pane c, String msg) { show(c, msg, ToastType.SUCCESS); }
    public static void error(Pane c, String msg)   { show(c, msg, ToastType.ERROR); }
    public static void warning(Pane c, String msg) { show(c, msg, ToastType.WARNING); }
    public static void info(Pane c, String msg)    { show(c, msg, ToastType.INFO); }
}
