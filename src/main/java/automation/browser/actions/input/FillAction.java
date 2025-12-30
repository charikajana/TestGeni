package automation.browser.actions.input;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.browser.locator.table.TableNavigator;

public class FillAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(FillAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        String value = plan.getValue();

        // 1. Try to use intelligent locator if already found during planning
        // BUT: Check if we're still on the same page! Window switches can invalidate pre-resolved locators
        if (plan.hasMetadata("intelligent_locator")) {
            Locator intelligentLocator = (Locator) plan.getMetadataValue("intelligent_locator");
            String resolvedPageUrl = (String) plan.getMetadataValue("resolved_page_url");
            String currentPageUrl = page.url();
            
            if (intelligentLocator != null) {
                // Check if page URL changed since locator was resolved
                if (resolvedPageUrl != null && !resolvedPageUrl.equals(currentPageUrl)) {
                    logger.warn("Page URL changed from '{}' to '{}' - discarding stale pre-resolved locator and re-resolving",
                        resolvedPageUrl, currentPageUrl);
                    // Don't use the stale locator - fall through to re-resolve
                } else {
                    logger.debug("Using pre-resolved intelligent locator for: {}", targetName);
                    return performFill(intelligentLocator, targetName, value);
                }
            }
        }

        // 2. Handle row-based scope
        Locator scope = null;
        if (plan.getRowAnchor() != null) {
             TableNavigator navigator = new TableNavigator();
             scope = navigator.findRowByAnchor(page, plan.getRowAnchor());
             if (scope == null) {
                 logger.failure("Row not found for anchor: {}", plan.getRowAnchor());
                 return false;
             }
        }
        
        // 3. Find element using SmartLocator
        Locator input = locator.waitForSmartElement(targetName, "input", scope, plan.getFrameAnchor());
        if (input != null) {
            return performFill(input, targetName, value);
        } else {
            logger.failure("Element not found for filling: {}", targetName);
            return false;
        }
    }

    /**
     * Internal helper to perform the fill
     */
    private boolean performFill(Locator input, String targetName, String value) {
        try {
            input.fill(value != null ? value : "");
            logger.browserAction("Fill", targetName + " = '" + value + "'");
            return true;
        } catch (com.microsoft.playwright.PlaywrightException e) {
            logger.failure("Element found for '{}' but could not be filled: {}", targetName, e.getMessage().split("\n")[0]);
            return false;
        }
    }
}
