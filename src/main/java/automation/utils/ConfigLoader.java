package automation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Config loader for application properties
 */
public class ConfigLoader {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(ConfigLoader.class);
    private static Properties appConfig;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        appConfig = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("config/app.properties")) {
            if (input != null) {
                appConfig.load(input);
                logger.info("Loaded app configuration with {} properties", appConfig.size());
            } else {
                logger.warning("app.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load app.properties: {}", e.getMessage());
        }
    }
    
    /**
     * Get application URL by name
     * @param appName Application name (e.g., "HotelBooker")
     * @return URL or null if not found
     */
    public static String getAppUrl(String appName) {
        // Try exact match first
        String key = appName.toLowerCase().replace(" ", ".") + ".url";
        String url = appConfig.getProperty(key);
        
        if (url == null) {
            // Try fuzzy match by searching property keys
            for (String propKey : appConfig.stringPropertyNames()) {
                if (propKey.endsWith(".name")) {
                    String nameValue = appConfig.getProperty(propKey);
                    if (nameValue.equalsIgnoreCase(appName)) {
                        // Found matching name, get corresponding URL
                        String urlKey = propKey.replace(".name", ".url");
                        url = appConfig.getProperty(urlKey);
                        break;
                    }
                }
            }
        }
        
        if (url != null) {
            logger.info("Resolved app '{}' to URL: {}", appName, url);
        } else {
            logger.warning("No URL found for app: {}", appName);
        }
        
        return url;
    }
    
    /**
     * Get test credentials
     */
    public static String getTestUsername() {
        return appConfig.getProperty("test.username", "");
    }
    
    public static String getTestPassword() {
        return appConfig.getProperty("test.password", "");
    }
    
    /**
     * Get credentials for a specific application (Phase 2)
     * @param appName Application name (e.g., "hotel.booker", "sabre.admin")
     * @return Map with "username" and "password" keys
     */
    public static java.util.Map<String, String> getCredentials(String appName) {
        String normalizedApp = appName.toLowerCase().replace(" ", ".");
        String username = appConfig.getProperty(normalizedApp + ".username");
        String password = appConfig.getProperty(normalizedApp + ".password");
        
        java.util.Map<String, String> creds = new java.util.HashMap<>();
        
        if (username != null && password != null) {
            creds.put("username", username);
            creds.put("password", password);
            logger.info("Retrieved credentials for: {}", appName);
        } else {
            // Fallback to test credentials
            logger.warning("No specific credentials for {}, using test defaults", appName);
            creds.put("username", getTestUsername());
            creds.put("password", getTestPassword());
        }
        
        return creds;
    }
    
    /**
     * Get form section data (Phase 2)
     * @param sectionName Section name (e.g., "booking.contact", "traveller")
     * @return Map of field names to values
     */
    public static java.util.Map<String, String> getFormSectionData(String sectionName) {
        String normalizedSection = sectionName.toLowerCase().replace(" ", ".");
        java.util.Map<String, String> formData = new java.util.HashMap<>();
        
        // Find all properties that start with this section name
        for (String key : appConfig.stringPropertyNames()) {
            if (key.startsWith(normalizedSection + ".")) {
                String fieldName = key.substring(normalizedSection.length() + 1);
                String value = appConfig.getProperty(key);
                formData.put(fieldName, value);
            }
        }
        
        if (!formData.isEmpty()) {
            logger.info("Retrieved {} fields for section: {}", formData.size(), sectionName);
        } else {
            logger.warning("No form data found for section: {}", sectionName);
        }
        
        return formData;
    }
    
    /**
     * Get any property by key
     */
    public static String getProperty(String key) {
        return appConfig.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return appConfig.getProperty(key, defaultValue);
    }
}
