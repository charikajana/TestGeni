package automation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging utility for the NoCodeAutomation framework.
 * Provides consistent logging across all components with proper formatting.
 */
public class LoggerUtil {
    
    private final Logger logger;
    
    // Private constructor to force usage of static factory method
    private LoggerUtil(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Get a logger instance for a specific class
     */
    public static LoggerUtil getLogger(Class<?> clazz) {
        return new LoggerUtil(clazz);
    }
    
    // ========== INFO LEVEL ==========
    
    public void info(String message) {
        logger.info(message);
    }
    
    public void info(String format, Object... args) {
        logger.info(format, args);
    }
    
    /**
     * Log step execution
     */
    public void step(String message) {
        logger.info("STEP: {}", message);
    }
    
    public void step(String format, Object... args) {
        logger.info("STEP: " + format, args);
    }
    
    /**
     * Log success messages
     */
    public void success(String message) {
        logger.info("SUCCESS: {}", message);
    }
    
    public void success(String format, Object... args) {
        logger.info("SUCCESS: " + format, args);
    }
    
    /**
     * Log feature/scenario headers
     */
    public void header(String message) {
        logger.info("\n{}\n{}\n{}", 
            "==================================================",
            message,
            "==================================================");
    }
    
    /**
     * Log section separators
     */
    public void section(String message) {
        logger.info("\n--------------------------------------------------");
        logger.info(" {}", message);
        logger.info("--------------------------------------------------");
    }
    
    // ========== DEBUG LEVEL ==========
    
    public void debug(String message) {
        logger.debug(message);
    }
    
    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }
    
    /**
     * Log technical/analysis details
     */
    public void analysis(String message) {
        logger.debug("ANALYSIS: {}", message);
    }
    
    public void analysis(String format, Object... args) {
        logger.debug("ANALYSIS: " + format, args);
    }
    
    // ========== WARN LEVEL ==========
    
    public void warn(String message) {
        logger.warn(message);
    }
    
    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }
    
    /**
     * Log warnings
     */
    public void warning(String message) {
        logger.warn("WARNING: {}", message);
    }
    
    public void warning(String format, Object... args) {
        logger.warn("WARNING: " + format, args);
    }
    
    // ========== ERROR LEVEL ==========
    
    public void error(String message) {
        logger.error(message);
    }
    
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    public void error(String format, Object... args) {
        logger.error(format, args);
    }
    
    /**
     * Log failures
     */
    public void failure(String message) {
        logger.error("FAILURE: {}", message);
    }
    
    public void failure(String format, Object... args) {
        logger.error("FAILURE: " + format, args);
    }
    
    /**
     * Log validation results
     */
    public void validation(boolean success, String message) {
        if (success) {
            success(message);
        } else {
            failure(message);
        }
    }
    
    /**
     * Log action plan execution
     */
    public void actionPlan(String actionType, String target) {
        logger.info("MATCH: Pattern: {}", actionType);
        logger.debug("   Target: {}", target);
    }
    
    /**
     * Log browser actions
     */
    public void browserAction(String action, String target) {
        logger.info("{} -> {}", action, target);
    }
    
    /**
     * Log alert/dialog detection
     */
    public void alert(String type, String message) {
        logger.info("ALERT DETECTED");
        logger.info("   Type: {}", type);
        logger.info("   Message: {}", message);
    }
    
    /**
     * Log wait operations
     */
    public void waiting(int seconds) {
        logger.info("WAIT: Waiting for {} second(s)...", seconds);
    }
    
    /**
     * Log summary with statistics
     */
    public void summary(String title, int total, int passed, int failed, int skipped) {
        section(title);
        logger.info("  Total   : {}", total);
        logger.info("  Passed  : {}", passed);
        logger.info("  Failed  : {}", failed);
        if (skipped > 0) {
            logger.info("  Skipped : {}", skipped);
        }
        logger.info("--------------------------------------------------");
    }
}
