package automation.browser.actions.verify;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;

import java.net.URI;

/**
 * Action handler for verifying the current browser URL with automatic polling
 */
public class VerifyURLAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyURLAction.class);

    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String expectedValue = plan.getValue();
        String stepText = plan.getTarget().toLowerCase();
        
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout
        boolean success = false;
        String currentUrl = "";

        logger.info("Verifying URL: Expected='{}' (with 5s polling)", expectedValue != null ? expectedValue : "homepage");

        while (System.currentTimeMillis() - startTime < timeout) {
            currentUrl = page.url();
            
            if (checkUrl(currentUrl, expectedValue, stepText)) {
                success = true;
                break;
            }
            
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }

        if (success) {
            logger.success("URL verification successful: '{}'", currentUrl);
            return true;
        } else {
            logger.failure("URL verification failed: Current URL '{}' did not match expected criteria '{}' after 5s", currentUrl, expectedValue);
            return false;
        }
    }

    private boolean checkUrl(String currentUrl, String expectedValue, String stepText) {
        // Handle shorthand verifications like "homepage" or "base URL"
        boolean isShorthand = (expectedValue == null || expectedValue.trim().isEmpty()) || 
                             expectedValue.toLowerCase().equals("homepage") || 
                             expectedValue.toLowerCase().equals("base url") || 
                             expectedValue.toLowerCase().equals("root url");

        if (isShorthand) {
            if (stepText.contains("homepage") || stepText.contains("base url") || stepText.contains("root url") || 
                (expectedValue != null && (expectedValue.toLowerCase().contains("homepage") || expectedValue.toLowerCase().contains("base url")))) {
                try {
                    URI uri = new URI(currentUrl);
                    String path = uri.getPath();
                    return path == null || path.isEmpty() || path.equals("/");
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        try {
            if (stepText.contains("path")) {
                String path = new URI(currentUrl).getPath();
                return path.equals(expectedValue) || path.contains(expectedValue);
            } else if (stepText.contains("domain") || stepText.contains("host")) {
                String host = new URI(currentUrl).getHost();
                return host != null && host.contains(expectedValue);
            } else if (stepText.contains("parameter") || stepText.contains("query")) {
                String query = new URI(currentUrl).getQuery();
                return query != null && query.contains(expectedValue);
            } else if (stepText.contains("hash") || stepText.contains("anchor") || stepText.contains("fragment")) {
                String fragment = new URI(currentUrl).getFragment();
                String targetFragment = expectedValue.startsWith("#") ? expectedValue.substring(1) : expectedValue;
                return fragment != null && fragment.contains(targetFragment);
            } else if (stepText.contains("starts with")) {
                return currentUrl.startsWith(expectedValue);
            } else if (stepText.contains("exactly") || stepText.contains("is exactly")) {
                return currentUrl.equals(expectedValue);
            } else {
                // Default: contains
                return currentUrl.contains(expectedValue);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
