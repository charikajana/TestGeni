package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Action handler for browser back navigation
 */
public class BackAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(BackAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            logger.info("Navigating back...");
            page.goBack();
            logger.success("Navigated back successfully");
            return true;
        } catch (Exception e) {
            logger.failure("Failed to navigate back: {}", e.getMessage());
            return false;
        }
    }
}
