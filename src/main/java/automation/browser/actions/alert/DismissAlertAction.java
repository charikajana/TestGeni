package automation.browser.actions.alert;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles dismissing JavaScript confirm dialogs.
 * 
 * Supports:
 * - confirm() - Clicks Cancel button
 */
public class DismissAlertAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(DismissAlertAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        try {
            AtomicBoolean dialogHandled = new AtomicBoolean(false);
            AtomicReference<String> dialogMessage = new AtomicReference<>("");
            
            // Set up one-time dialog handler
            page.onceDialog(dialog -> {
                try {
                    String message = dialog.message();
                    String type = dialog.type();
                    
                    dialogMessage.set(message);
                    
                    logger.alert(type, message);
                    logger.info(" Action: DISMISSING (Cancel)");
                    logger.info("--------------------------------------------------");
                    
                    // Dismiss the dialog (click Cancel)
                    dialog.dismiss();
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
                logger.success("Dialog dismissed successfully");
                return true;
            } else {
                logger.warning("No browser alert appeared. Checking for HTML modal as fallback...");
                
                // FALLBACK: Try to close as an HTML modal
                try {
                    com.microsoft.playwright.Locator modal = page.locator(".modal.show, .modal-dialog, [role='dialog']").first();
                    if (modal.count() > 0 && modal.isVisible()) {
                        logger.info("Found HTML modal, attempting to close...");
                        
                        // Try typical close buttons
                        com.microsoft.playwright.Locator closeBtn = modal.locator("button.close, [aria-label='Close'], button:has-text('×'), button:has-text('Close')").first();
                        if (closeBtn.count() > 0 && closeBtn.isVisible()) {
                            closeBtn.click();
                            logger.success("HTML modal closed successfully via fallback");
                            return true;
                        }
                        
                        // Escape key fallback
                        page.keyboard().press("Escape");
                        logger.success("Sent Escape key to close potential modal");
                        return true;
                    }
                } catch (Exception e) {
                    logger.debug("Fallback modal check failed: {}", e.getMessage());
                }
                
                logger.warning("No dialog or modal appeared to close");
                return true; // Return true as nothing was there to block
            }
            
        } catch (Exception e) {
            logger.error("Error in DismissAlertAction: {}", e.getMessage(), e);
            return false;
        }
    }
}
