package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for direct row actions (Edit/Delete/Remove the row where...).
 * Handles steps like: "Edit the row where Name is 'Vinoth'" or "Delete the row where ID is '123'"
 */
public class DirectRowActionHandler implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(DirectRowActionHandler.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("DirectRowActionHandler requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        String actionType = plan.getElementName();  // "edit", "delete", "remove", etc.
        String conditionColumn = enhancedPlan.getRowConditionColumn();
        String conditionValue = enhancedPlan.getRowConditionValue();
        
        if (actionType == null || conditionColumn == null || conditionValue == null) {
            logger.failure("Missing action type, condition column, or value");
            return false;
        }
        
        logger.info("Performing '{}' action on row where '{}' is '{}'", actionType, conditionColumn, conditionValue);
        
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
            
            // Search for the row matching the condition
            boolean found = false;
            for (int i = 0; i < rowCount; i++) {
                Locator row = tableRows.nth(i);
                String rowText = row.innerText().toLowerCase();
                
                // Check if the row contains the condition value
                if (rowText.contains(conditionValue.toLowerCase())) {
                    logger.debug("Found matching row: {}", rowText.replaceAll("\\n", " | "));
                    
                    // Find the appropriate button based on action type
                    String buttonText = mapActionToButtonText(actionType);
                    Locator button = row.locator(String.format("button:has-text('%s'), a:has-text('%s'), [role='button']:has-text('%s')", 
                        buttonText, buttonText, buttonText)).first();
                    
                    if (button.count() > 0) {
                        button.scrollIntoViewIfNeeded();
                        button.click();
                        logger.success("Performed '{}' action on row where '{}' is '{}'", actionType, conditionColumn, conditionValue);
                        found = true;
                        break;
                    } else {
                        logger.warning("No {} button found in matching row", buttonText);
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
            logger.failure("Error performing direct row action: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Map action type to likely button text
     */
    private String mapActionToButtonText(String actionType) {
        String normalized = actionType.toLowerCase();
        switch (normalized) {
            case "edit":
            case "update":
            case "modify":
                return "Edit";
            case "delete":
            case "remove":
                return "Delete";
            default:
                return actionType; // Use as-is if not recognized
        }
    }

}
