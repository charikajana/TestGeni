package automation.utils;

/**
 * Utility for analyzing selectors and identifying unstable patterns.
 */
public class SelectorUtil {

    /**
     * Detect dynamic patterns in IDs or Class names.
     * Examples: DemoQA hashes (50r6O), standard UUIDs, or long numeric sequences.
     */
    public static boolean isDynamic(String text) {
        if (text == null || text.isEmpty()) return false;
        
        // 1. DemoQA/React dynamic pattern: Mixed case + numbers, 5-10 chars
        if (text.length() >= 5 && text.length() <= 12) {
            boolean hasDigit = text.matches(".*\\d+.*");
            boolean hasUpper = text.matches(".*[A-Z]+.*");
            boolean hasLower = text.matches(".*[a-z]+.*");
            // If it has digits and letters, and it's not a common word
            if (hasDigit && (hasUpper || hasLower)) return true;
        }
        
        // 2. Pure numeric IDs are often dynamic/generated
        if (text.matches("^\\d{4,}$")) return true;
        
        // 3. Standard dynamic prefixes
        String lower = text.toLowerCase();
        if (lower.startsWith("u_0_") || lower.startsWith("id_") || lower.contains("announcer")) return true;
        
        // 4. UUID patterns
        if (text.matches(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}.*")) return true;
        
        return false;
    }

    /**
     * Check if a full selector is considered unstable
     */
    public static boolean isUnstableSelector(String selector, String strategy) {
        if (selector == null || selector.isEmpty()) return true;

        if ("id".equals(strategy) || selector.startsWith("#")) {
            return isDynamic(selector.replace("#", ""));
        }

        if (strategy.contains("css") || selector.contains(".")) {
            // Check for hash-based classes like .css-12345
            if (selector.matches(".*\\.(css|jss|Mui|atlaskit|r-)[a-zA-Z0-9_-]+.*")) {
                if (selector.matches(".*-[a-z0-9]{5,}.*")) return true;
            }
        }

        if ("xpath".equals(strategy)) {
            // Indexed XPaths are brittle
            if (selector.contains("[") && selector.matches(".*\\[\\d+\\].*")) return true;
        }

        return false;
    }
}
