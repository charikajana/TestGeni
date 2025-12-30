package automation.browser.actions.window;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ClickAndSwitchAction implements BrowserAction {
    private static final LoggerUtil logger = LoggerUtil.getLogger(ClickAndSwitchAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String elementName = plan.getElementName();
        logger.info("Attempting to click '{}' and switch to new window...", elementName);

        try {
            // SmartLocator now automatically extracts type hints from natural language
            // E.g., "Home link" will be parsed as name="Home", type="link"
            Locator element = locator.findSmartElement(elementName, "button"); 
            
            if (element == null) {
                logger.failure("Element '{}' not found for click-and-switch", elementName);
                return false;
            }

            logger.info("Waiting for popup triggered by clicking '{}'...", elementName);
            
            // Use waitForPopup to capture the new window reliably
            Page newPage = page.waitForPopup(() -> {
                try {
                    // Attempt click, scrolling if necessary
                    element.scrollIntoViewIfNeeded();
                    element.click();
                } catch (Exception e) {
                    logger.error("Error clicking element: {}", e.getMessage());
                    throw e;
                }
            });
            
            if (newPage != null) {
                newPage.waitForLoadState();
                newPage.bringToFront();
                logger.success("Successfully clicked '{}' and switched to new window", elementName);
                logger.info("New window URL: {}", newPage.url());
                return true;
            } else {
                logger.failure("Click performed but no new window detected within timeout");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during click-and-switch: {}", e.getMessage());
            return false;
        }
    }
}
