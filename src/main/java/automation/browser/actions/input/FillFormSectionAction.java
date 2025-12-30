package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.ConfigLoader;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Map;

/**
 * Auto-fills an entire form section from configuration.
 * Example: "Add Booking Contact details", "Add Traveller details"
 * 
 * Strategy:
 * 1. Get form data from config based on section name
 * 2. Find each field by name (firstName, lastName, email, etc.)
 * 3. Fill all fields automatically
 */
public class FillFormSectionAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(FillFormSectionAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String sectionName = plan.getElementName();  // e.g., "Booking Contact", "Traveller"
        
        logger.info("Fill Form Section -> {}", sectionName);
        
        try {
            // Normalize section name for config lookup
            String configKey = normalizeSection(sectionName);
            
            // Get form data from config
            Map<String, String> formData = ConfigLoader.getFormSectionData(configKey);
            
            if (formData.isEmpty()) {
                logger.error("No form data found for section: {}", sectionName);
                logger.info("TIP: Add {}.fieldName=value to config/app.properties", configKey);
                return false;
            }
            
            int filledCount = 0;
            int failedCount = 0;
            
            // Fill each field
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                String fieldName = entry.getKey();
                String value = entry.getValue();
                
                if (fillField(smartLocator, fieldName, value)) {
                    filledCount++;
                } else {
                    failedCount++;
                    logger.warning("Could not fill field: {}", fieldName);
                }
            }
            
            if (filledCount > 0) {
                logger.success("SUCCESS: Filled {}/{} fields in section '{}'", 
                    filledCount, formData.size(), sectionName);
                return true;  // Success if at least one field filled
            } else {
                logger.error("Could not fill any fields in section: {}", sectionName);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to fill form section: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Normalize section name for config lookup
     * "Booking Contact" -> "booking.contact"
     * "Traveller" -> "traveller"
     */
    private String normalizeSection(String sectionName) {
        return sectionName.toLowerCase()
            .replace(" ", ".")
            .trim();
    }
    
    /**
     * Fill a single field
     */
    private boolean fillField(SmartLocator smartLocator, String fieldName, String value) {
        try {
            // Convert camelCase to readable format
            // "firstName" -> "first name" or "First Name"
            String readableFieldName = camelCaseToReadable(fieldName);
            
            logger.debug("Attempting to fill: {} = '{}'", readableFieldName, value);
            
            // Try multiple field name variations
            String[] nameVariations = {
                fieldName,                    // firstName
                readableFieldName,            // first name
                capitalize(readableFieldName), // First Name
                fieldName.toLowerCase(),      // firstname
                readableFieldName.replace(" ", "")  // firstname
            };
            
            for (String nameVar : nameVariations) {
                Locator field = smartLocator.findSmartElement(nameVar, "input", null, null);
                
                if (field != null && field.count() > 0) {
                    field.clear();
                    field.fill(value);
                    logger.debug("✓ Filled {} = '{}'", readableFieldName, value);
                    return true;
                }
            }
            
            // If not found by name, try by placeholder
            Locator fieldByPlaceholder = smartLocator.findSmartElement(readableFieldName, null, null, null);
            if (fieldByPlaceholder != null && fieldByPlaceholder.count() > 0) {
                fieldByPlaceholder.clear();
                fieldByPlaceholder.fill(value);
                logger.debug("✓ Filled {} = '{}' (by placeholder)", readableFieldName, value);
                return true;
            }
            
            logger.debug("✗ Field not found: {}", readableFieldName);
            return false;
            
        } catch (Exception e) {
            logger.debug("Error filling field {}: {}", fieldName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Convert camelCase to readable format
     * "firstName" -> "first name"
     */
    private String camelCaseToReadable(String camelCase) {
        // Add space before uppercase letters
        String result = camelCase.replaceAll("([A-Z])", " $1");
        return result.trim().toLowerCase();
    }
    
    /**
     * Capitalize first letter of each word
     * "first name" -> "First Name"
     */
    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }
}
