package automation.browser.actions.input;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Fills an autocomplete/suggestion field by typing and selecting from dropdown.
 * Example: "enters location 'Dallas' from suggestion"
 * 
 * Strategy:
 * 1. Find the input field
 * 2. Type the value
 * 3. Wait for suggestions to appear
 * 4. Click the matching suggestion
 */
public class FillAutocompleteAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(FillAutocompleteAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String elementName = plan.getElementName();
        String value = plan.getValue();
        
        logger.info("Fill Autocomplete -> {} = '{}'", elementName, value);
        
        try {
            // Find the input field
            Locator inputField = smartLocator.waitForSmartElement(elementName, "input", null, null);
            
            if (inputField == null) {
                logger.error("Could not find autocomplete field: {}", elementName);
                return false;
            }
            
            // Clear existing value
            inputField.clear();
            
            // Type the value letter by letter to trigger autocomplete
            logger.debug("Typing '{}' to trigger autocomplete...", value);
            inputField.type(value, new Locator.TypeOptions().setDelay(100));
            
            // Wait a moment for suggestions to appear
            page.waitForTimeout(500);
            
            // Try to find and click the suggestion
            Locator suggestion = findSuggestion(page, value);
            
            if (suggestion != null && suggestion.count() > 0) {
                logger.debug("Found suggestion for '{}'", value);
                suggestion.first().click();
                logger.success("SUCCESS: Selected '{}' from autocomplete", value);
            } else {
                // If no suggestion found, just press Enter to accept what was typed
                logger.warning("No suggestion dropdown found, pressing Enter to confirm input");
                inputField.press("Enter");
                page.waitForTimeout(300);
                logger.success("SUCCESS: Entered '{}' in {} (no dropdown)", value, elementName);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to fill autocomplete: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Try multiple strategies to find the suggestion dropdown item
     */
    private Locator findSuggestion(Page page, String value) {
        // Strategy 1: Find by role="option" with exact text
        Locator byRole = page.locator("[role='option']").filter(
            new Locator.FilterOptions().setHasText(value)
        ).first();
        if (byRole.count() > 0) {
            logger.debug("Found suggestion via role='option'");
            return byRole;
        }
        
        // Strategy 2: Find li/div in common dropdown containers
        Locator byListItem = page.locator("ul li, .suggestion-item, .autocomplete-item, .dropdown-item").filter(
            new Locator.FilterOptions().setHasText(value)
        ).first();
        if (byListItem.count() > 0) {
            logger.debug("Found suggestion via list item");
            return byListItem;
        }
        
        // Strategy 3: Find any visible element with exact text that appeared recently
        Locator byText = page.getByText(value, new Page.GetByTextOptions().setExact(true)).first();
        if (byText.count() > 0 && byText.isVisible()) {
            logger.debug("Found suggestion via text match");
            return byText;
        }
        
        return null;
    }
}
