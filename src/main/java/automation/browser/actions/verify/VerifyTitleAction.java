package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

/**
 * Action handler for verifying the current page title
 */
public class VerifyTitleAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyTitleAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String expectedTitle = plan.getValue();
        String actualTitle = page.title();
        String stepText = plan.getTarget().toLowerCase();
        
        if (expectedTitle == null || expectedTitle.trim().isEmpty()) {
            logger.failure("Page title verification failed: No expected title specified");
            return false;
        }

        logger.info("Verifying page title: Actual='{}', Expected='{}'", actualTitle, expectedTitle);

        boolean match = false;
        if (stepText.contains("contains")) {
            match = actualTitle.contains(expectedTitle);
        } else {
            // Default: exact match
            match = actualTitle.equals(expectedTitle);
        }

        if (match) {
            logger.success("Page title verification successful: '{}'", expectedTitle);
            return true;
        } else {
            logger.failure("Page title verification failed: Actual title '{}' does not match expected '{}'", actualTitle, expectedTitle);
            return false;
        }
    }
}
