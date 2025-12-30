package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class VerifyEnabledAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyEnabledAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        Locator scope = null;

        if (plan.getRowAnchor() != null) {
            automation.browser.locator.table.TableNavigator navigator = new automation.browser.locator.table.TableNavigator();
            if (plan instanceof automation.planner.EnhancedActionPlan) {
                automation.planner.EnhancedActionPlan enhanced = (automation.planner.EnhancedActionPlan) plan;
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

        String actionType = plan.getActionType();
        boolean expectEnabled = !"verify_disabled".equals(actionType);

        Locator element = locator.waitForSmartElement(targetName, null, scope, plan.getFrameAnchor(), true);
        
        if (element != null) {
            boolean isEnabled = element.isEnabled();
            String tagName = (String) element.evaluate("el => el.tagName.toLowerCase()");
            
            // If it's a label, check the linked input
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
        } else {
             logger.failure("Element not found for enablement check: {}", targetName);
             return false;
        }
    }
}
