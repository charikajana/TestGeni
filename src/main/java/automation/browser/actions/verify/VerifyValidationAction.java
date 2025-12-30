package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for verifying the validation state of an element (e.g., red border/invalid)
 */
public class VerifyValidationAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyValidationAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        
        // Find the element
        Locator element = locator.waitForSmartElement(targetName, "field", null, plan.getFrameAnchor());
        
        if (element == null) {
            logger.failure("Field not found for validation check: {}", targetName);
            return false;
        }

        try {
            // Check for invalid state or red border
            // 1. HTML5 validation state
            // 2. CSS class indicating error (common across frameworks)
            // 3. Computed style border-color
            boolean isInvalid = (boolean) element.evaluate("el => {" +
                "  const style = window.getComputedStyle(el);" +
                "  const borderColor = style.borderColor || '';" +
                "  const isRed = borderColor.includes('rgb(220, 53, 69)') || borderColor.includes('255, 0, 0') || borderColor.includes('red');" +
                "  const hasErrorClass = el.classList.contains('is-invalid') || el.classList.contains('error');" +
                "  const isHTMLInvalid = el.validity && !el.validity.valid;" +
                "  const wasValidated = el.closest('form') ? el.closest('form').classList.contains('was-validated') : false;" +
                "  return isRed || hasErrorClass || (wasValidated && isHTMLInvalid);" +
                "}");

            if (isInvalid) {
                logger.success("Field '{}' is correctly identified as invalid (red border/error state)", targetName);
                return true;
            } else {
                logger.failure("Field '{}' does not show expected validation error (red border)", targetName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking validation for {}: {}", targetName, e.getMessage());
            return false;
        }
    }
}
