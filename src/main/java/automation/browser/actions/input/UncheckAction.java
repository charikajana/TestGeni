package automation.browser.actions.input;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class UncheckAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(UncheckAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        Locator uncheckbox = locator.waitForSmartElement(targetName, "checkbox", null, plan.getFrameAnchor(), true);
        if (uncheckbox != null) {
            try {
                // Try to uncheck it directly (forced for hidden elements)
                uncheckbox.uncheck(new Locator.UncheckOptions().setForce(true).setTimeout(5000));
                logger.browserAction("Uncheck", targetName);
                return true;
            } catch (Exception e) {
                logger.debug("Forced uncheck failed for '{}', trying forced click", targetName);
                try {
                    uncheckbox.click(new Locator.ClickOptions().setForce(true).setTimeout(2000));
                    logger.browserAction("Uncheck (Fallback Click)", targetName);
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
                            logger.browserAction("Uncheck (Text Click: " + candidate + ")", targetName);
                            return true;
                        } catch (Exception ignored) {}
                    }
                    
                    // Final attempt: Click parent label if it exists
                    try {
                        uncheckbox.locator("xpath=ancestor::label").first().click(new Locator.ClickOptions().setTimeout(2000));
                        logger.browserAction("Uncheck (Parent Label Click)", targetName);
                        return true;
                    } catch (Exception e4) {
                        logger.failure("Could not uncheck or click element '{}': {}", targetName, e4.getMessage());
                        return false;
                    }
                }
            }
        }
 else {
            logger.failure("Checkbox not found: {}", targetName);
            return false;
        }
    }
}
