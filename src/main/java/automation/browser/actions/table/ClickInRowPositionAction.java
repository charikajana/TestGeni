package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for clicking elements in a table row by position (first, last, 3rd, row 3).
 * Handles steps like: "Click Edit button in first row" or "Click Delete in row 3"
 */
public class ClickInRowPositionAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(ClickInRowPositionAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("ClickInRowPositionAction requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        String buttonName = plan.getElementName();
        String rowPosition = plan.getValue(); // "first", "last", "3", "3rd", "row 3"
        
        if (buttonName == null || rowPosition == null) {
            logger.failure("Missing button name or row position");
            return false;
        }
        
        logger.info("Clicking '{}' in {} row", buttonName, rowPosition);
        
        try {
            // Find table rows
            Locator tableRows = page.locator("table tbody tr");
            int rowCount = tableRows.count();
            
            if (rowCount == 0) {
                // Try data grid pattern
                tableRows = page.locator("[role='row'], .rt-tr, .data-row");
                rowCount = tableRows.count();
            }
            
            logger.debug("Found {} rows in table", rowCount);
            
            // Determine which row to select
            int targetRowIndex = -1;
            String normalizedPosition = rowPosition.toLowerCase().trim();
            
            if (normalizedPosition.equals("first")) {
                targetRowIndex = 0;
            } else if (normalizedPosition.equals("last")) {
                targetRowIndex = rowCount - 1;
            } else {
                // Extract number from "3rd", "row 3", "3", etc.
                String numberStr = normalizedPosition.replaceAll("[^\\d]", "");
                if (!numberStr.isEmpty()) {
                    targetRowIndex = Integer.parseInt(numberStr) - 1; // Convert to 0-based index
                }
            }
            
            if (targetRowIndex < 0 || targetRowIndex >= rowCount) {
                logger.failure("Invalid row position: {}. Table has {} rows", rowPosition, rowCount);
                return false;
            }
            
            // Get the target row
            Locator targetRow = tableRows.nth(targetRowIndex);
            logger.debug("Targeting row at index {}: {}", targetRowIndex, targetRow.innerText().replaceAll("\\n", " | "));
            
            // Find and click the button/element in that row
            Locator button = targetRow.locator(String.format("text='%s'", buttonName)).first();
            
            if (button.count() == 0) {
                // Try finding by partial text or button type
                button = targetRow.locator(String.format("button:has-text('%s'), a:has-text('%s'), [role='button']:has-text('%s')", 
                    buttonName, buttonName, buttonName)).first();
            }
            
            if (button.count() > 0) {
                button.scrollIntoViewIfNeeded();
                button.click();
                logger.success("Clicked '{}' in {} row", buttonName, rowPosition);
                return true;
            } else {
                logger.failure("Button '{}' not found in {} row", buttonName, rowPosition);
                return false;
            }
            
        } catch (Exception e) {
            logger.failure("Error clicking in row by position: {}", e.getMessage());
            return false;
        }
    }
}
