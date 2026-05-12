package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.GeminiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AnalyserResultatController {

    @FXML private Label      lblInfoResultat;
    @FXML private VBox       containerMessages;
    @FXML private TextArea   taQuestion;
    @FXML private ScrollPane scrollChat;

    // ── Nouveaux éléments UI
    @FXML private Label      lblModeIA;        // indicateur 🟢/🟡
    @FXML private TextField  tfCleAPI;         // champ clé Gemini
    @FXML private VBox       panneauCleAPI;    // panneau config clé
    @FXML private Label      lblStatutEnvoi;   // "En cours…"

    private Resultats resultat;
    private String    contextePatient = "";

    // ── Historique de conversation (pour la mémoire de Gemini)
    private final List<String[]> historique = new ArrayList<>(); // [question, reponse]
    private static final int MAX_HISTORIQUE = 6; // 6 échanges max en contexte

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        resultat = GestionResultatController.getResultatSelectionne();

        // ── Construire le contexte patient
        if (resultat != null) {
            String patient = nomPatient();
            contextePatient = construireContexte();

            lblInfoResultat.setText("Patient : " + patient +
                    "  |  État : " + (resultat.getEtat() != null ? resultat.getEtat() : "—") +
                    "  |  ID : #" + resultat.getIdResultat());
        } else {
            lblInfoResultat.setText("Aucun résultat sélectionné");
        }

        // ── Charger la clé API sauvegardée
        if (tfCleAPI != null) {
            tfCleAPI.setText(GeminiService.getCleApi());
        }

        // ── Masquer le panneau clé si déjà configurée
        if (panneauCleAPI != null) {
            boolean clePresente = GeminiService.hasCleApi();
            panneauCleAPI.setVisible(!clePresente);
            panneauCleAPI.setManaged(!clePresente);
        }

        // ── Vérifier le mode (internet + clé) dans un thread séparé
        verifierModeEtDemarrer();

        // ── Envoi avec Entrée (Shift+Entrée pour nouvelle ligne)
        taQuestion.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                handleEnvoyer();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION MODE ET DÉMARRAGE
    // ─────────────────────────────────────────────────────────────────────────

    private void verifierModeEtDemarrer() {
        Thread t = new Thread(() -> {
            boolean internet = GeminiService.isInternetAvailable();
            boolean hasCle   = GeminiService.hasCleApi();

            Platform.runLater(() -> {
                mettreAJourIndicateurMode(internet, hasCle);
                afficherMessageBienvenue(internet, hasCle);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void mettreAJourIndicateurMode(boolean internet, boolean hasCle) {
        if (lblModeIA == null) return;
        if (internet && hasCle) {
            lblModeIA.setText("🟢  Mode IA avancée (Gemini)");
            lblModeIA.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else if (!internet) {
            lblModeIA.setText("🟡  Mode hors ligne");
            lblModeIA.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lblModeIA.setText("🔑  Clé API manquante");
            lblModeIA.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void afficherMessageBienvenue(boolean internet, boolean hasCle) {
        String patient = resultat != null ? nomPatient() : null;

        if (resultat == null) {
            ajouterMessageAssistant(
                "Bonjour ! Je suis votre assistant médical IA. 🤖\n\n" +
                "Aucun résultat n'a été sélectionné.\n" +
                "Veuillez retourner à la liste et sélectionner un résultat.", false);
            return;
        }

        String modeInfo;
        if (internet && hasCle) {
            modeInfo = "🟢 Mode IA avancée activé — Je peux répondre à toutes vos questions médicales.";
        } else if (!internet) {
            modeInfo = "🟡 Mode hors ligne — Réponses basiques disponibles. Connectez-vous pour l'IA avancée.";
        } else {
            modeInfo = "🔑 Clé API manquante — Entrez votre clé Gemini pour activer l'IA avancée.";
        }

        ajouterMessageAssistant(
            "Bonjour ! Je suis votre assistant médical IA. 🤖\n\n" +
            modeInfo + "\n\n" +
            "📋 Résultat chargé :\n" +
            "• Patient : " + patient + "\n" +
            "• État : " + (resultat.getEtat() != null ? resultat.getEtat() : "—") + "\n" +
            "• Compte rendu : « " + (resultat.getCompteRendu() != null
                    ? tronquer(resultat.getCompteRendu(), 120) : "—") + " »\n\n" +
            "Posez-moi vos questions. Je peux vous aider à :\n" +
            "• Interpréter le compte rendu\n" +
            "• Évaluer la gravité et les risques\n" +
            "• Suggérer des examens complémentaires\n" +
            "• Expliquer les termes médicaux\n" +
            "• Proposer des recommandations cliniques\n\n" +
            "💡 Astuce : Appuyez sur Entrée pour envoyer, Shift+Entrée pour une nouvelle ligne.", false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOI DE QUESTION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleEnvoyer() {
        String question = taQuestion.getText().trim();
        if (question.isEmpty()) return;

        ajouterMessageUtilisateur(question);
        taQuestion.clear();
        taQuestion.setDisable(true);

        // Indicateur "en cours"
        if (lblStatutEnvoi != null) {
            lblStatutEnvoi.setText("⏳  Analyse en cours…");
            lblStatutEnvoi.setVisible(true);
        }

        // Traitement dans un thread séparé
        Thread thread = new Thread(() -> {
            String reponse = genererReponse(question);

            // Ajouter à l'historique
            historique.add(new String[]{question, reponse});
            if (historique.size() > MAX_HISTORIQUE) {
                historique.remove(0);
            }

            Platform.runLater(() -> {
                ajouterMessageAssistant(reponse, true);
                taQuestion.setDisable(false);
                taQuestion.requestFocus();
                if (lblStatutEnvoi != null) lblStatutEnvoi.setVisible(false);
                scrollChat.setVvalue(1.0);
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRATION DE RÉPONSE (hybride Gemini / local)
    // ─────────────────────────────────────────────────────────────────────────

    private String genererReponse(String question) {
        // ── Mode Gemini si internet + clé disponibles
        if (GeminiService.isInternetAvailable() && GeminiService.hasCleApi()) {
            String historiqueTexte = construireHistoriqueTexte();
            String reponse = GeminiService.poserQuestion(question, contextePatient, historiqueTexte);

            // Mettre à jour l'indicateur
            Platform.runLater(() -> mettreAJourIndicateurMode(true, true));
            return reponse;
        }

        // ── Mode hors ligne : fallback local amélioré
        Platform.runLater(() -> mettreAJourIndicateurMode(
                GeminiService.isInternetAvailable(), GeminiService.hasCleApi()));
        return genererReponseFallback(question);
    }

    /** Construit le contexte médical complet pour Gemini */
    private String construireContexte() {
        if (resultat == null) return "Aucun résultat disponible.";
        // Run the bio analysis with etat so Grave is always flagged
        pro.revive.services.ServicesLabo.AnalyseBiologiqueService.ResultatAnalyse bio =
            pro.revive.services.ServicesLabo.AnalyseBiologiqueService
                .analyserAvecEtat(resultat.getCompteRendu(), resultat.getEtat());
        return "Patient : " + nomPatient() + "\n" +
               "ID Résultat : #" + resultat.getIdResultat() + "\n" +
               "ID Demande : #" + resultat.getIdDemande() + "\n" +
               "État du résultat : " + (resultat.getEtat() != null ? resultat.getEtat() : "Non défini") + "\n" +
               "Niveau de gravité détecté : " + bio.niveauAttention + "\n" +
               "Compte rendu médical : " + (resultat.getCompteRendu() != null
                       ? resultat.getCompteRendu() : "Non disponible") + "\n" +
               "Fichier joint : " + (resultat.getFichierJoint() != null
                       && !resultat.getFichierJoint().isBlank()
                       ? resultat.getFichierJoint() : "Aucun") + "\n" +
               "Date du résultat : " + (resultat.getDateResultat() != null
                       ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                               .format(resultat.getDateResultat()) : "—");
    }

    /** Construit l'historique de conversation pour Gemini */
    private String construireHistoriqueTexte() {
        if (historique.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String[] echange : historique) {
            sb.append("Utilisateur : ").append(echange[0]).append("\n");
            sb.append("Assistant : ").append(tronquer(echange[1], 300)).append("\n\n");
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FALLBACK LOCAL AMÉLIORÉ (hors ligne)
    // ─────────────────────────────────────────────────────────────────────────

    private String genererReponseFallback(String question) {
        if (resultat == null) return "Aucun résultat sélectionné.";

        String cr    = resultat.getCompteRendu() != null ? resultat.getCompteRendu().toLowerCase() : "";
        String etat  = resultat.getEtat() != null ? resultat.getEtat() : "Propre";
        String q     = question.toLowerCase();
        boolean grave = "Grave".equalsIgnoreCase(etat);

        // ── Salutations
        if (q.matches(".*(bonjour|salut|hello|bonsoir).*")) {
            return "Bonjour ! 👋\n\nJe suis en mode hors ligne. Je peux répondre à vos questions " +
                   "sur le résultat de " + nomPatient() + " avec mes connaissances locales.\n\n" +
                   "État actuel : " + (grave ? "🔴 GRAVE" : "🟢 PROPRE");
        }

        // ── État / normalité
        if (q.matches(".*(normal|propre|bon|bien|correct|ok).*")) {
            return grave
                ? "🔴 Ce résultat est classifié comme GRAVE (anormal).\n\n" +
                  "Compte rendu : « " + resultat.getCompteRendu() + " »\n\n" +
                  "⚠️ Ce résultat nécessite une attention médicale immédiate."
                : "✅ Ce résultat est classifié comme PROPRE (normal).\n\n" +
                  "Compte rendu : « " + resultat.getCompteRendu() + " »\n\n" +
                  "Les valeurs semblent dans les normes attendues.";
        }

        // ── Gravité / urgence
        if (q.matches(".*(grave|critique|urgent|danger|sérieux|sérieux|risque|alerte).*")) {
            return grave
                ? "🔴 RÉSULTAT GRAVE — Actions recommandées :\n\n" +
                  "• 🚨 Informer immédiatement le médecin traitant\n" +
                  "• 👁 Surveillance rapprochée du patient\n" +
                  "• 🔬 Envisager des examens complémentaires urgents\n" +
                  "• 📋 Documenter l'évolution clinique\n\n" +
                  "Compte rendu : « " + resultat.getCompteRendu() + " »"
                : "🟢 Ce résultat n'est pas critique.\n\n" +
                  "Aucune urgence détectée. Le suivi habituel est suffisant.";
        }

        // ── Interprétation
        if (q.matches(".*(interpréter|signifie|expliquer|comprendre|veut dire|sens|signification).*")) {
            return "📋 Interprétation du compte rendu :\n\n" +
                   "« " + resultat.getCompteRendu() + " »\n\n" +
                   "État classifié : " + etat + "\n\n" +
                   (cr.contains("normal") || cr.contains("propre") || cr.contains("négatif") || cr.contains("absence")
                       ? "✅ Les termes utilisés suggèrent des résultats dans les limites normales."
                       : grave
                           ? "⚠️ Les termes indiquent une anomalie nécessitant une attention médicale."
                           : "ℹ️ Les résultats doivent être interprétés en contexte clinique.") +
                   "\n\n💡 Connectez-vous à internet pour une interprétation plus détaillée par l'IA Gemini.";
        }

        // ── Examens complémentaires
        if (q.matches(".*(examen|complément|suite|bilan|contrôle|suivi|prochaine).*")) {
            return "🔬 Examens complémentaires suggérés :\n\n" +
                   (grave
                       ? "• Bilan biologique complet\n• Imagerie médicale de contrôle\n" +
                         "• Consultation spécialisée urgente\n• Surveillance rapprochée (24-48h)"
                       : "• Contrôle de routine dans 3-6 mois\n• Maintien du suivi habituel\n" +
                         "• Examens préventifs selon protocole") +
                   "\n\n⚕️ Ces suggestions sont indicatives. Le médecin traitant décide du plan de suivi.";
        }

        // ── Traitement / médicaments
        if (q.matches(".*(traitement|médicament|thérapie|prescription|ordonnance|dose).*")) {
            return "💊 Note importante :\n\n" +
                   "La prescription de traitements relève exclusivement du médecin traitant.\n\n" +
                   "Ce que je peux confirmer :\n" +
                   "• État du résultat : " + etat + "\n" +
                   "• Compte rendu : « " + tronquer(resultat.getCompteRendu(), 100) + " »\n\n" +
                   "Consultez le médecin responsable pour toute décision thérapeutique.";
        }

        // ── Patient
        if (q.matches(".*(patient|nom|qui|identité|personne).*")) {
            return "👤 Informations du patient :\n\n" +
                   "• Nom : " + nomPatient() + "\n" +
                   "• ID Résultat : #" + resultat.getIdResultat() + "\n" +
                   "• ID Demande : #" + resultat.getIdDemande() + "\n" +
                   "• État : " + etat + "\n" +
                   "• Date : " + (resultat.getDateResultat() != null
                           ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                                   .format(resultat.getDateResultat()) : "—");
        }

        // ── Recommandations
        if (q.matches(".*(recommandation|conseil|que faire|action|conduite|protocole).*")) {
            return "📌 Recommandations cliniques :\n\n" +
                   (grave
                       ? "🔴 Résultat GRAVE :\n" +
                         "1. Alerter le médecin traitant immédiatement\n" +
                         "2. Mettre le patient sous surveillance\n" +
                         "3. Préparer un bilan complémentaire\n" +
                         "4. Documenter dans le dossier médical"
                       : "🟢 Résultat PROPRE :\n" +
                         "1. Informer le patient du résultat normal\n" +
                         "2. Planifier un suivi de routine\n" +
                         "3. Archiver le résultat dans le dossier\n" +
                         "4. Aucune action urgente requise");
        }

        // ── Réponse générique
        return "🤖 Analyse du résultat (mode hors ligne) :\n\n" +
               "Patient : " + nomPatient() + "\n" +
               "État : " + (grave ? "🔴 GRAVE" : "🟢 PROPRE") + "\n" +
               "Compte rendu : « " + tronquer(resultat.getCompteRendu(), 150) + " »\n\n" +
               (grave
                   ? "⚠️ Ce résultat nécessite une attention médicale. Informez le médecin traitant."
                   : "✅ Ce résultat semble dans les normes. Suivi de routine recommandé.") +
               "\n\n💡 Connectez-vous à internet et configurez votre clé Gemini pour des réponses " +
               "plus précises et contextuelles.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURATION CLÉ API
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSauvegarderCle() {
        if (tfCleAPI == null) return;
        String cle = tfCleAPI.getText().trim();
        if (cle.isBlank()) {
            ajouterMessageAssistant("⚠️ La clé API ne peut pas être vide.", false);
            return;
        }
        if (!cle.startsWith("AIza")) {
            ajouterMessageAssistant("⚠️ La clé API Gemini doit commencer par 'AIza'.\n\n" +
                    "Obtenez votre clé sur : aistudio.google.com", false);
            return;
        }

        GeminiService.setCleApi(cle);

        // Masquer le panneau après sauvegarde
        if (panneauCleAPI != null) {
            panneauCleAPI.setVisible(false);
            panneauCleAPI.setManaged(false);
        }

        ajouterMessageAssistant("✅ Clé API sauvegardée !\n\nVérification de la connexion…", false);

        // Revérifier le mode
        Thread t = new Thread(() -> {
            boolean internet = GeminiService.isInternetAvailable();
            Platform.runLater(() -> {
                mettreAJourIndicateurMode(internet, true);
                if (internet) {
                    ajouterMessageAssistant("🟢 Mode IA avancée activé !\n\n" +
                            "Je suis maintenant connecté à Gemini. " +
                            "Posez-moi vos questions pour des réponses intelligentes.", false);
                } else {
                    ajouterMessageAssistant("🟡 Clé sauvegardée mais pas de connexion internet.\n\n" +
                            "L'IA avancée sera disponible dès que vous serez connecté.", false);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EFFACER LA CONVERSATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleEffacer() {
        containerMessages.getChildren().clear();
        historique.clear();
        if (resultat != null) {
            ajouterMessageAssistant("🧹 Conversation effacée.\n\n" +
                    "Comment puis-je vous aider avec le résultat de " + nomPatient() + " ?", false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULLES DE CHAT
    // ─────────────────────────────────────────────────────────────────────────

    private void ajouterMessageUtilisateur(String texte) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setPadding(new Insets(4, 0, 4, 80));

        VBox bulle = new VBox(4);
        Label msg = new Label(texte);
        msg.setWrapText(true);
        msg.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1E40AF, #2563EB);" +
                "-fx-text-fill: white; -fx-background-radius: 16 16 4 16;" +
                "-fx-padding: 10 14; -fx-font-size: 13px; -fx-max-width: 420;"
        );

        Label heure = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        heure.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 9px;");
        heure.setAlignment(Pos.CENTER_RIGHT);

        bulle.getChildren().addAll(msg, heure);
        bulle.setAlignment(Pos.CENTER_RIGHT);
        wrapper.getChildren().add(bulle);
        containerMessages.getChildren().add(wrapper);
    }

    private void ajouterMessageAssistant(String texte, boolean avecAnimation) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(4, 80, 4, 0));

        // Avatar IA
        StackPane avatar = new StackPane();
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #7C3AED, #5B21B6);" +
                "-fx-background-radius: 18;");
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 16px;");
        avatar.getChildren().add(avatarIcon);
        avatar.setAlignment(Pos.TOP_CENTER);

        VBox bulle = new VBox(4);
        Label msg = new Label(texte);
        msg.setWrapText(true);
        msg.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1E293B;" +
                "-fx-background-radius: 16 16 16 4; -fx-padding: 10 14;" +
                "-fx-font-size: 13px; -fx-max-width: 440;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);"
        );

        Label heure = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        heure.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 9px;");

        bulle.getChildren().addAll(msg, heure);
        wrapper.getChildren().addAll(avatar, bulle);
        containerMessages.getChildren().add(wrapper);

        // Animation fade-in
        if (avecAnimation) {
            msg.setOpacity(0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), msg);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }

        // Scroll automatique
        Platform.runLater(() -> scrollChat.setVvalue(1.0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private String nomPatient() {
        if (resultat == null) return "Inconnu";
        return (resultat.getNomPatient() != null && !resultat.getNomPatient().isBlank())
                ? resultat.getNomPatient() : "Demande #" + resultat.getIdDemande();
    }

    private String tronquer(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Effets hover
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) { btn.setScaleX(1.15); btn.setScaleY(1.15); }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) { btn.setScaleX(1.0); btn.setScaleY(1.0); }
    }
}
