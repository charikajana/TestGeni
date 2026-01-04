package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.planner.EnhancedActionPlan;
import automation.browser.locator.table.TableNavigator;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Consolidated handler for verifying if elements are enabled or disabled.
 */
public class VerifyEnablementAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyEnablementAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        String actionType = plan.getActionType();
        boolean expectEnabled = "verify_enabled".equals(actionType);
        
        logger.info("Verifying element '{}' is {}", targetName, expectEnabled ? "ENABLED" : "DISABLED");
        
        // Handle Row Scoping
        Locator scope = null;
        if (plan.getRowAnchor() != null) {
            TableNavigator navigator = new TableNavigator();
            if (plan instanceof EnhancedActionPlan) {
                EnhancedActionPlan enhanced = (EnhancedActionPlan) plan;
                String columnName = enhanced.getRowConditionColumn();
                String columnValue = enhanced.getRowConditionValue();
                if (columnName != null && columnValue != null) {
                    scope = navigator.findRowByColumnValue(page, columnName, columnValue);
                } else {
                    scope = navigator.findRowByAnchor(page, plan.getRowAnchor());
                }
            } else {
                scope = navigator.findRowByAnchor(page, plan.getRowAnchor());
            }
            if (scope == null) {
                logger.failure("Row not found for anchor: {}", plan.getRowAnchor());
                return false;
            }
        }

        // Find the element
        Locator element = locator.waitForSmartElement(targetName, null, scope, plan.getFrameAnchor(), plan.getParentAnchor(), true);
        
        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectEnabled ? "ENABLED" : "DISABLED")
                .comparisonType("BOOLEAN");

        if (element == null) {
            logger.failure("Element not found for state check: {}", targetName);
            result.match(false).elementFound(false).details("Element not found: " + targetName);
            plan.setMetadataValue("validation", result);
            return false;
        }

        try {
            boolean isEnabled = element.isEnabled();
            String tagName = (String) element.evaluate("el => el.tagName.toLowerCase()");
            
            // Special handling for labels (check the associated input)
            if ("label".equals(tagName)) {
                String forAttr = (String) element.getAttribute("for");
                if (forAttr != null && !forAttr.isEmpty()) {
                    isEnabled = page.locator("#" + forAttr).isEnabled();
                } else {
                    // Try to find a nested input
                    Locator nestedInput = element.locator("input");
                    if (nestedInput.count() > 0) {
                        isEnabled = nestedInput.first().isEnabled();
                    }
                }
            }

            result.actual(isEnabled ? "ENABLED" : "DISABLED").match(isEnabled == expectEnabled).elementFound(true);
            plan.setMetadataValue("validation", result);

            if (isEnabled == expectEnabled) {
                logger.section("VALIDATION SUCCESS");
                logger.info(" Element '{}' matches expected state: {}", targetName, expectEnabled ? "ENABLED" : "DISABLED");
                logger.info("--------------------------------------------------");
                return true;
            } else {
                logger.section("VALIDATION FAILED");
                logger.error(" Element '{}' state mismatch. Expected: {}, Actual: {}", 
                    targetName, 
                    expectEnabled ? "ENABLED" : "DISABLED",
                    isEnabled ? "ENABLED" : "DISABLED");
                logger.info("--------------------------------------------------");
                return false;
            }
        } catch (Exception e) {
            logger.failure("Error verifying state for element '{}': {}", targetName, e.getMessage());
            result.match(false).details("Error: " + e.getMessage());
            plan.setMetadataValue("validation", result);
            return false;
        }
    }
}
