package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class VerifyNotTextAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyNotTextAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String value = plan.getValue();
        String targetName = plan.getElementName();
        String textToNotSee = (value != null && !value.isEmpty()) ? value : targetName;
        
        if (textToNotSee == null) {
            logger.failure("Verification failed - No text specified");
            return false;
        }

        // Handle Frame Scoping
        String frameAnchor = plan.getFrameAnchor();
        if (frameAnchor != null) {
            com.microsoft.playwright.Frame frame = locator.findFrame(frameAnchor);
            if (frame != null) {
                logger.debug("Scoping negative verification to iframe: '{}'", frameAnchor);
                return checkAbsence(frame.getByText(textToNotSee).first(), textToNotSee);
            }
        }

        // Standard check
        boolean absent = checkAbsence(page.getByText(textToNotSee).first(), textToNotSee);
        return absent;
    }

    private boolean checkAbsence(Locator loc, String textToNotSee) {
        try {
            // Assert HIDDEN (or not visible)
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(loc).isHidden(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsHiddenOptions().setTimeout(5000)
            );

            logger.section("VALIDATION SUCCESS (NEGATIVE)");
            logger.info(" Expected NOT Visible: {}", textToNotSee);
            logger.info(" Actual UI: Element is hidden/absent");
            logger.info("--------------------------------------------------");
            return true;
        } catch (Error e) {
            logger.section("VALIDATION FAILED (NEGATIVE)");
            logger.error(" ERROR: Text '{}' is VISIBLE when it should NOT be!", textToNotSee);
            logger.info("--------------------------------------------------");
            return false;
        }
    }
}
