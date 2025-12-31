package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;
import java.net.URI;

/**
 * Consolidated handler for verifying browser-level state like URL and Page Title.
 */
public class VerifyBrowserStateAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyBrowserStateAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        String expectedValue = plan.getValue();
        
        switch (actionType) {
            case "verify_url":
                return verifyUrl(page, expectedValue, plan.getTarget().toLowerCase());
            case "verify_page_title":
                return verifyTitle(page, expectedValue, plan.getTarget().toLowerCase());
            default:
                logger.error("Unknown browser state verification: {}", actionType);
                return false;
        }
    }

    private boolean verifyUrl(Page page, String expectedValue, String stepText) {
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        boolean success = false;
        String currentUrl = "";

        logger.info("Verifying URL: Expected='{}' (with 5s polling)", expectedValue != null ? expectedValue : "homepage");

        while (System.currentTimeMillis() - startTime < timeout) {
            currentUrl = page.url();
            if (checkUrlCriteria(currentUrl, expectedValue, stepText)) {
                success = true;
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }

        if (success) {
            logger.success("URL verification successful: '{}'", currentUrl);
            return true;
        } else {
            logger.failure("URL verification failed: Current URL '{}' did not match criteria '{}' after 5s", currentUrl, expectedValue);
            return false;
        }
    }

    private boolean checkUrlCriteria(String currentUrl, String expectedValue, String stepText) {
        // Shorthand support
        boolean isShorthand = (expectedValue == null || expectedValue.trim().isEmpty()) || 
                             expectedValue.toLowerCase().matches("homepage|base url|root url");

        if (isShorthand) {
            try {
                URI uri = new URI(currentUrl);
                String path = uri.getPath();
                return path == null || path.isEmpty() || path.equals("/");
            } catch (Exception e) { return false; }
        }

        try {
            URI uri = new URI(currentUrl);
            if (stepText.contains("path")) return uri.getPath().equals(expectedValue) || uri.getPath().contains(expectedValue);
            if (stepText.contains("domain") || stepText.contains("host")) return uri.getHost() != null && uri.getHost().contains(expectedValue);
            if (stepText.contains("parameter") || stepText.contains("query")) return uri.getQuery() != null && uri.getQuery().contains(expectedValue);
            if (stepText.contains("hash") || stepText.contains("anchor")) return currentUrl.contains("#" + expectedValue) || currentUrl.endsWith(expectedValue);
            
            // Default behaviors
            if (stepText.contains("starts with")) return currentUrl.startsWith(expectedValue);
            if (stepText.contains("exactly")) return currentUrl.equals(expectedValue);
            return currentUrl.contains(expectedValue);
        } catch (Exception e) { return false; }
    }

    private boolean verifyTitle(Page page, String expectedTitle, String stepText) {
        if (expectedTitle == null || expectedTitle.trim().isEmpty()) {
            logger.failure("Page title verification failed: No expected title specified");
            return false;
        }

        String actualTitle = page.title();
        logger.info("Verifying page title: Actual='{}', Expected='{}'", actualTitle, expectedTitle);

        boolean match = stepText.contains("contains") ? actualTitle.contains(expectedTitle) : actualTitle.equals(expectedTitle);
        
        if (match) {
            logger.success("Page title verification successful: '{}'", expectedTitle);
            return true;
        } else {
            logger.failure("Page title verification failed: Actual title '{}' does not match expected '{}'", actualTitle, expectedTitle);
            return false;
        }
    }
}
