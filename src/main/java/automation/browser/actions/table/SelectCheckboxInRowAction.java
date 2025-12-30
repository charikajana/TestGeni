package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for selecting a checkbox in a table row based on a condition.
 * Handles steps like: "And select checkbox in row where 'column' is 'value'"
 */
public class SelectCheckboxInRowAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SelectCheckboxInRowAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("SelectCheckboxInRowAction requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        String conditionColumn = enhancedPlan.getRowConditionColumn();
        String conditionValue = enhancedPlan.getRowConditionValue();
        
        if (conditionColumn == null || conditionValue == null) {
            logger.failure("Missing condition column or value for checkbox selection");
            return false;
        }
        
        logger.info("Selecting checkbox in row where '{}' is '{}'", conditionColumn, conditionValue);
        
        try {
            // Strategy 1: Try standard table structure
            Locator tableRows = page.locator("table tbody tr");
            int rowCount = tableRows.count();
            
            if (rowCount == 0) {
                // Strategy 2: Try data grid pattern
                tableRows = page.locator("[role='row'], .rt-tr, .data-row");
                rowCount = tableRows.count();
            }
            
            logger.debug("Found {} rows in table", rowCount);
            
            // Search for the row matching the condition
            boolean found = false;
            for (int i = 0; i < rowCount; i++) {
                Locator row = tableRows.nth(i);
                String rowText = row.innerText().toLowerCase();
                
                // Check if the row contains the condition value
                if (rowText.contains(conditionValue.toLowerCase())) {
                    logger.debug("Found matching row: {}", rowText.replaceAll("\\n", " | "));
                    
                    // Find and click the checkbox in this row
                    Locator checkbox = row.locator("input[type='checkbox']").first();
                    
                    if (checkbox.count() > 0) {
                        checkbox.scrollIntoViewIfNeeded();
                        checkbox.click();
                        logger.success("Checkbox selected in row where '{}' is '{}'", conditionColumn, conditionValue);
                        found = true;
                        break;
                    } else {
                        logger.warning("No checkbox found in matching row");
                        return false;
                    }
                }
            }
            
            if (!found) {
                logger.failure("No row found where '{}' is '{}'", conditionColumn, conditionValue);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.failure("Error selecting checkbox: {}", e.getMessage());
            return false;
        }
    }
}
