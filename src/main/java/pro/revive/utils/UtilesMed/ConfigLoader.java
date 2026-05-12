package pro.revive.utils.UtilesMed;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton pour charger config.properties une seule fois au demarrage.
 * Utilise par tous les services qui ont besoin de configuration.
 */
public class ConfigLoader {

    private static ConfigLoader instance;
    private final Properties properties;

    private ConfigLoader() {
        properties = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("ResourcesMed/config.properties")) {
            if (is != null) {
                properties.load(is);
                System.out.println("[ConfigLoader] config.properties charge avec succes.");
            } else {
                System.err.println("[ConfigLoader] config.properties introuvable.");
            }
        } catch (IOException e) {
            System.err.println("[ConfigLoader] Erreur de chargement : " + e.getMessage());
        }
    }

    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    /**
     * Recupere une valeur de configuration.
     * @param key Cle de configuration
     * @return Valeur ou chaine vide si absente
     */
    public String get(String key) {
        return properties.getProperty(key, "").trim();
    }

    /**
     * Recupere une valeur avec une valeur par defaut.
     */
    public String get(String key, String defaultValue) {
        String val = properties.getProperty(key, "").trim();
        return val.isEmpty() ? defaultValue : val;
    }
}
