package automation.browser.actions.alert;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles JavaScript prompt dialogs.
 * 
 * Supports:
 * - prompt() - Enter text and accept
 * - prompt() - Dismiss without entering text
 */
public class PromptAlertAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(PromptAlertAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            AtomicBoolean dialogHandled = new AtomicBoolean(false);
            String promptText = plan.getValue(); // The text to enter in prompt
            boolean shouldDismiss = "dismiss".equalsIgnoreCase(plan.getElementName());
            
            // Set up one-time dialog handler
            page.onceDialog(dialog -> {
                try {
                    String message = dialog.message();
                    String type = dialog.type();
                    String defaultValue = dialog.defaultValue();
                    
                    logger.info("--------------------------------------------------");
                    logger.info(" PROMPT DIALOG DETECTED");
                    logger.info(" Type: {}", type);
                    logger.info(" Message: {}", message);
                    logger.info(" Default Value: {}", defaultValue);
                    
                    if (shouldDismiss) {
                        logger.info(" Action: DISMISSING");
                        dialog.dismiss();
                    } else {
                        logger.info(" Action: ENTERING TEXT '{}'", promptText);
                        dialog.accept(promptText != null ? promptText : "");
                    }
                    
                    logger.info("--------------------------------------------------");
                    dialogHandled.set(true);
                    
                } catch (Exception e) {
                    logger.error("Error handling prompt: {}", e.getMessage());
                }
            });
            
            // Wait a moment for dialog to appear and be handled
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            if (dialogHandled.get()) {
                if (shouldDismiss) {
                    logger.success("Prompt dismissed successfully");
                } else {
                    logger.success("Prompt accepted with text: {}", promptText);
                }
                return true;
            } else {
                logger.warning("No prompt dialog appeared");
                return true; // Don't fail if already handled
            }
            
        } catch (Exception e) {
            logger.error("Error in PromptAlertAction: {}", e.getMessage(), e);
            return false;
        }
    }
}
