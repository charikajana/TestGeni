package automation.browser.actions.frame;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;

/**
 * Handles switching to or out of iframes.
 */
public class SwitchFrameAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SwitchFrameAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        
        if ("switch_to_main_frame".equals(actionType)) {
            logger.info("Switching back to main content");
            // In Playwright we don't really 'switch', but we can log that we are resetting context
            // Actually, the user might want this to ensure next actions are global.
            return true; 
        }
        
        // Handling switch_to_frame
        String frameName = plan.getElementName();
        if (frameName == null) {
            // fallback to value if elementName is null (depends on parser)
            frameName = plan.getValue();
        }
        
        if (frameName == null || frameName.isEmpty()) {
            logger.failure("No frame name/ID provided for switching");
            return false;
        }

        logger.info("Locating iframe: '{}'", frameName);
        Frame frame = locator.findFrame(frameName);
        
        if (frame != null) {
            logger.success("Successfully identified iframe: '{}'", frameName);
            // In our framework, we don't have a 'current frame' state yet, 
            // but the SmartLocator uses the frameAnchor from the ActionPlan.
            // If the user uses "Switch to frame", they expect subsequent actions to be in that frame.
            // We could store it in BrowserService if we wanted persistence.
            return true;
        } else {
            logger.failure("Iframe '{}' not found on page", frameName);
            return false;
        }
    }
}
