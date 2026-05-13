package pro.revive.utils.TriageUtils;

/**
 * VoiceOutputService — Text-to-Speech for Visual Assistance.
 *
 * Uses Windows built-in speech engine (System.Speech) via PowerShell.
 * No external library or Maven dependency required.
 *
 * - speak(text) : speaks the given text immediately.
 *   If a previous speech is still running, it is interrupted first.
 * - stop()      : stops any ongoing speech.
 */
public class VoiceOutputService {

    private static Process currentProcess = null;
    private static final Object LOCK = new Object();
    private static volatile boolean muted = false;

    /**
     * Speaks the given text aloud.
     * Interrupts any currently running speech before starting.
     * Strips emojis and special characters that TTS engines cannot pronounce.
     *
     * @param text The analysis text to speak.
     */
    /** Toggle mute on/off. Returns the new muted state. */
    public static boolean toggleMute() {
        muted = !muted;
        if (muted) stop();
        return muted;
    }

    public static boolean isMuted() { return muted; }

    public static void speak(String text) {
        if (muted) return;
        if (text == null || text.isBlank()) return;

        String cleaned = clean(text);
        if (cleaned.isBlank()) return;

        // Escape single quotes for PowerShell string
        String escaped = cleaned.replace("'", " ");

        AppExecutor.run(() -> {
            synchronized (LOCK) {
                // Kill previous speech if still running
                stop();

                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.SelectVoice('Microsoft Hortense Desktop'); " +
                        "$s.Rate = 1; " +
                        "$s.Speak('" + escaped + "');"
                    );
                    pb.redirectErrorStream(true);
                    currentProcess = pb.start();
                    currentProcess.waitFor();
                } catch (Exception e) {
                    System.out.println("[VoiceOutputService] TTS error: " + e.getMessage());
                } finally {
                    currentProcess = null;
                }
            }
        });
    }

    /**
     * Stops any currently playing speech immediately.
     */
    public static void stop() {
        synchronized (LOCK) {
            if (currentProcess != null && currentProcess.isAlive()) {
                currentProcess.destroyForcibly();
                currentProcess = null;
            }
        }
    }

    /**
     * Removes emojis, symbols, and non-speakable characters from the text.
     * Keeps letters, digits, spaces, punctuation, and French accented characters.
     */
    private static String clean(String text) {
        if (text == null) return "";
        // Remove emoji and non-Latin/non-basic symbols
        String result = text
            .replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{P}]", " ") // keep letters, digits, spaces, punctuation
            .replaceAll("\\s{2,}", " ")                        // collapse multiple spaces
            .trim();
        return result;
    }
}
