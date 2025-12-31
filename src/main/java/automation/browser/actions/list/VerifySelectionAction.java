package automation.browser.actions.list;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that specific items are selected in a selectable list.
 * 
 * Examples:
 * - "Verify 'Cras justo odio' is selected"
 * - "Verify items 'One;Three;Five' are selected"
 * - "Verify 'Item1' is not selected"
 */
public class VerifySelectionAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifySelectionAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        String itemsText = plan.getValue();
        
        if (itemsText == null || itemsText.trim().isEmpty()) {
            logger.failure("No items specified for selection verification");
            return false;
        }
        
        // Parse expected items
        List<String> expectedItems = parseItems(itemsText);
        boolean expectSelected = !"verify_not_selected".equals(actionType);
        
        logger.info("Verifying {} item(s) {} selected", 
            expectedItems.size(), 
            expectSelected ? "are" : "are NOT");
        
        // Get all currently selected items
        List<String> selectedItems = getSelectedItems(page);
        logger.debug("Currently selected items: {}", selectedItems);
        
        // Verify each expected item
        boolean allValid = true;
        for (String expectedItem : expectedItems) {
            boolean isCurrentlySelected = selectedItems.contains(expectedItem);
            boolean matches = (expectSelected == isCurrentlySelected);
            
            if (matches) {
                logger.success("Item '{}' is {} selected (as expected)", 
                    expectedItem, 
                    isCurrentlySelected ? "" : "NOT");
            } else {
                logger.failure("Item '{}' is {} selected (expected: {})", 
                    expectedItem,
                    isCurrentlySelected ? "" : "NOT",
                    expectSelected ? "selected" : "not selected");
                allValid = false;
            }
        }
        
        return allValid;
    }
    
    /**
     * Get all currently selected items from the page.
     * Returns a list of text content from items with 'active' or 'selected' class.
     */
    private List<String> getSelectedItems(Page page) {
        List<String> selectedItems = new ArrayList<>();
        
        try {
            // Strategy 1: Find by 'active' class (common across frameworks)
            Locator activeItems = page.locator(".list-group-item.active, [role='listitem'].active");
            
            if (activeItems.count() > 0) {
                List<String> texts = activeItems.allTextContents();
                for (String text : texts) {
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty()) {
                        selectedItems.add(trimmed);
                    }
                }
                return selectedItems;
            }
            
            // Strategy 2: Find by 'selected' class
            Locator selectedByClass = page.locator(".selected, .is-selected");
            
            if (selectedByClass.count() > 0) {
                List<String> texts = selectedByClass.allTextContents();
                for (String text : texts) {
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty()) {
                        selectedItems.add(trimmed);
                    }
                }
                return selectedItems;
            }
            
            // Strategy 3: Find by ARIA attribute
            Locator selectedByAria = page.locator("[aria-selected='true']");
            
            if (selectedByAria.count() > 0) {
                List<String> texts = selectedByAria.allTextContents();
                for (String text : texts) {
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty()) {
                        selectedItems.add(trimmed);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error getting selected items: {}", e.getMessage());
        }
        
        return selectedItems;
    }
    
    /**
     * Parse items from semicolon-separated string.
     * Examples:
     * - "One;Two;Three" → ["One", "Two", "Three"]
     * - "Single Item" → ["Single Item"]
     */
    private List<String> parseItems(String itemsText) {
        List<String> items = new ArrayList<>();
        
        if (itemsText.contains(";")) {
            String[] parts = itemsText.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    items.add(trimmed);
                }
            }
        } else {
            items.add(itemsText.trim());
        }
        
        return items;
    }
}
