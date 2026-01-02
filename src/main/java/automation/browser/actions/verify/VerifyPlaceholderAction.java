package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action to verify placeholder attribute values of input elements
 */
public class VerifyPlaceholderAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyPlaceholderAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String fieldName = plan.getElementName();
        String expectedPlaceholder = plan.getValue();
        
        logger.info("Verifying {} field has placeholder: '{}'", fieldName, expectedPlaceholder);
        
        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectedPlaceholder)
                .comparisonType("TEXT_MATCH");

        try {
            // Find input field by label, name, id, or placeholder
            Locator field = findInputField(page, fieldName);
            
            if (field == null) {
                result.match(false).elementFound(false).details("Could not find input field: " + fieldName);
                plan.setMetadataValue("validation", result);
                throw new RuntimeException("Could not find input field: " + fieldName);
            }
            
            // Get placeholder attribute
            String actualPlaceholder = field.getAttribute("placeholder");
            result.actual(actualPlaceholder != null ? actualPlaceholder : "[No Placeholder]").elementFound(true);

            if (actualPlaceholder == null) {
                logger.error("FAILURE: Field '{}' has no placeholder attribute", fieldName);
                result.match(false).details("Field has no placeholder attribute");
                plan.setMetadataValue("validation", result);
                throw new RuntimeException("Field '" + fieldName + "' has no placeholder attribute");
            }
            
            // Verify placeholder value
            boolean match = actualPlaceholder.trim().equalsIgnoreCase(expectedPlaceholder.trim()) || 
                          actualPlaceholder.toLowerCase().contains(expectedPlaceholder.toLowerCase());
            
            result.match(match);
            plan.setMetadataValue("validation", result);

            if (match) {
                logger.info("\n--------------------------------------------------");
                logger.info(" PLACEHOLDER VERIFICATION SUCCESS");
                logger.info("--------------------------------------------------");
                logger.info(" Field: {}", fieldName);
                logger.info(" Expected: {}", expectedPlaceholder);
                logger.info(" Actual: {}", actualPlaceholder);
                logger.info(" Match: {}", actualPlaceholder.trim().equalsIgnoreCase(expectedPlaceholder.trim()) ? "EXACT" : "CONTAINS");
                logger.info("--------------------------------------------------");
                return true;
            } else {
                logger.error("FAILURE: Placeholder mismatch for field '{}'", fieldName);
                logger.error(" Expected: {}", expectedPlaceholder);
                logger.error(" Actual: {}", actualPlaceholder);
                throw new RuntimeException(
                    String.format("Placeholder mismatch for field '%s'. Expected: '%s', Actual: '%s'",
                        fieldName, expectedPlaceholder, actualPlaceholder));
            }
            
        } catch (Exception e) {
            if (!plan.hasMetadata("validation")) {
                result.match(false).details("Error: " + e.getMessage());
                plan.setMetadataValue("validation", result);
            }
            logger.error("Error during placeholder verification: {}", e.getMessage());
            throw new RuntimeException("Placeholder verification failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find input field by various strategies
     */
    private Locator findInputField(Page page, String fieldName) {
        // Try by label
        try {
            Locator byLabel = page.locator("text=" + fieldName).locator("..").locator("input");
            if (byLabel.count() > 0) {
                return byLabel.first();
            }
        } catch (Exception ignored) {}
        
        // Try by placeholder
        try {
            Locator byPlaceholder = page.locator("input[placeholder*='" + fieldName + "' i]");
            if (byPlaceholder.count() > 0) {
                return byPlaceholder.first();
            }
        } catch (Exception ignored) {}
        
        // Try by name attribute
        try {
            Locator byName = page.locator("input[name*='" + fieldName.toLowerCase().replace(" ", "") + "' i]");
            if (byName.count() > 0) {
                return byName.first();
            }
        } catch (Exception ignored) {}
        
        // Try by id attribute
        try {
            Locator byId = page.locator("input[id*='" + fieldName.toLowerCase().replace(" ", "") + "' i]");
            if (byId.count() > 0) {
                return byId.first();
            }
        } catch (Exception ignored) {}
        
        // Try semantic matching - find input with text nearby
        try {
            // Get all inputs with placeholders
            Locator allInputs = page.locator("input[placeholder]");
            int count = allInputs.count();
            
            for (int i = 0; i < count; i++) {
                Locator input = allInputs.nth(i);
                String placeholder = input.getAttribute("placeholder");
                String name = input.getAttribute("name");
                String id = input.getAttribute("id");
                
                // Check if any attribute contains the field name
                if ((placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase())) ||
                    (name != null && name.toLowerCase().contains(fieldName.toLowerCase().replace(" ", ""))) ||
                    (id != null && id.toLowerCase().contains(fieldName.toLowerCase().replace(" ", "")))) {
                    return input;
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }
}
