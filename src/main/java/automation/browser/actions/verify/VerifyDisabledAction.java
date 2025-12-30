package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class VerifyDisabledAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyDisabledAction.class);
    
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

        Locator element = locator.waitForSmartElement(targetName, null, scope, plan.getFrameAnchor(), true);
        
        if (element != null) {
            boolean isEnabled = element.isEnabled();
            if ("label".equals(element.evaluate("el => el.tagName.toLowerCase()"))) {
                String forAttr = (String) element.getAttribute("for");
                if (forAttr != null) {
                     isEnabled = page.locator("#" + forAttr).isEnabled();
                }
            }

            if (!isEnabled) {
                logger.section("VALIDATION SUCCESS");
                logger.info(" Element '{}' matches expected state: DISABLED", targetName);
                logger.info("--------------------------------------------------");
                return true;
            } else {
                logger.section("VALIDATION FAILED");
                logger.error(" Element '{}' is ENABLED", targetName);
                logger.info("--------------------------------------------------");
                return false;
            }
        } else {
             logger.failure("Element not found for disablement check: {}", targetName);
             return false;
        }
    }
}
