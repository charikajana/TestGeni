package automation.browser.actions.input;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Consolidated handler for checkbox and radio button interactions.
 * Handles both 'check' and 'uncheck' actions with robust fallback strategies.
 */
public class CheckToggleAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CheckToggleAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        String actionType = plan.getActionType();
        boolean isCheck = "check".equalsIgnoreCase(actionType);
        
        logger.info("{} element: {}", isCheck ? "Checking" : "Unchecking", targetName);
        
        Locator checkbox = locator.waitForSmartElement(targetName, "checkbox", null, plan.getFrameAnchor(), plan.getParentAnchor(), true);
        
        if (checkbox == null) {
            logger.failure("Checkbox not found: {}", targetName);
            return false;
        }

        try {
            // Step 1: Try native Playwright check/uncheck (forced for speed and robustness)
            if (isCheck) {
                checkbox.check(new Locator.CheckOptions().setForce(true).setTimeout(5000));
            } else {
                checkbox.uncheck(new Locator.UncheckOptions().setForce(true).setTimeout(5000));
            }
            logger.browserAction(isCheck ? "Check" : "Uncheck", targetName);
            return true;
            
        } catch (Exception e) {
            logger.debug("Forced native {} failed for '{}', trying robust fallbacks", actionType, targetName);
            
            // Strategy 1: Forced Click (often works when check/uncheck fails due to complex styling)
            try {
                checkbox.click(new Locator.ClickOptions().setForce(true).setTimeout(2000));
                logger.browserAction(isCheck ? "Check (Fallback Click)" : "Uncheck (Fallback Click)", targetName);
                return true;
            } catch (Exception e2) {
                logger.debug("Forced click failed, trying to find visible labels for '{}'", targetName);
                
                // Strategy 2: Click by text (labels often overlap the input)
                String[] candidates = {
                    targetName,
                    targetName.replaceAll("(?i)\\s*(?:check\\s*box|checkbox)$", "").trim()
                };

                for (String candidate : candidates) {
                    try {
                        page.getByText(candidate, new Page.GetByTextOptions().setExact(false))
                            .first().click(new Locator.ClickOptions().setTimeout(2000));
                        logger.browserAction(isCheck ? "Check (Text Click: " + candidate + ")" : "Uncheck (Text Click: " + candidate + ")", targetName);
                        return true;
                    } catch (Exception ignored) {}
                }
                
                // Strategy 3: Click parent label (very common in modern UI frameworks)
                try {
                    checkbox.locator("xpath=ancestor::label").first().click(new Locator.ClickOptions().setTimeout(2000));
                    logger.browserAction(isCheck ? "Check (Parent Label Click)" : "Uncheck (Parent Label Click)", targetName);
                    return true;
                } catch (Exception e4) {
                    logger.failure("Could not {} or click element '{}': {}", actionType, targetName, e4.getMessage());
                    return false;
                }
            }
        }
    }
}
