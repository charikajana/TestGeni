package automation.context;

import automation.utils.LoggerUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Test execution context for storing and retrieving dynamic data during test execution.
 * 
 * Use cases:
 * - Store booking reference numbers
 * - Store generated IDs
 * - Store session tokens
 * - Store any runtime data that needs to be reused across steps
 * 
 * Thread-safe singleton implementation.
 */
public class TestContext {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(TestContext.class);
    private static TestContext instance;
    
    private Map<String, Object> contextData;
    private Map<String, String> bookingReferences;
    private Map<String, String> userSessions;
    
    private TestContext() {
        contextData = new HashMap<>();
        bookingReferences = new HashMap<>();
        userSessions = new HashMap<>();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
            logger.info("Test Context initialized");
        }
        return instance;
    }
    
    /**
     * Store generic data
     */
    public void set(String key, Object value) {
        contextData.put(key, value);
        logger.debug("Context: Stored {} = {}", key, value);
    }
    
    /**
     * Retrieve generic data
     */
    public Object get(String key) {
        return contextData.get(key);
    }
    
    /**
     * Retrieve generic data as String
     */
    public String getString(String key) {
        Object value = contextData.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Check if key exists
     */
    public boolean has(String key) {
        return contextData.containsKey(key);
    }
    
    // ========================================
    // BOOKING REFERENCE MANAGEMENT
    // ========================================
    
    /**
     * Store booking reference
     * @param type Type of booking (e.g., "hotel", "flight")
     * @param reference Reference number
     */
    public void setBookingReference(String type, String reference) {
        bookingReferences.put(type.toLowerCase(), reference);
        logger.info("Stored Booking Reference [{}]: {}", type, reference);
    }
    
    /**
     * Get booking reference
     */
    public String getBookingReference(String type) {
        return bookingReferences.get(type.toLowerCase());
    }
    
    /**
     * Get the most recent booking reference (any type)
     */
    public String getLastBookingReference() {
        if (bookingReferences.isEmpty()) {
            return null;
        }
        // Return the last added reference
        return bookingReferences.values().stream()
            .reduce((first, second) -> second)
            .orElse(null);
    }
    
    // ========================================
    // USER SESSION MANAGEMENT
    // ========================================
    
    /**
     * Store user session token
     */
    public void setUserSession(String username, String sessionToken) {
        userSessions.put(username, sessionToken);
        logger.debug("Stored session for user: {}", username);
    }
    
    /**
     * Get user session token
     */
    public String getUserSession(String username) {
        return userSessions.get(username);
    }
    
    // ========================================
    // CONTEXT LIFECYCLE
    // ========================================
    
    /**
     * Clear all context data (use between test scenarios)
     */
    public void clear() {
        contextData.clear();
        bookingReferences.clear();
        userSessions.clear();
        logger.info("Test Context cleared");
    }
    
    /**
     * Clear specific category
     */
    public void clearCategory(String category) {
        switch (category.toLowerCase()) {
            case "bookings":
                bookingReferences.clear();
                logger.info("Cleared booking references");
                break;
            case "sessions":
                userSessions.clear();
                logger.info("Cleared user sessions");
                break;
            case "data":
                contextData.clear();
                logger.info("Cleared context data");
                break;
            default:
                logger.warning("Unknown category: {}", category);
        }
    }
    
    /**
     * Get all context data (for debugging)
     */
    public Map<String, Object> getAll() {
        return new HashMap<>(contextData);
    }
    
    /**
     * Print context summary
     */
    public void printSummary() {
        logger.info("=== Test Context Summary ===");
        logger.info("Generic Data: {} entries", contextData.size());
        logger.info("Booking References: {} entries", bookingReferences.size());
        logger.info("User Sessions: {} entries", userSessions.size());
        
        if (!bookingReferences.isEmpty()) {
            logger.info("Booking References:");
            bookingReferences.forEach((type, ref) -> 
                logger.info("  [{}]: {}", type, ref));
        }
    }
}
