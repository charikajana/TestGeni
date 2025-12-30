package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for verifying the value of an input field
 */
public class VerifyValueAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyValueAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String expectedValue = plan.getValue();
        String targetName = plan.getElementName();
        
        if (expectedValue == null || targetName == null) {
            logger.failure("Verification failed - Missing element name or expected value");
            return false;
        }

        logger.debug("Verifying value of '{}' is '{}'", targetName, expectedValue);
        
        // Find the element
        Locator element = locator.waitForSmartElement(targetName, "field", null, plan.getFrameAnchor());
        
        if (element == null) {
            logger.failure("Field not found for value verification: {}", targetName);
            
            // Store validation result for BDD integration
            plan.setMetadataValue("validation", new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectedValue)
                .actual(null)
                .elementFound(false)
                .match(false)
                .comparisonType("EXACT")
                .details("Element not found: " + targetName));
            
            return false;
        }

        try {
            // Get the value using JS to be safe (handles properties correctly)
            String actualValue = (String) element.evaluate("el => el.value || el.innerText || ''");
            actualValue = actualValue.trim();
            
            boolean isMatch = actualValue.equals(expectedValue.trim());
            
            // Store validation result for BDD integration
            plan.setMetadataValue("validation", new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectedValue.trim())
                .actual(actualValue)
                .elementFound(true)
                .elementVisible(element.isVisible())
                .match(isMatch)
                .comparisonType("EXACT")
                .details(isMatch ? "Exact match" : "Values differ"));
            
            if (isMatch) {
                logger.success("Verification successful: Field '{}' contains expected value '{}'", targetName, expectedValue);
                return true;
            } else {
                logger.failure("Verification failed: Field '{}' expected value '{}' but found '{}'", targetName, expectedValue, actualValue);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error verifying value for {}: {}", targetName, e.getMessage());
            
            // Store error in validation result
            plan.setMetadataValue("validation", new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectedValue)
                .actual(null)
                .elementFound(true)
                .match(false)
                .comparisonType("EXACT")
                .details("Error reading value: " + e.getMessage()));
            
            return false;
        }
    }
}
