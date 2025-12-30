package automation.browser.actions.keyboard;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Action handler for keyboard key presses
 * Supports: Escape, Enter, Tab, Space, Delete, Backspace, Arrow keys
 */
public class PressKeyAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(PressKeyAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            // Get the key name from the plan (captured from regex group 1)
            String keyName = plan.getElementName();
            
            if (keyName == null || keyName.trim().isEmpty()) {
                logger.failure("No key specified to press");
                return false;
            }
            
            logger.info("Pressing key: {}", keyName);
            
            // Press the key on the page
            page.keyboard().press(keyName);
            
            logger.success("Key '{}' pressed successfully", keyName);
            return true;
            
        } catch (Exception e) {
            logger.failure("Failed to press key: {}", e.getMessage());
            return false;
        }
    }
}
