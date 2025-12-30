package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.ConfigLoader;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Map;

/**
 * Fills username and password from configuration.
 * Example: "user enters username and password"
 * 
 * Strategy:
 * 1. Get credentials from config (default or app-specific)
 * 2. Find username field
 * 3. Find password field
 * 4. Fill both fields
 */
public class FillCredentialsAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(FillCredentialsAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        logger.info("Fill Credentials -> Entering username and password from config");
        
        try {
            // Get credentials from config
            // Try to determine app context from current URL
            String currentUrl = page.url().toLowerCase();
            String appContext = determineAppContext(currentUrl);
            
            Map<String, String> credentials = ConfigLoader.getCredentials(appContext);
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            if (username == null || username.isEmpty()) {
                logger.error("No username found in configuration");
                return false;
            }
            
            logger.debug("Using credentials for context: {}", appContext);
            
            // Find and fill username field
            Locator usernameField = findUsernameField(smartLocator);
            if (usernameField == null) {
                logger.error("Could not find username field");
                return false;
            }
            
            usernameField.clear();
            usernameField.fill(username);
            logger.debug("Filled username field");
            
            // Find and fill password field
            Locator passwordField = findPasswordField(smartLocator);
            if (passwordField == null) {
                logger.error("Could not find password field");
                return false;
            }
            
            passwordField.clear();
            passwordField.fill(password);
            logger.debug("Filled password field");
            
            logger.success("SUCCESS: Credentials entered");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to fill credentials: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Determine app context from URL
     */
    private String determineAppContext(String url) {
        if (url.contains("hotel") || url.contains("booker")) {
            return "hotel.booker";
        } else if (url.contains("sabre") && url.contains("admin")) {
            return "sabre.admin";
        }
        return "test";  // Default to test credentials
    }
    
    /**
     * Find username field using multiple strategies
     */
    private Locator findUsernameField(SmartLocator smartLocator) {
        // Try common username field identifiers
        String[] usernameVariants = {
            "username", "user name", "email", "user id", "login", 
            "userid", "user", "account"
        };
        
        for (String variant : usernameVariants) {
            try {
                Locator field = smartLocator.findSmartElement(variant, "input", null, null);
                if (field != null && field.count() > 0) {
                    logger.debug("Found username field using: {}", variant);
                    return field;
                }
            } catch (Exception e) {
                // Try next variant
                continue;
            }
        }
        
        return null;
    }
    
    /**
     * Find password field using multiple strategies
     */
    private Locator findPasswordField(SmartLocator smartLocator) {
        // Try common password field identifiers
        String[] passwordVariants = {
            "password", "pass word", "pwd", "pass"
        };
        
        for (String variant : passwordVariants) {
            try {
                Locator field = smartLocator.findSmartElement(variant, "input", null, null);
                if (field != null && field.count() > 0) {
                    logger.debug("Found password field using: {}", variant);
                    return field;
                }
            } catch (Exception e) {
                // Try next variant
                continue;
            }
        }
        
        return null;
    }
}
