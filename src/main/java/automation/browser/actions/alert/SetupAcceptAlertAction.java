package automation.browser.actions.alert;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Sets up a global alert handler that will accept the next dialog that appears.
 * This handler remains active until a dialog is triggered.
 * 
 * Usage: Set this action BEFORE clicking a button that will trigger an alert.
 */
public class SetupAcceptAlertAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SetupAcceptAlertAction.class);
    
    private static volatile boolean alertHandled = false;
    private static volatile String lastAlertMessage = "";
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            alertHandled = false;
            lastAlertMessage = "";
            
            logger.info("Setting up alert handler (will auto-accept next dialog)");
            
            // Set up persistent dialog handler
            page.onDialog(dialog -> {
                try {
                    String message = dialog.message();
                    String type = dialog.type();
                    
                    lastAlertMessage = message;
                    
                    logger.alert(type, message + " & AUTO-ACCEPTED");
                    logger.info("--------------------------------------------------");
                    
                    dialog.accept();
                    alertHandled = true;
                    
                } catch (Exception e) {
                    logger.error("Error in dialog handler: {}", e.getMessage());
                }
            });
            
            logger.success("Alert handler ready - next dialog will be auto-accepted");
            return true;
            
        } catch (Exception e) {
            logger.error("Error setting up alert handler: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public static boolean wasAlertHandled() {
        return alertHandled;
    }
    
    public static String getLastAlertMessage() {
        return lastAlertMessage;
    }
}
