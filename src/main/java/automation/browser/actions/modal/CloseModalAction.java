package automation.browser.actions.modal;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action to close an open modal dialog.
 */
public class CloseModalAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CloseModalAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        logger.info("Attempting to close the open modal...");
        
        try {
            // 1. Find the active modal
            Locator modal = page.locator(".modal.show, .modal-dialog, [role='dialog']").first();
            
            if (modal.count() == 0 || !modal.isVisible()) {
                logger.warning("No active modal found to close");
                return true; // Consider success if no modal is there to close
            }
            
            // 2. Try closing via typical close buttons inside the modal
            // Strategy A: Header 'x' button
            Locator xButton = modal.locator("button.close, [aria-label='Close'], button:has-text('×')").first();
            if (xButton.count() > 0 && xButton.isVisible()) {
                logger.debug("Closing via header 'x' button");
                xButton.click();
                waitForModalToHide(modal);
                logger.success("Modal closed via 'x' button");
                return true;
            }
            
            // Strategy B: Footer 'Close' or 'Cancel' button
            Locator closeBtn = modal.locator("button:has-text('Close'), button:has-text('Cancel'), button:has-text('OK')").first();
            if (closeBtn.count() > 0 && closeBtn.isVisible()) {
                logger.debug("Closing via text button: '{}'", closeBtn.innerText());
                closeBtn.click();
                waitForModalToHide(modal);
                logger.success("Modal closed via text button");
                return true;
            }
            
            // Strategy C: Force close via Escape key
            logger.debug("Trying to close modal via Escape key");
            page.keyboard().press("Escape");
            waitForModalToHide(modal);
            
            if (modal.isHidden()) {
                logger.success("Modal closed via Escape key");
                return true;
            }
            
            logger.failure("Could not find a way to close the modal");
            return false;
            
        } catch (Exception e) {
            logger.error("Error closing modal: {}", e.getMessage());
            return false;
        }
    }
    
    private void waitForModalToHide(Locator modal) {
        try {
            modal.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(2000));
        } catch (Exception e) {
            // Ignore timeout
        }
    }
}
