package automation.browser.actions.alert;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Consolidated handler for all JavaScript native dialogs (alert, confirm, prompt).
 * Also includes fallback logic for HTML-based modals when dismissing.
 */
public class JavaScriptDialogAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(JavaScriptDialogAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        String expectedValue = plan.getValue();
        
        AtomicBoolean dialogHandled = new AtomicBoolean(false);
        AtomicReference<String> dialogMessage = new AtomicReference<>("");
        
        try {
            // Set up one-time dialog handler
            page.onceDialog(dialog -> {
                try {
                    String message = dialog.message();
                    String type = dialog.type();
                    dialogMessage.set(message);
                    
                    logger.alert(type, message);
                    
                    switch (actionType) {
                        case "accept_alert":
                        case "verify_alert":
                            logger.info(" Action: ACCEPTING");
                            dialog.accept();
                            break;
                            
                        case "dismiss_alert":
                        case "dismiss_prompt":
                            logger.info(" Action: DISMISSING");
                            dialog.dismiss();
                            break;
                            
                        case "prompt_alert":
                            logger.info(" Action: ENTERING TEXT '{}'", expectedValue);
                            dialog.accept(expectedValue != null ? expectedValue : "");
                            break;
                            
                        default:
                            logger.info(" Action: DEFAULT ACCEPT");
                            dialog.accept();
                            break;
                    }
                    dialogHandled.set(true);
                } catch (Exception e) {
                    logger.error("Error in dialog handler: {}", e.getMessage());
                }
            });
            
            // Wait for dialog
            Thread.sleep(800);
            
            if (dialogHandled.get()) {
                String actualMsg = dialogMessage.get();
                // Optional verification for 'verify_alert'
                if ("verify_alert".equals(actionType) && expectedValue != null && !expectedValue.isEmpty()) {
                    if (actualMsg.contains(expectedValue)) {
                        logger.success("Alert message verified: '{}'", expectedValue);
                        return true;
                    } else {
                        logger.failure("Alert message mismatch. Expected: '{}', Actual: '{}'", expectedValue, actualMsg);
                        return false;
                    }
                }
                logger.success("Dialog handled successfully");
                return true;
            } else {
                // FALLBACK for 'dismiss' - try HTML modals
                if (actionType.contains("dismiss")) {
                    return handleHtmlModalFallback(page);
                }
                logger.warning("No native dialog detected");
                return true; 
            }
        } catch (Exception e) {
            logger.error("Failed to handle dialog: {}", e.getMessage());
            return false;
        }
    }

    private boolean handleHtmlModalFallback(Page page) {
        try {
            com.microsoft.playwright.Locator modal = page.locator(".modal.show, .modal-dialog, [role='dialog'], .swal2-container").first();
            if (modal.count() > 0 && modal.isVisible()) {
                logger.info("Found HTML modal/popup, attempting to close...");
                com.microsoft.playwright.Locator closeBtn = modal.locator("button.close, [aria-label='Close'], button:has-text('×'), button:has-text('Close'), .swal2-close").first();
                if (closeBtn.count() > 0 && closeBtn.isVisible()) {
                    closeBtn.click();
                    logger.success("Closed via HTML close button");
                    return true;
                }
                page.keyboard().press("Escape");
                logger.success("Closed via Escape key");
                return true;
            }
        } catch (Exception ignored) {}
        return true;
    }
}
