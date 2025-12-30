package automation.browser.actions.modal;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Action to verify that a modal dialog is visible or has a specific title.
 */
public class VerifyModalAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyModalAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String expectedTitle = plan.getValue();
        boolean isNegative = "verify_modal_not_visible".equals(plan.getActionType());
        
        logger.info("Verifying modal visibility: Title='{}'", expectedTitle != null ? expectedTitle : "Any");
        
        try {
            // Locate the active modal using generic patterns
            // Works with: Bootstrap, Material-UI, custom modals
            Locator modal = page.locator(".modal.show, .modal-dialog, [role='dialog']").first();
            
            if (isNegative) {
                // Verify hidden
                PlaywrightAssertions.assertThat(modal).isHidden(new com.microsoft.playwright.assertions.LocatorAssertions.IsHiddenOptions().setTimeout(5000));
                logger.success("Modal is hidden as expected");
                return true;
            } else {
                // Verify visible
                PlaywrightAssertions.assertThat(modal).isVisible(new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions().setTimeout(5000));
                
                if (expectedTitle != null && !expectedTitle.isEmpty()) {
                    // Check if title matches
                    Locator title = modal.locator(".modal-title, .modal-header, [id*='title']").getByText(expectedTitle, new Locator.GetByTextOptions().setExact(false)).first();
                    if (title.count() > 0 && title.isVisible()) {
                        logger.success("Found modal with title: '{}'", expectedTitle);
                    } else {
                        logger.failure("Modal found but title '{}' not found in header", expectedTitle);
                        return false;
                    }
                } else {
                    logger.success("Modal is visible");
                }
                return true;
            }
            
        } catch (Error e) {
            if (isNegative) {
                logger.failure("Modal is still visible but should be hidden");
            } else {
                logger.failure("Modal not found or not visible: {}", e.getMessage());
            }
            return false;
        }
    }
}
