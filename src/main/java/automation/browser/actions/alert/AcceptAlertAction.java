package automation.browser.actions.alert;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles accepting JavaScript alerts and confirm dialogs.
 * Optionally verifies the alert message.
 * 
 * Supports:
 * - alert() - Simple message box
 * - confirm() - Message with OK/Cancel
 * 
 * Can verify alert message if expectedMessage is provided in ActionPlan.value
 */
public class AcceptAlertAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(AcceptAlertAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            AtomicBoolean dialogHandled = new AtomicBoolean(false);
            AtomicReference<String> dialogMessage = new AtomicReference<>("");
            String expectedMessage = plan.getValue(); // Expected message to verify
            
            // Set up one-time dialog handler
            page.onceDialog(dialog -> {
                try {
                    String message = dialog.message();
                    String type = dialog.type();
                    
                    dialogMessage.set(message);
                    
                    logger.alert(type, message);
                    if (expectedMessage != null && !expectedMessage.isEmpty()) {
                        logger.info(" Expected: {}", expectedMessage);
                    }
                    logger.info(" Action: ACCEPTING");
                    logger.info("--------------------------------------------------");
                    
                    // Accept the dialog
                    dialog.accept();
                    dialogHandled.set(true);
                    
                } catch (Exception e) {
                    logger.error("Error handling dialog: {}", e.getMessage());
                }
            });
            
            // Wait a moment for dialog to appear and be handled
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            if (dialogHandled.get()) {
                String actualMessage = dialogMessage.get();
                
                // Verify expected message if provided
                if (expectedMessage != null && !expectedMessage.isEmpty()) {
                if (actualMessage.contains(expectedMessage)) {
                    logger.section("ALERT MESSAGE VERIFIED");
                    logger.info(" Expected: {}", expectedMessage);
                    logger.info(" Actual: {}", actualMessage);
                    logger.info(" Match: SUCCESS");
                    logger.info("--------------------------------------------------");
                    return true;
                } else {
                    logger.section("ALERT MESSAGE MISMATCH");
                    logger.error(" Expected: {}", expectedMessage);
                    logger.error(" Actual: {}", actualMessage);
                    logger.info("--------------------------------------------------");
                    return false;
                }
                } else {
                    // No verification needed, just accept
                    logger.success("Alert accepted successfully");
                    return true;
                }
            } else {
                logger.warning("No alert appeared or already handled");
                return true; // Don't fail if alert was already handled by auto-handler
            }
            
        } catch (Exception e) {
            logger.error("Error in AcceptAlertAction: {}", e.getMessage(), e);
            return false;
        }
    }
}
