package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import pro.revive.utils.UtilesMateriel.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AmbulanceFormController implements Initializable {

    @FXML private Label lblTitre;
    @FXML private TextField txtNumeroSerie, txtModele, txtAnnee;
    @FXML private TextField txtKmTotal, txtKmVidange, txtKmPneus;
    @FXML private ComboBox<String> cmbEtat, cmbMarque;
    @FXML private DatePicker dpVidange, dpPneus;
    @FXML private Button btnSave;

    private final AmbulanceService ambulanceService = new AmbulanceService();
    private Ambulance ambulance;
    private Runnable onSaveCallback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialiser les états
        cmbEtat.setItems(FXCollections.observableArrayList("Disponible", "En route", "En panne", "En maintenance"));
        cmbEtat.setValue("Disponible");

        // Initialiser les marques d'ambulances du monde entier
        cmbMarque.setItems(FXCollections.observableArrayList(
            // Marques Européennes
            "Mercedes-Benz",
            "Volkswagen",
            "Renault",
            "Peugeot",
            "Citroën",
            "Fiat",
            "Iveco",
            "Ford",
            "Opel",
            "Nissan",
            "Toyota",
            "Volvo",
            "MAN",
            "Scania",
            
            // Marques Américaines
            "Chevrolet",
            "GMC",
            "Dodge",
            "Ram",
            "International",
            "Freightliner",
            
            // Marques Asiatiques
            "Hyundai",
            "Kia",
            "Mitsubishi",
            "Isuzu",
            "Hino",
            "Mazda",
            
            // Marques Spécialisées Ambulances
            "Demers Ambulances",
            "Braun Ambulances",
            "Leader Ambulances",
            "Horton Emergency Vehicles",
            "Road Rescue",
            "AEV (American Emergency Vehicles)",
            "Excellance",
            "Osage Ambulance",
            "Marque Life Line",
            "WAS (Wietmarscher Ambulanz)",
            "Binz",
            "Miesen",
            "Ambulanz Mobile",
            
            // Autres
            "Autre"
        ));
        
        // Permettre la saisie personnalisée
        cmbMarque.setEditable(true);

        // ═══════════════════════════════════════════════════════════
        // CONTRÔLES DE SAISIE PROFESSIONNELS
        // ═══════════════════════════════════════════════════════════
        
        // Numéro de série : Format AMB-001-TN
        ValidationUtils.setNumeroSerieFormat(txtNumeroSerie);
        txtNumeroSerie.setPromptText("Format: AMB-001-TN");
        
        // Modèle : Lettres, chiffres et tirets
        ValidationUtils.setAlphanumericWithDash(txtModele);
        ValidationUtils.setMaxLength(txtModele, 50);
        
        // Année : 4 chiffres entre 1900 et 2100
        ValidationUtils.setYearFormat(txtAnnee);
        
        // Kilométrages : Nombres décimaux positifs
        ValidationUtils.setDecimalOnly(txtKmTotal);
        ValidationUtils.setDecimalOnly(txtKmVidange);
        ValidationUtils.setDecimalOnly(txtKmPneus);
        
        // Limiter les kilométrages à des valeurs raisonnables
        ValidationUtils.setMaxLength(txtKmTotal, 10);
        ValidationUtils.setMaxLength(txtKmVidange, 10);
        ValidationUtils.setMaxLength(txtKmPneus, 10);
        
        // Validation en temps réel pour les champs obligatoires
        txtNumeroSerie.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                validateNumeroSerie();
            }
        });
        
        cmbMarque.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                validateMarque();
            }
        });
    }

    public void setAmbulance(Ambulance ambulance) {
        this.ambulance = ambulance;
        if (ambulance != null) {
            lblTitre.setText("✏️  MODIFIER AMBULANCE");
            remplirFormulaire();
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void remplirFormulaire() {
        txtNumeroSerie.setText(ambulance.getNumeroSerie());
        cmbMarque.setValue(ambulance.getMarque());
        txtModele.setText(ambulance.getModele());
        if (ambulance.getAnneeFabrication() != null) {
            txtAnnee.setText(String.valueOf(ambulance.getAnneeFabrication()));
        }
        cmbEtat.setValue(ambulance.getEtat());
        txtKmTotal.setText(String.valueOf(ambulance.getKmTotal()));

        if (ambulance.getDateDerniereVidange() != null) {
            dpVidange.setValue(ambulance.getDateDerniereVidange());
        }
        if (ambulance.getKmDerniereVidange() != null) {
            txtKmVidange.setText(String.valueOf(ambulance.getKmDerniereVidange()));
        }
        if (ambulance.getDateDerniersPneus() != null) {
            dpPneus.setValue(ambulance.getDateDerniersPneus());
        }
        if (ambulance.getKmDerniersPneus() != null) {
            txtKmPneus.setText(String.valueOf(ambulance.getKmDerniersPneus()));
        }
    }

    @FXML
    private void onSave() {
        // Liste des erreurs de validation
        List<String> errors = new ArrayList<>();
        
        // ═══════════════════════════════════════════════════════════
        // VALIDATION COMPLÈTE
        // ═══════════════════════════════════════════════════════════
        
        // 1. Numéro de série (obligatoire, format AMB-001-TN)
        if (ValidationUtils.isEmpty(txtNumeroSerie)) {
            errors.add("• Le numéro de série est obligatoire");
            ValidationUtils.setErrorStyle(txtNumeroSerie);
        } else if (!ValidationUtils.isValidNumeroSerie(txtNumeroSerie.getText())) {
            errors.add("• Le numéro de série doit être au format AMB-001-TN");
            ValidationUtils.setErrorStyle(txtNumeroSerie);
        } else {
            ValidationUtils.clearErrorStyle(txtNumeroSerie);
        }
        
        // 2. Marque (obligatoire)
        String marque = cmbMarque.getValue();
        if (marque == null || marque.trim().isEmpty()) {
            errors.add("• La marque est obligatoire");
            cmbMarque.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
        } else {
            cmbMarque.setStyle("");
        }
        
        // 3. Année (optionnelle, mais si remplie doit être valide)
        if (!ValidationUtils.isEmpty(txtAnnee)) {
            if (!ValidationUtils.isInRange(txtAnnee.getText(), 1900, 2100)) {
                errors.add("• L'année doit être entre 1900 et 2100");
                ValidationUtils.setErrorStyle(txtAnnee);
            } else {
                ValidationUtils.clearErrorStyle(txtAnnee);
            }
        }
        
        // 4. Kilométrage total (doit être positif)
        if (!ValidationUtils.isEmpty(txtKmTotal)) {
            if (!ValidationUtils.isPositive(txtKmTotal.getText())) {
                errors.add("• Le kilométrage total doit être positif");
                ValidationUtils.setErrorStyle(txtKmTotal);
            } else {
                ValidationUtils.clearErrorStyle(txtKmTotal);
            }
        }
        
        // 5. Kilométrage vidange (doit être <= km total)
        if (!ValidationUtils.isEmpty(txtKmVidange) && !ValidationUtils.isEmpty(txtKmTotal)) {
            double kmVidange = Double.parseDouble(txtKmVidange.getText());
            double kmTotal = Double.parseDouble(txtKmTotal.getText());
            if (kmVidange > kmTotal) {
                errors.add("• Le km de la dernière vidange ne peut pas dépasser le km total");
                ValidationUtils.setErrorStyle(txtKmVidange);
            } else {
                ValidationUtils.clearErrorStyle(txtKmVidange);
            }
        }
        
        // 6. Kilométrage pneus (doit être <= km total)
        if (!ValidationUtils.isEmpty(txtKmPneus) && !ValidationUtils.isEmpty(txtKmTotal)) {
            double kmPneus = Double.parseDouble(txtKmPneus.getText());
            double kmTotal = Double.parseDouble(txtKmTotal.getText());
            if (kmPneus > kmTotal) {
                errors.add("• Le km des derniers pneus ne peut pas dépasser le km total");
                ValidationUtils.setErrorStyle(txtKmPneus);
            } else {
                ValidationUtils.clearErrorStyle(txtKmPneus);
            }
        }
        
        // 7. Date vidange (ne peut pas être dans le futur)
        if (dpVidange.getValue() != null && dpVidange.getValue().isAfter(LocalDate.now())) {
            errors.add("• La date de vidange ne peut pas être dans le futur");
            dpVidange.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
        } else {
            dpVidange.setStyle("");
        }
        
        // 8. Date pneus (ne peut pas être dans le futur)
        if (dpPneus.getValue() != null && dpPneus.getValue().isAfter(LocalDate.now())) {
            errors.add("• La date des pneus ne peut pas être dans le futur");
            dpPneus.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
        } else {
            dpPneus.setStyle("");
        }
        
        // Afficher les erreurs si présentes
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return;
        }

        // ═══════════════════════════════════════════════════════════
        // SAUVEGARDE
        // ═══════════════════════════════════════════════════════════
        
        try {
            if (ambulance == null) {
                ambulance = new Ambulance();
            }

            ambulance.setNumeroSerie(txtNumeroSerie.getText().trim().toUpperCase());
            ambulance.setMarque(marque.trim());
            ambulance.setModele(txtModele.getText().trim());
            
            if (!ValidationUtils.isEmpty(txtAnnee)) {
                ambulance.setAnneeFabrication(Integer.parseInt(txtAnnee.getText()));
            }
            
            ambulance.setEtat(cmbEtat.getValue());
            ambulance.setKmTotal(Double.parseDouble(txtKmTotal.getText().isEmpty() ? "0" : txtKmTotal.getText()));

            ambulance.setDateDerniereVidange(dpVidange.getValue());
            ambulance.setKmDerniereVidange(Double.parseDouble(txtKmVidange.getText().isEmpty() ? "0" : txtKmVidange.getText()));
            
            ambulance.setDateDerniersPneus(dpPneus.getValue());
            ambulance.setKmDerniersPneus(Double.parseDouble(txtKmPneus.getText().isEmpty() ? "0" : txtKmPneus.getText()));

            if (ambulance.getIdAmbulance() == null) {
                ambulanceService.create(ambulance);
            } else {
                ambulanceService.update(ambulance);
            }

            if (onSaveCallback != null) onSaveCallback.run();
            fermer();

        } catch (NumberFormatException e) {
            showError("Erreur de saisie", "Veuillez vérifier les valeurs numériques.");
        } catch (SQLException e) {
            showError("Erreur base de données", e.getMessage());
        }
    }
    
    /**
     * Valide le numéro de série
     */
    private boolean validateNumeroSerie() {
        if (ValidationUtils.isEmpty(txtNumeroSerie)) {
            return true; // Sera validé lors de la sauvegarde
        }
        if (!ValidationUtils.isValidNumeroSerie(txtNumeroSerie.getText())) {
            ValidationUtils.setErrorStyle(txtNumeroSerie);
            return false;
        }
        ValidationUtils.clearErrorStyle(txtNumeroSerie);
        return true;
    }
    
    /**
     * Valide la marque
     */
    private boolean validateMarque() {
        String marque = cmbMarque.getValue();
        if (marque == null || marque.trim().isEmpty()) {
            cmbMarque.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
            return false;
        }
        cmbMarque.setStyle("");
        return true;
    }
    
    /**
     * Affiche les erreurs de validation
     */
    private void showValidationErrors(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
        alert.setContentText(String.join("\n", errors));
        styleAlert(alert);
        alert.showAndWait();
    }
    
    /**
     * Applique le style dark à l'alerte
     */
    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm()
            );
        } catch (Exception ignored) {}
    }

    @FXML
    private void onCancel() {
        fermer();
    }

    private void fermer() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
