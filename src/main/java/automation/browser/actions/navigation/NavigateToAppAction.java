package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.ConfigLoader;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Navigates to an application using its name from configuration.
 * Example: "Open Browser and Navigate to HotelBooker"
 * Reads the URL from config/app.properties
 */
public class NavigateToAppAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(NavigateToAppAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String appName = plan.getElementName();
        
        logger.info("Navigate to App -> {}", appName);
        
        // Get URL from config
        String url = ConfigLoader.getAppUrl(appName);
        
        if (url == null || url.isEmpty()) {
            logger.error("No URL configured for application: {}", appName);
            logger.info("TIP: Add '{}.url=<your-url>' to config/app.properties", 
                appName.toLowerCase().replace(" ", "."));
            throw new RuntimeException("Application URL not configured: " + appName);
        }
        
        try {
            logger.info("Navigating to: {}", url);
            page.navigate(url);
            
            // Wait for page to be ready
            page.waitForLoadState();
            
            logger.success("SUCCESS: Navigated to {} ({})", appName, url);
            return true;
            
        } catch (Exception e) {
            logger.error("Navigation failed: {}", e.getMessage());
            return false;
        }
    }
}
