package automation.browser.actions.select;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Selects an option based on multiple criteria.
 * Example: "Select the Rate Plan from 'Sabre' with refundable 'No'"
 * 
 * Strategy:
 * 1. Find all elements matching the primary criterion (provider)
 * 2. Filter by secondary criterion (refundable status)
 * 3. Click the matching element
 */
public class SelectWithCriteriaAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SelectWithCriteriaAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String elementName = plan.getElementName();  // e.g., "Rate Plan"
        String primaryValue = plan.getValue();        // e.g., "Sabre"
        
        // Extract secondary criteria from the step text
        // Pattern captured groups: 1=element, 2=primaryValue, 3=secondaryCriterion, 4=secondaryValue
        String step = plan.getTarget();
        String secondaryCriterion = extractSecondaryCriterion(step);
        String secondaryValue = extractSecondaryValue(step);
        
        logger.info("Select With Criteria -> {} from '{}' with {} '{}'", 
            elementName, primaryValue, secondaryCriterion, secondaryValue);
        
        try {
            // Strategy 1: Try to find container for provider
            Locator providerContainer = page.locator(
                String.format("div,section,tr:has-text('%s')", primaryValue)
            ).first();
            
            if (providerContainer.count() == 0) {
                logger.warning("Could not find container for provider: {}", primaryValue);
                return tryFallbackStrategy(page, elementName, primaryValue, secondaryCriterion, secondaryValue);
            }
            
            // Within the provider container, look for the secondary criterion match
            Locator matchingElement = providerContainer.locator(
                String.format("*:has-text('%s')", secondaryValue)
            ).first();
            
            if (matchingElement.count() > 0) {
                // Found the element, now click the associated button/link
                Locator clickTarget = findClickableElement(providerContainer, elementName);
                
                if (clickTarget != null && clickTarget.count() > 0) {
                    clickTarget.click();
                    logger.success("SUCCESS: Selected {} from '{}' with {} '{}'",
                        elementName, primaryValue, secondaryCriterion, secondaryValue);
                    return true;
                }
            }
            
            logger.warning("Could not find matching element with all criteria");
            return tryFallbackStrategy(page, elementName, primaryValue, secondaryCriterion, secondaryValue);
            
        } catch (Exception e) {
            logger.error("Failed to select with criteria: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract secondary criterion name from step
     * e.g., "with refundable 'No'" -> "refundable"
     */
    private String extractSecondaryCriterion(String step) {
        String pattern = "with\\s+([\\w\\s]+?)\\s+[\"']";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(step);
        
        if (m.find()) {
            return m.group(1).trim();
        }
        return "criteria";
    }
    
    /**
     * Extract secondary value from step
     * e.g., "with refundable 'No'" -> "No"
     */
    private String extractSecondaryValue(String step) {
        String pattern = "with\\s+[\\w\\s]+?\\s+[\"']([^\"']+)[\"']";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(step);
        
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
    
    /**
     * Find clickable element within container
     */
    private Locator findClickableElement(Locator container, String elementName) {
        // Try common clickable patterns
        String[] selectors = {
            "button",
            "a",
            "[role='button']",
            ".btn",
            ".select-btn",
            "*:has-text('" + elementName + "')"
        };
        
        for (String selector : selectors) {
            Locator element = container.locator(selector).first();
            if (element.count() > 0 && element.isVisible()) {
                logger.debug("Found clickable element using: {}", selector);
                return element;
            }
        }
        
        // If no specific button found, return the container itself
        return container;
    }
    
    /**
     * Fallback strategy: find by text combination
     */
    private boolean tryFallbackStrategy(Page page, String elementName, String primaryValue, 
                                       String secondaryCriterion, String secondaryValue) {
        logger.debug("Trying fallback strategy with combined text search");
        
        try {
            // Look for elements containing both criteria values
            String combinedPattern = String.format("*:has-text('%s'):has-text('%s')", 
                primaryValue, secondaryValue);
            
            Locator combined = page.locator(combinedPattern).first();
            
            if (combined.count() > 0) {
                // Try to find a clickable element nearby
                Locator clickable = combined.locator("button,a,[role='button']").first();
                
                if (clickable.count() == 0) {
                    clickable = combined;
                }
                
                clickable.click();
                logger.success("SUCCESS: Selected using fallback strategy");
                return true;
            }
            
        } catch (Exception e) {
            logger.debug("Fallback strategy failed: {}", e.getMessage());
        }
        
        return false;
    }
}
