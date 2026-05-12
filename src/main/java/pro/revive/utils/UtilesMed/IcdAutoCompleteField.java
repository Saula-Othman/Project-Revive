package pro.revive.utils.UtilesMed;

import pro.revive.services.ServicesMed.IcdSearchService;
import pro.revive.services.ServicesMed.IcdSearchService.IcdEntry;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Feature 1 — Champ TextArea avec autocomplete ICD-10.
 * Utilise ContextMenu (auto-positionne sous le champ, fonctionne dans ScrollPane).
 * Debounce 350ms — appel API dans un thread background.
 */
public class IcdAutoCompleteField {

    private final TextArea           textArea;
    private final IcdSearchService   service  = new IcdSearchService();
    private final ContextMenu        menu     = new ContextMenu();
    private final Consumer<IcdEntry> onSelect;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "icd-search");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> pendingTask;

    // Garde la derniere liste pour eviter les doublons
    private List<IcdEntry> currentResults;

    public IcdAutoCompleteField(TextArea textArea, Consumer<IcdEntry> onSelect) {
        this.textArea = textArea;
        this.onSelect = onSelect;
        configureMenu();
        configureListeners();
    }

    private void configureMenu() {
        menu.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #C8D8EE; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(11,78,162,0.18), 12, 0, 0, 4);");
        menu.setAutoHide(true);
    }

    private void configureListeners() {
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                menu.hide();
                return;
            }
            scheduleSearch(newVal.trim());
        });

        textArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) menu.hide();
        });
    }

    private void scheduleSearch(String query) {
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }
        pendingTask = scheduler.schedule(() -> {
            List<IcdEntry> results = service.search(query);
            Platform.runLater(() -> showResults(results));
        }, 350, TimeUnit.MILLISECONDS);
    }

    private void showResults(List<IcdEntry> results) {
        currentResults = results;
        menu.getItems().clear();

        if (results.isEmpty()) {
            menu.hide();
            return;
        }

        for (IcdEntry entry : results) {
            MenuItem item = new MenuItem(entry.toString());
            item.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
            item.setOnAction(e -> selectEntry(entry));
            menu.getItems().add(item);
        }

        // ContextMenu se positionne automatiquement sous le TextArea
        if (!menu.isShowing()) {
            menu.show(textArea, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void selectEntry(IcdEntry entry) {
        textArea.setText(entry.description());
        menu.hide();
        if (onSelect != null) onSelect.accept(entry);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
