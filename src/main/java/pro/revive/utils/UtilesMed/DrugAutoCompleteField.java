package pro.revive.utils.UtilesMed;

import pro.revive.services.ServicesMed.DrugSearchService;
import pro.revive.services.ServicesMed.DrugSearchService.DrugEntry;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Feature 2 — Autocomplete medicaments via RxNorm.
 * Utilise ContextMenu (auto-positionne sous le champ).
 * Debounce 350ms, appel API en background.
 */
public class DrugAutoCompleteField {

    private final TextField           field;
    private final DrugSearchService   service  = new DrugSearchService();
    private final ContextMenu         menu     = new ContextMenu();
    private final Consumer<DrugEntry> onSelect;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "drug-search");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> pendingTask;

    public DrugAutoCompleteField(TextField field, Consumer<DrugEntry> onSelect) {
        this.field    = field;
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
        field.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().length() < 2) {
                menu.hide();
                return;
            }
            scheduleSearch(n.trim());
        });

        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) menu.hide();
        });
    }

    private void scheduleSearch(String query) {
        if (pendingTask != null && !pendingTask.isDone()) pendingTask.cancel(false);
        pendingTask = scheduler.schedule(() -> {
            List<DrugEntry> results = service.search(query);
            Platform.runLater(() -> showResults(results));
        }, 350, TimeUnit.MILLISECONDS);
    }

    private void showResults(List<DrugEntry> results) {
        menu.getItems().clear();

        if (results.isEmpty()) {
            menu.hide();
            return;
        }

        for (DrugEntry entry : results) {
            MenuItem item = new MenuItem(entry.toString());
            item.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
            item.setOnAction(e -> selectEntry(entry));
            menu.getItems().add(item);
        }

        if (!menu.isShowing()) {
            menu.show(field, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void selectEntry(DrugEntry entry) {
        field.setText(entry.name());
        menu.hide();
        if (onSelect != null) onSelect.accept(entry);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
