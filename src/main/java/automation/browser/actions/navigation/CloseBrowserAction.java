package automation.browser.actions.navigation;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

public class CloseBrowserAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CloseBrowserAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        logger.warning("Skipping 'Close Browser' - Browser will be closed automatically at test end");
        return true;
    }
}
