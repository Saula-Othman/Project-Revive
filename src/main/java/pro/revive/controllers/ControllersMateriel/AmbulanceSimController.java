package pro.revive.controllers.ControllersMateriel;

import pro.revive.utils.UtilesMateriel.NotificationService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.json.JSONArray;
import org.json.JSONObject;
import pro.revive.services.ServicesMateriel.EmailAlert;
import pro.revive.services.ServicesMateriel.EmailService;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.Trajet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AmbulanceSimController implements Initializable {

    @FXML private WebView webView;
    @FXML private TextField txtBase, txtUrgence;
    @FXML private javafx.scene.control.ComboBox<String> cmbAmbulances;
    @FXML private Label lblSimStatus, lblSimDetail, lblTime, lblDistance;
    @FXML private VBox boxStatus;
    @FXML private Button btnStart, btnAIDispatch;

    private WebEngine webEngine;
    private boolean mapLoaded = false;
    private double totalDuration = 0; // en secondes
    private double totalDistance = 0; // en metres
    private javafx.animation.Timeline missionTimeline;
    private int remainingSeconds = 0;
    private boolean aiDispatchEnabled = false;
    private final EmailService emailService = new EmailService();
    private final AmbulanceService ambulanceService = new AmbulanceService();
    private Thread emailPollingThread;
    private volatile boolean stopPolling = false;
    private Ambulance ambulanceSelectionnee = null;
    private Integer ambulanceASuivreId = null;  // ID de l'ambulance à suivre

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        webEngine = webView.getEngine();
        
        // Configurer le User-Agent et JavaScript
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        webEngine.setJavaScriptEnabled(true);
        
        // Définir un répertoire de cache pour le WebView
        try {
            java.nio.file.Path cacheDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "revive-webview-cache");
            java.nio.file.Files.createDirectories(cacheDir);
            webView.getEngine().setUserDataDirectory(cacheDir.toFile());
            System.out.println("[WebView] Cache directory: " + cacheDir);
        } catch (Exception e) {
            System.err.println("[WebView] Impossible de définir le cache: " + e.getMessage());
        }
        
        // Charger les ambulances disponibles
        chargerAmbulances();
        
        // Debug : Afficher les messages de la console JS dans Java
        webEngine.setOnError(event -> System.err.println("[JS Error] " + event.getMessage()));
        webEngine.setOnAlert(event -> System.out.println("[JS Alert] " + event.getData()));

        // Charger la carte avec Leaflet intégré (pas de dépendances externes CDN)
        chargerCarte();

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapLoaded = true;
                System.out.println("[Ambulance] Carte chargee avec succes.");

                // Injecter le connecteur Java pour recevoir les clics de la carte
                netscape.javascript.JSObject window = (netscape.javascript.JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());

                // Force Leaflet to recalculate the map container size.
                // JavaFX layout settles in stages, so we fire at several intervals.
                // The HTML page also has its own ResizeObserver + window resize listener.
                webEngine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 200);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 1000);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 2000);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 3500);"
                );
                
                // Si on a une ambulance à suivre, charger son dernier trajet
                if (ambulanceASuivreId != null) {
                    System.out.println("[Ambulance] Carte chargée, chargement du trajet pour ambulance ID: " + ambulanceASuivreId);
                    chargerDernierTrajetAmbulance();
                }
            } else if (newState == Worker.State.FAILED) {
                System.err.println("[Ambulance] ECHEC du chargement de la carte.");
            }
        });

        // When the WebView's layout dimensions change, force the map to re-invalidate.
        // This catches the common JavaFX case where the WebView gets its final size
        // *after* the HTML has already loaded.
        webView.widthProperty().addListener((o, ov, nv) -> {
            if (mapLoaded) {
                webEngine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 100);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);"
                );
            }
        });
        webView.heightProperty().addListener((o, ov, nv) -> {
            if (mapLoaded) {
                webEngine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 100);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);"
                );
            }
        });
    }

    /**
     * Charge la carte en intégrant Leaflet CSS/JS directement dans le HTML.
     * Évite les problèmes de chargement CDN et de chemins relatifs dans WebView.
     */
    private void chargerCarte() {
        try {
            // Lire le template HTML
            String html;
            try (InputStream is = getClass().getResourceAsStream("/ResourcesMateriel/module5/html/map.html")) {
                if (is == null) {
                    System.err.println("[Carte] ERREUR: map.html introuvable");
                    return;
                }
                html = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
            }
            
            // Lire Leaflet CSS et l'inliner
            try (InputStream is = getClass().getResourceAsStream("/ResourcesMateriel/module5/html/lib/leaflet.css")) {
                if (is != null) {
                    String css = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    html = html.replace("<!-- LEAFET_CSS_REPLACED_BY_JAVA -->",
                            "<style>" + css + "</style>");
                }
            }
            
            // Lire Leaflet JS et l'inliner
            try (InputStream is = getClass().getResourceAsStream("/ResourcesMateriel/module5/html/lib/leaflet.js")) {
                if (is != null) {
                    String js = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    // Échapper </script> dans le code pour éviter une fermeture prématurée
                    js = js.replace("</script>", "<\\/script>");
                    html = html.replace("<!-- LEAFLET_JS_REPLACED_BY_JAVA -->",
                            "<script>" + js + "</script>");
                }
            }
            
            System.out.println("[Carte] HTML chargé, taille: " + html.length() + " caractères");
            webEngine.loadContent(html);
            
        } catch (Exception e) {
            System.err.println("[Carte] ERREUR chargement: " + e.getMessage());
            e.printStackTrace();
            // Fallback: essayer le chargement direct
            URL mapUrl = getClass().getResource("/ResourcesMateriel/module5/html/map.html");
            if (mapUrl != null) {
                webEngine.load(mapUrl.toExternalForm());
            }
        }
    }

    // Méthode pour définir l'ambulance à suivre (appelée depuis la liste)
    public void setAmbulanceASuivre(Integer ambulanceId) {
        this.ambulanceASuivreId = ambulanceId;
        System.out.println("[Simulation] Ambulance à suivre: " + ambulanceId);
        
        // Si la carte est déjà chargée, charger le trajet immédiatement
        if (mapLoaded) {
            chargerDernierTrajetAmbulance();
        }
    }

    private void chargerDernierTrajetAmbulance() {
        if (ambulanceASuivreId == null) {
            System.out.println("[Suivi] Aucune ambulance à suivre définie");
            return;
        }
        
        new Thread(() -> {
            try {
                // Récupérer l'ambulance
                Ambulance amb = ambulanceService.findById(ambulanceASuivreId);
                if (amb == null) {
                    System.err.println("[Suivi] Ambulance non trouvée: " + ambulanceASuivreId);
                    Platform.runLater(() -> {
                        updateStatus("Erreur", "Ambulance non trouvée");
                    });
                    return;
                }
                
                // Récupérer le dernier trajet de cette ambulance
                List<Trajet> trajets = ambulanceService.getTrajetsAmbulance(ambulanceASuivreId);
                if (trajets.isEmpty()) {
                    System.out.println("[Suivi] Aucun trajet trouvé pour " + amb.getNumeroSerie());
                    Platform.runLater(() -> {
                        updateStatus("Suivi", "Aucun trajet enregistré pour " + amb.getNumeroSerie());
                    });
                    return;
                }
                
                // Prendre le dernier trajet
                Trajet dernierTrajet = trajets.get(0); // Le plus récent
                System.out.println("[Suivi] Dernier trajet trouvé: " + dernierTrajet.getLocalisationUrgence() + 
                                 " (" + dernierTrajet.getDistanceKm() + " km, " + dernierTrajet.getDureeMinutes() + " min)");
                System.out.println("[Suivi] Date du trajet: " + dernierTrajet.getDateTrajet());
                
                // Calculer le temps écoulé depuis le début du trajet
                java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
                java.time.LocalDateTime dateTrajet = dernierTrajet.getDateTrajet();
                long secondesEcoulees = java.time.Duration.between(dateTrajet, maintenant).getSeconds();
                
                // Calculer le temps restant
                int dureeTotaleSecondes = dernierTrajet.getDureeMinutes() * 60;
                int tempsRestantSecondes = (int)(dureeTotaleSecondes - secondesEcoulees);
                
                System.out.println("[Suivi] Durée totale: " + dernierTrajet.getDureeMinutes() + " min");
                
                // Si le temps est déjà écoulé, la mission est terminée
                if (tempsRestantSecondes <= 0) {
                    System.out.println("[Suivi] ⚠️ Mission déjà terminée (temps écoulé: " + secondesEcoulees + "s)");
                    Platform.runLater(() -> {
                        updateStatus("Mission Terminée", 
                            "Cette mission est déjà terminée. L'ambulance devrait être disponible.");
                        
                        // Forcer la finalisation si elle n'a pas été faite
                        new Thread(() -> {
                            try {
                                ambulanceService.changerStatutTrajet(dernierTrajet.getIdTrajet(), "Terminé");
                                ambulanceService.changerEtat(ambulanceASuivreId, "Disponible");
                                System.out.println("[Suivi] ✅ Mission finalisée automatiquement");
                            } catch (Exception e) {
                                System.err.println("[Suivi] Erreur finalisation: " + e.getMessage());
                            }
                        }).start();
                    });
                    return;
                }
                
                System.out.println("[Suivi] Temps écoulé: " + secondesEcoulees + "s, Temps restant: " + tempsRestantSecondes + "s");
                
                // Mettre à jour l'interface
                Platform.runLater(() -> {
                    // Remplir les champs avec le dernier trajet
                    txtBase.setText("Clinique Hannibal, Tunis");
                    txtUrgence.setText(dernierTrajet.getLocalisationUrgence());
                    
                    // Rendre la box de statut visible
                    boxStatus.setVisible(true);
                    
                    // Mettre à jour les labels de distance et temps RESTANT
                    double distanceKm = dernierTrajet.getDistanceKm();
                    lblDistance.setText(String.format("%.1f km", distanceKm));
                    
                    // Définir le temps restant (pas le temps total)
                    remainingSeconds = tempsRestantSecondes;
                    int mins = remainingSeconds / 60;
                    int secs = remainingSeconds % 60;
                    lblTime.setText(String.format("%02d:%02d", mins, secs));
                    
                    // Démarrer le chronomètre à rebours avec le temps restant
                    startMissionChrono();
                    
                    // Afficher un message avec le temps restant
                    updateStatus("🚑 En Mission", 
                        "Suivi de " + amb.getNumeroSerie() + 
                        " vers " + dernierTrajet.getLocalisationUrgence() + 
                        " - Temps restant: " + mins + ":" + String.format("%02d", secs));
                    
                    // Désactiver le ComboBox et le bouton (on est en mode suivi)
                    cmbAmbulances.setDisable(true);
                    btnStart.setDisable(true);
                    
                    // Attendre un peu que la carte soit bien initialisée
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000); // Attendre 2 secondes
                            Platform.runLater(() -> {
                                System.out.println("[Suivi] Affichage du trajet après délai...");
                                afficherTrajetSurCarte(dernierTrajet);
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });
                
            } catch (Exception e) {
                System.err.println("[Suivi] Erreur chargement trajet: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    updateStatus("Erreur", "Impossible de charger le trajet: " + e.getMessage());
                });
            }
        }).start();
    }

    private void afficherTrajetSurCarte(Trajet trajet) {
        if (!mapLoaded) {
            System.out.println("[Suivi] Carte pas encore chargée, attente...");
            // Attendre que la carte soit chargée
            new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) { // Essayer pendant 10 secondes
                        Thread.sleep(1000);
                        if (mapLoaded) {
                            System.out.println("[Suivi] Carte chargée après " + (i+1) + " secondes");
                            Platform.runLater(() -> afficherTrajetSurCarte(trajet));
                            return;
                        }
                    }
                    System.err.println("[Suivi] Timeout: Carte toujours pas chargée après 10 secondes");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            return;
        }
        
        // Géocoder dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // Préparer les coordonnées
                String base = "Clinique Hannibal, Tunis";
                String urgence = trajet.getLocalisationUrgence();
                
                System.out.println("[Suivi] Géocodage des adresses...");
                System.out.println("[Suivi] Base: " + base);
                System.out.println("[Suivi] Urgence: " + urgence);
                
                // Géocoder les adresses pour obtenir les vraies coordonnées
                double[] baseCoord = geocode(base);
                double[] urgCoord = geocode(urgence);
                
                if (baseCoord == null) {
                    System.err.println("[Suivi] Impossible de géocoder la base: " + base);
                    // Utiliser des coordonnées par défaut pour la Clinique Hannibal
                    baseCoord = new double[]{36.8065, 10.1815};
                    System.out.println("[Suivi] Utilisation coordonnées par défaut pour base: " + baseCoord[0] + ", " + baseCoord[1]);
                }
                
                if (urgCoord == null) {
                    System.err.println("[Suivi] Impossible de géocoder l'urgence: " + urgence);
                    // Utiliser des coordonnées par défaut près de la base
                    urgCoord = new double[]{36.8000, 10.1900};
                    System.out.println("[Suivi] Utilisation coordonnées par défaut pour urgence: " + urgCoord[0] + ", " + urgCoord[1]);
                }
                
                System.out.println("[Suivi] Coordonnées obtenues:");
                System.out.println("[Suivi]   Base: " + baseCoord[0] + ", " + baseCoord[1]);
                System.out.println("[Suivi]   Urgence: " + urgCoord[0] + ", " + urgCoord[1]);
                
                // Préparer les coordonnées finales
                final double baseLat = baseCoord[0];
                final double baseLon = baseCoord[1];
                final double urgLat = urgCoord[0];
                final double urgLon = urgCoord[1];
                
                // Appeler la fonction JavaScript sur le thread JavaFX
                Platform.runLater(() -> {
                    try {
                        String script = String.format(java.util.Locale.US,
                            "if (typeof afficherTrajetExistant === 'function') { " +
                            "  console.log('[JS] Appel afficherTrajetExistant avec coordonnées'); " +
                            "  afficherTrajetExistant(%f, %f, %f, %f, '%s', '%s', %f, %d); " +
                            "} else { " +
                            "  console.error('[JS] Fonction afficherTrajetExistant non disponible'); " +
                            "}",
                            baseLat, baseLon, urgLat, urgLon,
                            base.replace("'", "\\'"), urgence.replace("'", "\\'"), 
                            trajet.getDistanceKm(), trajet.getDureeMinutes()
                        );
                        
                        System.out.println("[Suivi] Exécution script JS...");
                        Object result = webEngine.executeScript(script);
                        System.out.println("[Suivi] Résultat JS: " + result);
                        System.out.println("[Suivi] Trajet affiché sur la carte: " + urgence);
                    } catch (Exception e) {
                        System.err.println("[Suivi] Erreur exécution JS: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
            } catch (Exception e) {
                System.err.println("[Suivi] Erreur affichage trajet: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void chargerAmbulances() {
        try {
            List<Ambulance> ambulances = ambulanceService.findAll();
            cmbAmbulances.getItems().clear();
            
            // Filtrer pour n'afficher que les ambulances disponibles
            List<Ambulance> ambulancesDisponibles = new ArrayList<>();
            for (Ambulance amb : ambulances) {
                if ("Disponible".equals(amb.getEtat())) {
                    ambulancesDisponibles.add(amb);
                    String item = amb.getNumeroSerie() + " - " + amb.getMarque() + " " + 
                                 (amb.getModele() != null ? amb.getModele() : "");
                    cmbAmbulances.getItems().add(item);
                }
            }
            
            // Sélectionner la première ambulance disponible par défaut
            if (!ambulancesDisponibles.isEmpty()) {
                cmbAmbulances.getSelectionModel().select(0);
                ambulanceSelectionnee = ambulancesDisponibles.get(0);
                System.out.println("[Ambulance] Sélectionnée par défaut: " + ambulanceSelectionnee.getNumeroSerie());
            } else {
                System.out.println("[Ambulance] ⚠️ Aucune ambulance disponible !");
                updateStatus("Attention", "Aucune ambulance disponible pour le moment.");
            }
            
            // Listener pour la sélection
            cmbAmbulances.getSelectionModel().selectedIndexProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && newVal.intValue() >= 0 && newVal.intValue() < ambulancesDisponibles.size()) {
                    ambulanceSelectionnee = ambulancesDisponibles.get(newVal.intValue());
                    System.out.println("[Ambulance] Sélectionnée: " + ambulanceSelectionnee.getNumeroSerie());
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur chargement ambulances: " + e.getMessage());
        }
    }

    @FXML
    private void onStartSimulation() {
        // Vérifier qu'une ambulance est sélectionnée
        if (ambulanceSelectionnee == null) {
            updateStatus("Erreur", "Veuillez sélectionner une ambulance avant de démarrer.");
            return;
        }
        
        // Vérifier que l'ambulance est disponible
        if (!"Disponible".equals(ambulanceSelectionnee.getEtat())) {
            updateStatus("Erreur", "L'ambulance sélectionnée n'est pas disponible (État: " + ambulanceSelectionnee.getEtat() + ")");
            return;
        }
        
        String baseAddr = "Clinique Hannibal, Tunis";
        String urgAddr = txtUrgence.getText();

        if (urgAddr == null || urgAddr.isEmpty()) {
            updateStatus("Erreur", "Veuillez saisir ou recevoir une localisation d'urgence.");
            return;
        }
        
        // Changer l'état de l'ambulance à "En route"
        try {
            ambulanceService.changerEtat(ambulanceSelectionnee.getIdAmbulance(), "En route");
            ambulanceSelectionnee.setEtat("En route");
            System.out.println("[Ambulance] État changé à 'En route' pour " + ambulanceSelectionnee.getNumeroSerie());
            
            // Rafraîchir la liste des ambulances
            chargerAmbulances();
            
            // Désactiver la sélection pendant la mission
            cmbAmbulances.setDisable(true);
        } catch (Exception e) {
            updateStatus("Erreur", "Impossible de changer l'état de l'ambulance: " + e.getMessage());
            return;
        }

        boxStatus.setVisible(true);
        updateStatus("Calcul...", "Recuperation des coordonnees GPS...");
        btnStart.setDisable(true);

        new Thread(() -> {
            try {
                // 1. Geocodage
                double[] baseCoord = geocode(baseAddr);
                double[] urgCoord = geocode(urgAddr);

                if (baseCoord == null || urgCoord == null) {
                    Platform.runLater(() -> {
                        updateStatus("❌ Erreur", "Impossible de localiser les adresses.");
                        btnStart.setDisable(false);
                        cmbAmbulances.setDisable(false);
                    });
                    return;
                }

                Platform.runLater(() -> {
                    webEngine.executeScript(String.format(java.util.Locale.US, "setMarker('base', %f, %f, 'Clinique Hannibal (Base)', '🏠')", baseCoord[0], baseCoord[1]));
                    webEngine.executeScript(String.format(java.util.Locale.US, "setMarker('urgence', %f, %f, 'Patient (Urgence)', '🚨')", urgCoord[0], urgCoord[1]));
                });

                // 2. Routage (Base -> Urgence seulement, pas de retour)
                updateStatus("Itineraire", "Calcul du trajet vers l'urgence...");
                totalDuration = 0;
                totalDistance = 0;
                
                List<double[]> fullRoute = new ArrayList<>();
                fullRoute.addAll(getRoute(baseCoord, urgCoord));
                // NE PAS ajouter le retour - l'ambulance reviendra automatiquement après la mission

                if (fullRoute.isEmpty()) {
                    Platform.runLater(() -> {
                        updateStatus("Erreur", "Impossible de calculer l'itineraire.");
                        btnStart.setDisable(false);
                    });
                    return;
                }

                // 3. Lancer l'animation
                Platform.runLater(() -> {
                    double km = Math.round((totalDistance / 1000.0) * 10.0) / 10.0;
                    lblDistance.setText(km + " km");
                    
                    remainingSeconds = (int) totalDuration;
                    startMissionChrono();
                    
                    updateStatus("En route", "L'ambulance a quitte la base.");
                    
                    // Convertir la route en format JS
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < fullRoute.size(); i++) {
                        sb.append(String.format(java.util.Locale.US, "[%f, %f]", fullRoute.get(i)[0], fullRoute.get(i)[1]));
                        if (i < fullRoute.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    
                    webEngine.executeScript("drawRoute(" + sb.toString() + ")");
                    webEngine.executeScript("animateAmbulance(" + sb.toString() + ", " + (int)totalDuration + ")");
                    
                    // NE PAS enregistrer le trajet maintenant - il sera enregistré à la fin de la mission
                    // L'enregistrement se fera dans finaliserMission() quand le chronomètre atteint 0
                    
                    btnStart.setDisable(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Message d'erreur spécifique pour localisation hors Tunisie
                    String message = e.getMessage();
                    if (message != null && message.contains("Tunisie")) {
                        updateStatus("❌ Localisation Invalide", message);
                    } else {
                        updateStatus("❌ Erreur", "Échec de la simulation : " + message);
                    }
                    stopMissionChrono();
                    btnStart.setDisable(false);
                    cmbAmbulances.setDisable(false);
                    
                    // Remettre l'ambulance à disponible en cas d'erreur
                    if (ambulanceSelectionnee != null) {
                        try {
                            ambulanceService.changerEtat(ambulanceSelectionnee.getIdAmbulance(), "Disponible");
                            ambulanceSelectionnee.setEtat("Disponible");
                            chargerAmbulances();
                        } catch (Exception ex) {
                            System.err.println("[Erreur] Impossible de remettre l'ambulance disponible: " + ex.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    private void startMissionChrono() {
        stopMissionChrono(); // Sécurité
        missionTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    lblTime.setText("ARRIVÉ");
                    updateStatus("✅ Mission Terminée", "L'ambulance est arrivée à destination.");
                    stopMissionChrono();
                    // Son de succès
                    NotificationService.playSuccessSound();
                    // Finaliser la mission dans un thread séparé
                    finaliserMission();
                } else {
                    int mins = remainingSeconds / 60;
                    int secs = remainingSeconds % 60;
                    lblTime.setText(String.format("%02d:%02d", mins, secs));
                }
            })
        );
        missionTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        missionTimeline.play();
    }
    
    /**
     * Finalise la mission : enregistre le trajet, remet l'ambulance à "Disponible" et marque le trajet comme "Terminé"
     */
    private void finaliserMission() {
        new Thread(() -> {
            try {
                // Si on est en mode suivi (ambulance en mission)
                if (ambulanceASuivreId != null) {
                    // Marquer le dernier trajet comme terminé
                    List<Trajet> trajets = ambulanceService.getTrajetsAmbulance(ambulanceASuivreId);
                    if (!trajets.isEmpty()) {
                        Trajet dernierTrajet = trajets.get(0);
                        if ("En cours".equals(dernierTrajet.getStatut())) {
                            ambulanceService.changerStatutTrajet(dernierTrajet.getIdTrajet(), "Terminé");
                            System.out.println("[Mission] ✅ Trajet ID " + dernierTrajet.getIdTrajet() + " marqué comme 'Terminé'");
                        }
                    }
                    
                    // Remettre l'ambulance à "Disponible"
                    ambulanceService.changerEtat(ambulanceASuivreId, "Disponible");
                    System.out.println("[Mission] ✅ Ambulance ID " + ambulanceASuivreId + " remise à 'Disponible'");
                    
                    // Réactiver les contrôles
                    Platform.runLater(() -> {
                        cmbAmbulances.setDisable(false);
                        btnStart.setDisable(false);
                        ambulanceASuivreId = null; // Réinitialiser le mode suivi
                        chargerAmbulances();
                    });
                }
                // Si on est en mode simulation normale
                else if (ambulanceSelectionnee != null) {
                    // Enregistrer le trajet maintenant (à la fin de la mission)
                    String depart = txtBase.getText();
                    String urgence = txtUrgence.getText();
                    double distanceKm = totalDistance / 1000.0;
                    int dureeMin = (int)(totalDuration / 60);
                    
                    System.out.println("[Mission] Enregistrement du trajet : " + distanceKm + " km, " + dureeMin + " min");
                    
                    // Créer le trajet avec statut "Terminé" (car la mission est finie)
                    Trajet trajet = new Trajet(ambulanceSelectionnee.getIdAmbulance(), depart, urgence, distanceKm, dureeMin);
                    trajet.setStatut("Terminé");
                    ambulanceService.enregistrerTrajet(trajet);
                    
                    System.out.println("[Mission] ✅ Trajet enregistré");
                    
                    // Remettre l'ambulance à "Disponible"
                    ambulanceService.changerEtat(ambulanceSelectionnee.getIdAmbulance(), "Disponible");
                    ambulanceSelectionnee.setEtat("Disponible");
                    System.out.println("[Mission] ✅ Ambulance " + ambulanceSelectionnee.getNumeroSerie() + " remise à 'Disponible'");
                    
                    // Réactiver les contrôles
                    Platform.runLater(() -> {
                        cmbAmbulances.setDisable(false);
                        btnStart.setDisable(false);
                        chargerAmbulances();
                    });
                }
            } catch (Exception ex) {
                System.err.println("[Mission] ❌ Erreur finalisation: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void stopMissionChrono() {
        if (missionTimeline != null) {
            missionTimeline.stop();
        }
    }

    // Connecteur pour recevoir les clics depuis JavaScript
    public class JavaConnector {
        public void onMapClick(double lat, double lon) {
            Platform.runLater(() -> {
                try {
                    updateStatus("Smart Map", "Recherche de l'adresse...");
                    String address = reverseGeocode(lat, lon);
                    txtUrgence.setText(address);
                    webEngine.executeScript(String.format(java.util.Locale.US, "setMarker('urgence', %f, %f, 'Urgence', '🚨')", lat, lon));
                    updateStatus("Smart Map", "Urgence définie. Prêt à démarrer.");
                } catch (Exception e) {
                    System.err.println("Reverse geocode error: " + e.getMessage());
                }
            });
        }
    }

    private String reverseGeocode(double lat, double lon) throws Exception {
        String urlStr = String.format(java.util.Locale.US, "https://nominatim.openstreetmap.org/reverse?lat=%f&lon=%f&format=json", lat, lon);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "ReviveApp/1.0");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            
            JSONObject obj = new JSONObject(response.toString());
            return obj.optString("display_name", "Lieu inconnu");
        }
    }

    private double[] geocode(String address) throws Exception {
        // Si c'est déjà des coordonnées (format "lat,lon")
        if (address.matches("^-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?$")) {
            String[] parts = address.split(",");
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            
            // Vérifier si les coordonnées sont en Tunisie
            if (!estEnTunisie(lat, lon)) {
                throw new Exception("Les coordonnées ne sont pas en Tunisie");
            }
            
            return new double[]{lat, lon};
        }

        String query = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String urlStr = "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1";
        
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "ReviveApp/1.0");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            
            JSONArray arr = new JSONArray(response.toString());
            if (arr.length() > 0) {
                JSONObject obj = arr.getJSONObject(0);
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                
                // Vérifier si la localisation est en Tunisie
                if (!estEnTunisie(lat, lon)) {
                    throw new Exception("La localisation '" + address + "' n'est pas en Tunisie");
                }
                
                return new double[]{lat, lon};
            }
        }
        return null;
    }
    
    /**
     * Vérifie si les coordonnées GPS sont en Tunisie
     * Tunisie : Latitude 30°N à 37.5°N, Longitude 7.5°E à 11.6°E
     */
    private boolean estEnTunisie(double lat, double lon) {
        // Limites géographiques de la Tunisie
        final double LAT_MIN = 30.0;    // Sud (frontière Libye)
        final double LAT_MAX = 37.5;    // Nord (Cap Bon)
        final double LON_MIN = 7.5;     // Ouest (frontière Algérie)
        final double LON_MAX = 11.6;    // Est (Mer Méditerranée)
        
        boolean dansLimites = lat >= LAT_MIN && lat <= LAT_MAX && lon >= LON_MIN && lon <= LON_MAX;
        
        if (!dansLimites) {
            System.out.println("[Validation] ❌ Coordonnées hors Tunisie: " + lat + ", " + lon);
        } else {
            System.out.println("[Validation] ✅ Coordonnées en Tunisie: " + lat + ", " + lon);
        }
        
        return dansLimites;
    }

    private List<double[]> getRoute(double[] start, double[] end) throws Exception {
        String urlStr = String.format(java.util.Locale.US, "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                start[1], start[0], end[1], end[0]); // OSRM use lon,lat
        
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        List<double[]> points = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            
            JSONObject json = new JSONObject(response.toString());
            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject routeObj = routes.getJSONObject(0);
                totalDuration += routeObj.getDouble("duration");
                totalDistance += routeObj.getDouble("distance");
                
                JSONArray coords = routeObj.getJSONObject("geometry").getJSONArray("coordinates");
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    points.add(new double[]{c.getDouble(1), c.getDouble(0)}); // Back to lat,lon
                }
            }
        }
        return points;
    }

    private void updateStatus(String title, String detail) {
        Platform.runLater(() -> {
            boxStatus.setVisible(true);
            lblSimStatus.setText(title);
            lblSimDetail.setText(detail);
        });
    }

    /**
     * Affiche la popup de dispatch avec les infos de l'alerte email.
     * Si accepté → lance la simulation. Si refusé → met à jour le statut.
     */
    private void showDispatchDialog(EmailAlert alert) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ResourcesMateriel/module5/view/DispatchAlert.fxml")
            );
            Parent root = loader.load();

            DispatchAlertController controller = loader.getController();
            controller.setAlert(alert, accepted -> {
                if (accepted) {
                    txtUrgence.setText(alert.getLocation());
                    updateStatus("🚨 Mission acceptée", "Lancement de la simulation...");
                    onStartSimulation();
                } else {
                    updateStatus("❌ Mission refusée", "En attente du prochain email...");
                }
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setTitle("Alerte Urgence");

            Scene scene = new Scene(root);
            java.net.URL cssUrl = getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();

        } catch (Exception e) {
            System.err.println("[DispatchDialog] Erreur ouverture popup : " + e.getMessage());
            e.printStackTrace();
            // Fallback : lancer directement sans popup
            txtUrgence.setText(alert.getLocation());
            onStartSimulation();
        }
    }

    @FXML
    private void onClear() {
        if (mapLoaded) {
            webEngine.executeScript("clearMap()");
            boxStatus.setVisible(false);
            stopMissionChrono();
            lblTime.setText("00:00");
            lblDistance.setText("0 km");
            txtUrgence.clear();
        }
    }

    @FXML
    private void toggleAIDispatch() {
        boxStatus.setVisible(true);
        if (!aiDispatchEnabled) {
            aiDispatchEnabled = true;
            btnAIDispatch.setText("🛑 Désactiver IA");
            btnAIDispatch.setStyle("-fx-background-color: #ef4444; -fx-padding: 12;");

            new Thread(() -> {
                // ── Étape 1 : Test de connexion Gmail ──────────────────
                updateStatus("📡 Connexion...", "Tentative de connexion à Gmail...");
                try {
                    emailService.testConnection();
                    updateStatus("✅ Gmail connecté", "Connexion réussie. Démarrage du polling...");
                    Thread.sleep(1500);
                } catch (Exception e) {                    Platform.runLater(() -> {
                        updateStatus("❌ Connexion échouée", "Gmail inaccessible : " + e.getMessage());
                        aiDispatchEnabled = false;
                        btnAIDispatch.setText("🤖 Activer Dispatch IA");
                        btnAIDispatch.setStyle("-fx-background-color: #8b5cf6; -fx-padding: 12;");
                    });
                    return;
                }

                // ── Étape 2 : Polling ──────────────────────────────────
                int pollCount = 0;
                while (aiDispatchEnabled) {
                    try {
                        pollCount++;
                        final int count = pollCount;
                        updateStatus("📬 Vérification #" + count, "Lecture des nouveaux emails...");

                        EmailAlert emailAlert = emailService.fetchLatestEmergencyAlert();

                        if (emailAlert != null) {
                            // Mail avec localisation trouvé
                            System.out.println("[AI Dispatch] ✅ Alerte reçue : " + emailAlert);
                            updateStatus("🚨 Mail reçu !", "De : " + emailAlert.getSenderEmail());
                            // Son d'alerte
                            NotificationService.playAlertSound();
                            Thread.sleep(500);
                            Platform.runLater(() -> showDispatchDialog(emailAlert));
                            Thread.sleep(30000); // Pause après alerte
                        } else {
                            // Afficher le vrai statut depuis EmailService
                            String detail = emailService.getLastStatus();
                            updateStatus("👁 IA Active — #" + count, detail + " | Prochaine vérif dans 10s...");
                            Thread.sleep(10000);
                        }

                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[AI Dispatch] Erreur : " + e.getMessage());
                        updateStatus("⚠️ Erreur réseau", e.getMessage().length() > 60
                                ? e.getMessage().substring(0, 60) + "..." : e.getMessage());
                        try { Thread.sleep(15000); } catch (InterruptedException ie2) { break; }
                    }
                }

                Platform.runLater(() -> updateStatus("IA Off", "Mode manuel."));

            }).start();

        } else {
            aiDispatchEnabled = false;
            btnAIDispatch.setText("🤖 Activer Dispatch IA");
            btnAIDispatch.setStyle("-fx-background-color: #8b5cf6; -fx-padding: 12;");
            updateStatus("IA Off", "Mode manuel.");
        }
    }

    @FXML private void onVoirDashboard()  { naviguerVers("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",         "Tableau de Bord"); }
    @FXML private void onVoirSalles()     { naviguerVers("/ResourcesMateriel/module5/view/SalleList.fxml",          "Salles Physiques"); }
    @FXML private void onVoirMateriel()   { naviguerVers("/ResourcesMateriel/module5/view/MaterielList.fxml",       "Materiel d'Urgence"); }
    @FXML private void onVoirAmbulances() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceList.fxml",      "Ambulances"); }
    @FXML private void onVoirHistorique() { naviguerVers("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml", "Historique Missions"); }
    @FXML private void onVoirRecherche()  { naviguerVers("/ResourcesMateriel/module5/view/RechercheGlobale.fxml",   "Recherche Globale"); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) webView.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void naviguerVers(String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) webView.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("REVIVE — " + titre);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
