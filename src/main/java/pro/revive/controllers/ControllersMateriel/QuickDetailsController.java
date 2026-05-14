package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.services.ServicesMateriel.MaterielService;
import pro.revive.services.ServicesMateriel.SalleService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * Gestionnaire pour le popup de details premium avec interaction materiel.
 */
public class QuickDetailsController {

    @FXML private Label lblHeaderIcon, lblTitle, lblSubtitle, lblStatutBadge;
    @FXML private Label lblDetail1Label, lblDetail1Value, lblDetail2Label, lblDetail2Value;
    @FXML private Label lblExtraValue;
    @FXML private ProgressBar progressCapacity;
    @FXML private HBox boxWarning;
    @FXML private Label lblWarning;
    @FXML private VBox vboxEquipment;
    @FXML private VBox cardExtra;
    @FXML private Button btnDelete;
    @FXML private Button btnEdit;

    private Object currentItem;
    private java.util.function.Consumer<Object> onEditRequested;
    private Runnable onCloseRequested;
    private final SalleService salleService = new SalleService();
    private final MaterielService materielService = new MaterielService();
    
    private boolean dataChanged = false;
    private boolean editRequested = false;

    public void setOnCloseRequested(Runnable callback) { this.onCloseRequested = callback; }
    public void setOnEditRequested(java.util.function.Consumer<Object> callback) { this.onEditRequested = callback; }

    public void setItem(Object item) {
        this.currentItem = item;
        if (item instanceof SallePhysique) {
            setupSalle((SallePhysique) item);
        } else if (item instanceof MaterielUrgence) {
            setupMateriel((MaterielUrgence) item);
        }
    }

    private void setupSalle(SallePhysique s) {
        lblHeaderIcon.setText("🏠");
        lblTitle.setText(s.getNom());
        lblSubtitle.setText(s.getType().toUpperCase());

        lblStatutBadge.setText(s.getStatut().toUpperCase());
        updateBadgeStyle(s.getStatut());

        lblDetail1Label.setText("OCCUPATION");
        lblDetail1Value.setText(s.getNombreActuel() + " / " + s.getCapaciteMax() + " patients");

        if (s.getCapaciteMax() > 0) {
            double prog = (double) s.getNombreActuel() / s.getCapaciteMax();
            progressCapacity.setProgress(prog);
            String accent = prog >= 0.9 ? "#EF4444" : prog >= 0.7 ? "#F59E0B" : "#0B4EA2";
            progressCapacity.setStyle("-fx-accent: " + accent + ";");
        }

        lblDetail2Label.setText("LOCALISATION");
        lblDetail2Value.setText(s.getLocalisation() != null && !s.getLocalisation().isEmpty()
            ? s.getLocalisation() : "Non spécifiée");

        chargerEquipements(s.getIdSalle());

        boolean hasPatients = s.getNombreActuel() > 0;
        if (hasPatients) {
            btnDelete.setDisable(true);
            btnDelete.setOpacity(0.4);
            boxWarning.setVisible(true);
            boxWarning.setManaged(true);
            lblWarning.setText("Sécurité : impossible de supprimer une salle occupée par des patients.");
        }
    }

    private void chargerEquipements(int idSalle) {
        vboxEquipment.getChildren().clear();
        try {
            List<MaterielUrgence> mats = materielService.findBySalle(idSalle);
            lblExtraValue.setText(String.valueOf(mats.size()));
            
            if (mats.isEmpty()) {
                Label empty = new Label("Aucun materiel assigne.");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                vboxEquipment.getChildren().add(empty);
            } else {
                for (MaterielUrgence m : mats) {
                    vboxEquipment.getChildren().add(creerLigneMateriel(m));
                }
            }
        } catch (SQLException e) {
            lblExtraValue.setText("!");
        }
    }

    private HBox creerLigneMateriel(MaterielUrgence m) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 10; -fx-border-color: #f1f5f9; -fx-border-radius: 10;");
        
        Label icon = new Label("🔧");
        icon.setStyle("-fx-font-size: 16px;");

        VBox info = new VBox(2);
        Label name = new Label(m.getNom());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
        
        // Badge de statut pour l'équipement
        Label badgeEtat = new Label(m.getEtat().toUpperCase());
        badgeEtat.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 4;");
        
        String etat = m.getEtat().toLowerCase();
        if (etat.contains("fonctionnel")) {
            badgeEtat.setStyle(badgeEtat.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
        } else if (etat.contains("reviser") || etat.contains("alerte")) {
            badgeEtat.setStyle(badgeEtat.getStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
        } else {
            badgeEtat.setStyle(badgeEtat.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }

        info.getChildren().addAll(name, badgeEtat);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnRetirer = new Button("✖");
        btnRetirer.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand;");
        btnRetirer.setOnMouseEntered(e -> btnRetirer.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-background-radius: 6;"));
        btnRetirer.setOnMouseExited(e -> btnRetirer.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px;"));
        btnRetirer.setOnAction(e -> retirerMateriel(m));

        row.getChildren().addAll(icon, info, btnRetirer);
        return row;
    }

    private void retirerMateriel(MaterielUrgence m) {
        try {
            m.setIdSalle(0); // 0 ou null selon ton service (ici on utilise 0 pour reserve)
            materielService.update(m);
            dataChanged = true;
            if (currentItem instanceof SallePhysique) {
                chargerEquipements(((SallePhysique) currentItem).getIdSalle());
            }
        } catch (SQLException e) {
            System.err.println("Erreur retrait : " + e.getMessage());
        }
    }

    private void setupMateriel(MaterielUrgence m) {
        lblHeaderIcon.setText("🔧");
        lblTitle.setText(m.getNom());
        lblSubtitle.setText("MATÉRIEL D'URGENCE");
        lblStatutBadge.setText(m.getEtat().toUpperCase());
        updateBadgeStyle(m.getEtat());

        lblDetail1Label.setText("QUANTITÉ");
        lblDetail1Value.setText(m.getQuantite() + " unité(s)");
        progressCapacity.setVisible(false);
        progressCapacity.setManaged(false);

        lblDetail2Label.setText("EMPLACEMENT");
        lblDetail2Value.setText(m.getNomSalle() != null && !m.getNomSalle().isEmpty()
            ? m.getNomSalle() : "Réserve Centrale");

        cardExtra.setVisible(false);
        cardExtra.setManaged(false);
        boxWarning.setVisible(false);
        boxWarning.setManaged(false);

        // Afficher la date de maintenance si disponible
        if (m.getDateDerniereMaintenance() != null) {
            lblDetail2Label.setText("DERNIÈRE MAINTENANCE");
            lblDetail2Value.setText(m.getDateDerniereMaintenance().toString());
        }
    }

    private void updateBadgeStyle(String status) {
        String s = status.toLowerCase();
        String bg, fg;
        if (s.contains("disponible") || s.contains("fonctionnel")) {
            bg = "#DCFCE7"; fg = "#15803D";
        } else if (s.contains("occupe") || s.contains("reviser") || s.contains("nettoyage")) {
            bg = "#FEF3C7"; fg = "#B45309";
        } else if (s.contains("maintenance") || s.contains("panne")) {
            bg = "#FEE2E2"; fg = "#DC2626";
        } else {
            bg = "#EEF4FB"; fg = "#0B4EA2";
        }
        lblStatutBadge.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 3 12;" +
            "-fx-background-radius: 20;"
        );
    }

    @FXML
    public void onEdit() {
        editRequested = true;
        if (onCloseRequested != null) onCloseRequested.run();
        if (onEditRequested != null) onEditRequested.accept(currentItem);
    }
    @FXML public void onClose() { close(); }

    @FXML
    public void onDelete() {
        try {
            if (currentItem instanceof SallePhysique) {
                salleService.delete(((SallePhysique) currentItem).getIdSalle());
            } else if (currentItem instanceof MaterielUrgence) {
                materielService.delete(((MaterielUrgence) currentItem).getIdMateriel());
            }
            dataChanged = true;
            close();
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    private void close() { 
        if (onCloseRequested != null) {
            onCloseRequested.run();
        }
    }

    public boolean isDataChanged() { return dataChanged; }
    public boolean isEditRequested() { return editRequested; }
}
