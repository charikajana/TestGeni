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
        Locator input = locator.waitForSmartElement(targetName, "input", scope, plan.getFrameAnchor(), plan.getParentAnchor());
        if (input != null) {
            locator.recordMatch(plan);
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
        String expectedValue = value != null ? value : "";
        try {
            // Get the actual field information for validation logging
            String actualId = (String) input.evaluate("el => el.id || ''");
            String actualName = (String) input.evaluate("el => el.name || ''");
            String actualPlaceholder = (String) input.evaluate("el => el.placeholder || ''");
            
            // CRITICAL: Log what field we're actually filling to detect false positives
            logger.info("Filling field: Target='{}', ActualID='{}', ActualName='{}', Placeholder='{}'", 
                targetName, actualId, actualName, actualPlaceholder);
            
            // VALIDATION: Warn if target doesn't match actual field
            String targetLower = targetName.toLowerCase().replaceAll("\\s+", "");
            String idLower = actualId.toLowerCase();
            String nameLower = actualName.toLowerCase();
            
            boolean matchFound = idLower.contains(targetLower) || nameLower.contains(targetLower) || 
                                targetLower.contains(idLower) || targetLower.contains(nameLower);
            
            if (!matchFound && !actualId.isEmpty()) {
                logger.warn("POTENTIAL FALSE POSITIVE: Target='{}' but filling field ID='{}' name='{}'. These don't match!",
                    targetName, actualId, actualName);
            }
            
            // Perform the fill
            input.fill(expectedValue);
            
            // Post-interaction validation to avoid false positives
            String actualValue = (String) input.evaluate("el => el.value || ''");
            
            if (actualValue.equals(expectedValue)) {
                logger.browserAction("Fill", targetName + " = '" + expectedValue + "'");
                return true;
            } else {
                // Discrepancy detected - some UI frameworks (like React/Angular) might need a moment or events
                logger.warning("Fill value mismatch for '{}'. Expected: '{}', Actual: '{}'. Retrying with focus...", 
                    targetName, expectedValue, actualValue);
                
                input.focus();
                input.fill(expectedValue);
                actualValue = (String) input.evaluate("el => el.value || ''");
                
                if (actualValue.equals(expectedValue)) {
                    logger.browserAction("Fill", targetName + " = '" + expectedValue + "' (Resolved after retry)");
                    return true;
                } else {
                    logger.failure("CRITICAL: Value did not stick for '{}'. Expected: '{}', Actual: '{}'", 
                        targetName, expectedValue, actualValue);
                    return false;
                }
            }
        } catch (com.microsoft.playwright.PlaywrightException e) {
            logger.failure("Element found for '{}' but could not be filled: {}", targetName, e.getMessage().split("\n")[0]);
            return false;
        }
    }
}
