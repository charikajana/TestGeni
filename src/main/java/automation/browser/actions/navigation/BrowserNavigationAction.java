package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Consolidated action handler for browser-level navigation operations.
 * Handles: back, forward, and refresh.
 */
public class BrowserNavigationAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(BrowserNavigationAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        
        try {
            switch (actionType) {
                case "browser_back":
                    logger.info("Navigating back...");
                    page.goBack();
                    logger.success("Navigated back successfully");
                    return true;
                    
                case "browser_forward":
                    logger.info("Navigating forward...");
                    page.goForward();
                    logger.success("Navigated forward successfully");
                    return true;
                    
                case "refresh_page":
                    logger.info("Refreshing page...");
                    page.reload();
                    logger.success("Page refreshed successfully");
                    return true;
                    
                default:
                    logger.error("Unknown browser navigation action: {}", actionType);
                    return false;
            }
        } catch (Exception e) {
            logger.failure("Browser navigation action '{}' failed: {}", actionType, e.getMessage());
            return false;
        }
    }
}
