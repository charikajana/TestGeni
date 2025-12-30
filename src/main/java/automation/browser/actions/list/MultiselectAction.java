package automation.browser.actions.list;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.KeyboardModifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles multi-selection in selectable lists (NOT dropdowns).
 * Similar to file selection in Windows Explorer: Click to select one, Ctrl+Click to add/remove.
 * 
 * Examples:
 * - "Select 'Cras justo odio' from list"
 * - "Select multiple items 'One;Three;Five' from grid"
 * - "Multiselect 'Item1' and 'Item2' and 'Item3'"
 * - "Verify items 'One;Three' are selected"
 */
public class MultiselectAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(MultiselectAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String itemsText = plan.getValue();
        
        if (itemsText == null || itemsText.trim().isEmpty()) {
            logger.failure("No items specified for multiselect");
            return false;
        }
        
        // Parse items - support semicolon-separated values
        List<String> items = parseItems(itemsText);
        logger.info("Multiselect operation: {} item(s)", items.size());
        
        boolean isMultiSelect = items.size() > 1;
        
        // Select items
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            logger.info("  Selecting item {} of {}: '{}'", (i + 1), items.size(), item);
            
            boolean success = selectItem(page, locator, item, isMultiSelect && i > 0);
            
            if (!success) {
                logger.failure("Failed to select item: '{}'", item);
                return false;
            }
            
            // Small delay between selections
            if (i < items.size() - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        logger.success("Successfully selected {} item(s)", items.size());
        return true;
    }
    
    /**
     * Select a single item from the selectable list.
     * 
     * @param page Playwright page
     * @param locator Smart locator for finding elements
     * @param itemText Text of the item to select
     * @param useCtrlKey Whether to use Ctrl+Click (for multi-selection)
     * @return true if successful
     */
    private boolean selectItem(Page page, SmartLocator locator, String itemText, boolean useCtrlKey) {
        try {
            // Find the list item by text
            // Common patterns:
            // 1. <li class="list-group-item">Text</li>
            // 2. <div role="listitem">Text</div>
            // 3. Any element with selectable class containing text
            
            Locator item = findSelectableItem(page, itemText);
            
            if (item == null || item.count() == 0) {
                logger.failure("Could not find selectable item: '{}'", itemText);
                return false;
            }
            
            Locator target = item.first();

            // Perform click with modifiers if needed
            if (useCtrlKey) {
                logger.debug("Clicking with Ctrl modifier to add to selection");
                target.click(new Locator.ClickOptions()
                    .setModifiers(Arrays.asList(KeyboardModifier.CONTROLORMETA)));
            } else {
                target.click();
            }
            
            // Verify selection
            page.waitForTimeout(500); // Wait longer for UI update
            boolean isSelected = isItemSelected(target);
            
            if (isSelected) {
                logger.debug("Item '{}' is now selected", itemText);
                return true;
            } else {
                logger.failure("Item '{}' was not selected (no 'active' class found)", itemText);
                return false;
            }
            
        } catch (Exception e) {
            logger.failure("Error selecting item '{}': {}", itemText, e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a selectable item by its text content.
     * Tries multiple strategies to locate the item.
     */
    private Locator findSelectableItem(Page page, String itemText) {
        // Strategy 1: Look for list items with the exact text
        Locator byListItem = page.locator("li.list-group-item, [role='listitem'], .list-group-item")
            .filter(new Locator.FilterOptions().setHasText(itemText));
        
        // If multiple found, try to find the one that is NOT a container
        if (byListItem.count() > 1) {
            for (int i = 0; i < byListItem.count(); i++) {
                Locator candidate = byListItem.nth(i);
                String text = candidate.innerText().trim();
                if (text.equalsIgnoreCase(itemText)) {
                    return candidate;
                }
            }
        }

        if (byListItem.count() > 0) {
            logger.debug("Found item via list-group-item selector");
            return byListItem.first();
        }
        
        // Strategy 2: Look for any selectable element (common class patterns)
        Locator bySelectableClass = page.locator(".selectable, .list-item, [class*='select']")
            .filter(new Locator.FilterOptions().setHasText(itemText));
        
        if (bySelectableClass.count() > 0) {
            logger.debug("Found item via selectable class selector");
            return bySelectableClass.first();
        }
        
        // Strategy 3: Look for any clickable element with the text
        Locator byText = page.getByText(itemText, new Page.GetByTextOptions().setExact(true));
        
        if (byText.count() > 0) {
            logger.debug("Found item via text selector");
            return byText.first();
        }
        
        return null;
    }
    
    /**
     * Check if an item is currently selected.
     * Selected items typically have an 'active' or 'selected' class.
     */
    private boolean isItemSelected(Locator item) {
        try {
            return (boolean) item.evaluate("(el) => {" +
                    "  const cl = el.classList;" +
                    "  return cl.contains('active') || cl.contains('selected') || " +
                    "         el.getAttribute('aria-selected') === 'true' || " +
                    "         el.getAttribute('aria-checked') === 'true';" +
                    "}");
        } catch (Exception e) {
            logger.debug("Could not verify selection state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse items from semicolon-separated string or "and"-separated string.
     * Examples:
     * - "One;Two;Three" → ["One", "Two", "Three"]
     * - "One and Two and Three" → ["One", "Two", "Three"]
     */
    private List<String> parseItems(String itemsText) {
        List<String> items = new ArrayList<>();
        
        // Check for semicolon-separated
        if (itemsText.contains(";")) {
            String[] parts = itemsText.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    items.add(trimmed);
                }
            }
        } 
        // Check for "and"-separated (quoted items)
        else if (itemsText.matches(".*[\"'].*[\"']\\s+and\\s+[\"'].*[\"'].*")) {
            // Pattern: 'Item1' and 'Item2' and 'Item3'
            String[] parts = itemsText.split("\\s+and\\s+");
            for (String part : parts) {
                String cleaned = part.trim().replaceAll("^[\"']|[\"']$", "");
                if (!cleaned.isEmpty()) {
                    items.add(cleaned);
                }
            }
        }
        // Single item
        else {
            items.add(itemsText.trim());
        }
        
        return items;
    }
}
