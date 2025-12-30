package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action handler for verifying that a new row was added to a table with specific values.
 * Handles steps like: "Then Verify New Row is added with 'value' in 'column' column"
 */
public class VerifyRowAddedAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyRowAddedAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("VerifyRowAddedAction requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        String columnName = enhancedPlan.getColumnName();
        String expectedValue = enhancedPlan.getValue();
        
        if (columnName == null || expectedValue == null) {
            logger.failure("Missing columnName or value for row verification");
            return false;
        }
        
        logger.info("Verifying row exists with '{}' in '{}' column", expectedValue, columnName);
        
        try {
            // Wait 100ms for form submission to complete and row to be added
            Thread.sleep(200);
            
            // Strategy 1: Try to find table rows
            Locator tableRows = page.locator("table").locator("tbody tr");
            int rowCount = tableRows.count();
            
            if (rowCount == 0) {
                // Strategy 2: Try data grid pattern
                tableRows = page.locator("[role='row'], .rt-tr, .data-row");
                rowCount = tableRows.count();
            }
            
            logger.debug("Found {} rows in table", rowCount);
            
            // Search for the value in the appropriate column
            boolean found = false;
            for (int i = 0; i < rowCount; i++) {
                Locator row = tableRows.nth(i);
                String rowText = row.innerText().toLowerCase();
                
                // Check if the row contains the expected value
                if (rowText.contains(expectedValue.toLowerCase())) {
                    logger.success("Found row containing '{}'", expectedValue);
                    logger.debug("  Row text: {}", row.innerText().replaceAll("\n", " | "));
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                logger.section("VALIDATION FAILED");
                logger.error(" Expected: Row with '{}' in '{}' column", expectedValue, columnName);
                logger.error(" Actual: No such row found in table");
                logger.info("--------------------------------------------------");
                return false;
            }
            
            logger.section("VALIDATION SUCCESS");
            logger.info(" Expected: Row with '{}' in '{}' column", expectedValue, columnName);
            logger.info(" Status: Row found in table");
            logger.info("--------------------------------------------------");
            return true;
            
        } catch (Exception e) {
            logger.section("VALIDATION FAILED");
            logger.error(" Error: {}", e.getMessage());
            logger.info("--------------------------------------------------");
            return false;
        }
    }
}
