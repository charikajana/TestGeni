package automation.browser.actions.input;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class CheckAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(CheckAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        Locator checkbox = locator.waitForSmartElement(targetName, "checkbox", null, plan.getFrameAnchor(), true);
        if (checkbox != null) {
            try {
                // Try to check it directly (forced for hidden elements)
                checkbox.check(new Locator.CheckOptions().setForce(true).setTimeout(5000));
                logger.browserAction("Check", targetName);
                return true;
            } catch (Exception e) {
                logger.debug("Forced check failed for '{}', trying forced click", targetName);
                try {
                    checkbox.click(new Locator.ClickOptions().setForce(true).setTimeout(2000));
                    logger.browserAction("Check (Fallback Click)", targetName);
                    return true;
                } catch (Exception e2) {
                    logger.debug("Forced click failed, trying to find visible label/text for '{}'", targetName);
                    
                    // Try to find by text - if targetName is "Home Check box", try "Home Check box" then "Home"
                    String[] candidates = {
                        targetName,
                        targetName.replaceAll("(?i)\\s*(?:check\\s*box|checkbox)$", "").trim()
                    };

                    for (String candidate : candidates) {
                        try {
                            page.getByText(candidate, new Page.GetByTextOptions().setExact(false)).first().click(new Locator.ClickOptions().setTimeout(2000));
                            logger.browserAction("Check (Text Click: " + candidate + ")", targetName);
                            return true;
                        } catch (Exception ignored) {}
                    }
                    
                    // Final attempt: Click parent label if it exists
                    try {
                        checkbox.locator("xpath=ancestor::label").first().click(new Locator.ClickOptions().setTimeout(2000));
                        logger.browserAction("Check (Parent Label Click)", targetName);
                        return true;
                    } catch (Exception e4) {
                        logger.failure("Could not check or click element '{}': {}", targetName, e4.getMessage());
                        return false;
                    }
                }
            }
        } else {
            logger.failure("Checkbox not found: {}", targetName);
            return false;
        }
    }
}
