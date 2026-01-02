package automation.browser.actions.table;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.browser.locator.builders.DynamicTableXPathBuilder;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class VerifyRowNotExistsAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyRowNotExistsAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        if (!(plan instanceof EnhancedActionPlan)) {
            logger.failure("VerifyRowNotExistsAction requires EnhancedActionPlan");
            return false;
        }
        
        EnhancedActionPlan enhancedPlan = (EnhancedActionPlan) plan;
        String columnName = enhancedPlan.getRowConditionColumn();
        String columnValue = enhancedPlan.getRowConditionValue();
        
        if (columnName == null || columnValue == null) {
            logger.failure("Column name and value required for row validation");
            return false;
        }
        
        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(String.format("Row where '%s' = '%s' should NOT exist", columnName, columnValue))
                .comparisonType("ROW_ABSENCE");

        // Use XPath builder to check if row exists
        DynamicTableXPathBuilder builder = new DynamicTableXPathBuilder(page);
        String xpath = builder.buildRowXPath(columnName, columnValue);
        
        if (xpath == null) {
            logger.failure("Could not build XPath for row validation");
            result.match(false).details("Could not build XPath for row validation");
            plan.setMetadataValue("validation", result);
            return false;
        }
        
        Locator row = page.locator(xpath);
        int rowCount = row.count();
        
        result.match(rowCount == 0).actual(rowCount == 0 ? "Row not found" : "Row still exists");
        plan.setMetadataValue("validation", result);

        if (rowCount == 0) {
            // Row does NOT exist - SUCCESS!
            logger.section("VALIDATION SUCCESS");
            logger.info(" Expected: Row with '{}' = '{}' should NOT exist", columnName, columnValue);
            logger.info(" Actual  : Row NOT found (deleted successfully)");
            logger.info("--------------------------------------------------");
            return true;
        } else {
            // Row still exists - FAILURE!
            String rowText = row.first().innerText().replaceAll("\n", " | ");
            logger.section("VALIDATION FAILED");
            logger.error(" Expected: Row with '{}' = '{}' should NOT exist", columnName, columnValue);
            logger.error(" Actual  : Row STILL EXISTS");
            logger.error(" Row Content: {}", rowText);
            logger.info("--------------------------------------------------");
            return false;
        }
    }
}
