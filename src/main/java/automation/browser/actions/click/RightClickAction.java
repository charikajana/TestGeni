package automation.browser.actions.click;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class RightClickAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(RightClickAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        
        Locator clickable = locator.waitForSmartElement(targetName, "button", null, plan.getFrameAnchor());
        
        if (clickable != null) {
            try {
                clickable.click(new Locator.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT));
                logger.browserAction("Right-click", targetName);
                return true;
            } catch (Exception e) {
                logger.failure("Right-click failed: {}", e.getMessage());
                return false;
            }
        } else {
            logger.failure("Element not found for right-clicking: {}", targetName);
            return false;
        }
    }
}
