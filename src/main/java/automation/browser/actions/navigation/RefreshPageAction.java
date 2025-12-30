package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Action handler for refreshing/reloading the current page
 */
public class RefreshPageAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(RefreshPageAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            logger.info("Refreshing page...");
            page.reload();
            logger.success("Page refreshed successfully");
            return true;
        } catch (Exception e) {
            logger.failure("Failed to refresh page: {}", e.getMessage());
            return false;
        }
    }
}
