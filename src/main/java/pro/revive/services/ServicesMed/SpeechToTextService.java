package pro.revive.services.ServicesMed;

import javafx.application.Platform;

import java.io.*;
import java.util.function.Consumer;

/**
 * Service de dictée vocale utilisant l'API Windows Speech Recognition
 * via un script PowerShell — stable, sans dépendance native qui crash.
 *
 * Fallback : si PowerShell échoue, affiche un message d'erreur clair.
 */
public class SpeechToTextService {

    private volatile boolean running = false;
    private Process          psProcess;
    private Thread           readerThread;

    // ── API publique ──────────────────────────────────────────────────────

    /** Toujours disponible sur Windows — pas de lib native à charger. */
    public static boolean isVoskDisponible() {
        // On utilise Windows Speech — toujours "disponible" sur Windows
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    /**
     * Démarre la dictée vocale via un script PowerShell temporaire.
     */
    public void startListening(String language,
                               Consumer<String> onResult,
                               Consumer<String> onError) {
        if (running) return;

        String winLang = switch (language) {
            case "fr-FR" -> "fr-FR";
            case "ar-TN" -> "ar-SA";
            default      -> "fr-FR";
        };

        // Écrire le script dans un fichier temporaire pour éviter les problèmes d'échappement
        File scriptFile;
        try {
            scriptFile = File.createTempFile("revive_stt_", ".ps1");
            scriptFile.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(scriptFile), "UTF-8"))) {
                pw.println(buildPowerShellScript(winLang));
            }
        } catch (IOException e) {
            if (onError != null)
                Platform.runLater(() -> onError.accept("Impossible de créer le script : " + e.getMessage()));
            return;
        }

        running = true;
        final File finalScript = scriptFile;

        readerThread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-File", finalScript.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                psProcess = pb.start();

                System.out.println("[STT] Windows Speech démarré (langue: " + winLang + ")");

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(psProcess.getInputStream(), "UTF-8"));

                String line;
                while (running && (line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    System.out.println("[STT] << " + trimmed);
                    if (trimmed.startsWith("[ERR]")) {
                        final String err = trimmed.substring(5).trim();
                        if (onError != null)
                            Platform.runLater(() -> onError.accept(err));
                        break;
                    }
                    if (trimmed.startsWith("[TEXT]")) {
                        final String text = trimmed.substring(6).trim() + " ";
                        if (!text.isBlank())
                            Platform.runLater(() -> onResult.accept(text));
                    }
                }
            } catch (Exception e) {
                System.err.println("[STT] Erreur : " + e.getMessage());
                if (onError != null)
                    Platform.runLater(() -> onError.accept("Erreur : " + e.getMessage()));
            } finally {
                running = false;
                finalScript.delete();
            }
        }, "stt-reader-thread");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    /** Surcharge sans callback d'erreur. */
    public void startListening(String language, Consumer<String> onResult) {
        startListening(language, onResult, null);
    }

    /** Arrête la dictée et tue le processus PowerShell. */
    public void stopListening() {
        running = false;
        if (psProcess != null) {
            psProcess.destroyForcibly();
            psProcess = null;
        }
        System.out.println("[STT] Dictée arrêtée.");
    }

    public boolean isRunning() { return running; }

    // ── Script PowerShell ─────────────────────────────────────────────────

    /**
     * Génère un script PowerShell qui utilise System.Speech.Recognition
     * (intégré à Windows depuis Vista) pour reconnaître la voix en continu.
     * Chaque résultat est préfixé par [TEXT] pour être parsé côté Java.
     */
    private String buildPowerShellScript(String lang) {
        // Script multi-lignes — écrit dans un fichier .ps1 temporaire
        // Les $ sont des variables PowerShell, pas Java
        return
            "Add-Type -AssemblyName System.Speech\n" +
            "try {\n" +
            "    $culture = [System.Globalization.CultureInfo]::GetCultureInfo('" + lang + "')\n" +
            "    $rec = New-Object System.Speech.Recognition.SpeechRecognitionEngine($culture)\n" +
            "    $rec.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar))\n" +
            "    $rec.SetInputToDefaultAudioDevice()\n" +
            "    Register-ObjectEvent -InputObject $rec -EventName SpeechRecognized -Action {\n" +
            "        $txt = $Event.SourceEventArgs.Result.Text\n" +
            "        if ($txt) { [Console]::WriteLine('[TEXT]' + $txt) }\n" +
            "    } | Out-Null\n" +
            "    $rec.RecognizeAsync([System.Speech.Recognition.RecognizeMode]::Multiple)\n" +
            "    while ($true) { Start-Sleep -Milliseconds 300 }\n" +
            "} catch [System.Globalization.CultureNotFoundException] {\n" +
            "    [Console]::WriteLine('[ERR]Langue non installee sur Windows : " + lang + "')\n" +
            "} catch {\n" +
            "    [Console]::WriteLine('[ERR]' + $_.Exception.Message)\n" +
            "}\n";
    }
}
