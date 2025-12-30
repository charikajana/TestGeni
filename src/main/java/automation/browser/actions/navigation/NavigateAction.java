package automation.browser.actions.navigation;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;
import java.net.URI;

public class NavigateAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(NavigateAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String value = plan.getValue();
        String stepText = plan.getTarget();
        
        String url = extractUrl(stepText);
        if (url == null && value != null && (value.startsWith("http") || value.startsWith("/"))) url = value;
        
        if (url != null) {
            String finalUrl = resolveUrl(page, url);
            logger.browserAction("Navigate", finalUrl);
            page.navigate(finalUrl);
            logger.success("Navigated to: {}", finalUrl);
        } else {
            // Fallback: Try to use SmartLocator to find the element and click it
            // This handles cases like "Navigate to 'Forms > Practice Form'" where it's a menu path
            logger.info("No URL found, attempting UI-based navigation for: {}", value);
            
            // Handle hierarchical navigation (e.g., Menu > Submenu)
            if (value != null && (value.contains(">") || value.contains("->"))) {
                String separator = value.contains(">") ? ">" : "->";
                String[] parts = value.split(separator);
                boolean allClicked = true;
                
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    logger.info("Navigating hierarchy: clicking '{}'", trimmedPart);
                    
                    // 1. Try SmartLocator
                    com.microsoft.playwright.Locator element = locator.waitForSmartElement(trimmedPart, null);
                    
                    // 2. Fallback: Native text match if SmartLocator missed it (e.g., non-interactive tags)
                    if (element == null) {
                        logger.info("SmartLocator failed, trying native text locator for '{}'", trimmedPart);
                        com.microsoft.playwright.Locator nativeLoc = page.getByText(trimmedPart).first();
                        // Short wait for native locator
                        try {
                            nativeLoc.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(2000));
                            if (nativeLoc.isVisible()) {
                                element = nativeLoc;
                            }
                        } catch (Exception e) {
                            // Native wait failed
                        }
                    }

                    if (element != null) {
                        // Scroll if needed (for elements at bottom of page)
                        element.scrollIntoViewIfNeeded();
                        element.click();
                        // Wait a bit for menu to expand/page to load
                        try { Thread.sleep(500); } catch (Exception e) {}
                    } else {
                        logger.failure("Could not find part of navigation hierarchy: '{}'", trimmedPart);
                        allClicked = false;
                        break;
                    }
                }
                
                if (allClicked) {
                    logger.success("Hierarchical UI navigation successful for: {}", value);
                    return true;
                }
            }
            
            // Default single-element lookup
            if (plan.getElementName() == null) {
                plan.setElementName(value);
            }
            
            com.microsoft.playwright.Locator element = locator.findSmartElement(plan.getElementName(), null);
            if (element != null) {
                logger.info("Found UI element for navigation, clicking...");
                element.click();
                logger.success("UI-based navigation successful for: {}", value);
                return true;
            }
            
            logger.error("No valid URL or UI element found for navigation in step: {}", stepText);
            return false;
        }
        return true;
    }

    private String extractUrl(String stepText) {
        // Try to find a quoted string first
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\"']([^\"']+)[\"']").matcher(stepText);
        if (m.find()) {
            String val = m.group(1);
            if (val.startsWith("http") || val.startsWith("/")) return val;
        }

        // Fallback to searching for http or / at start of a word
        if (stepText.contains("http")) {
            int start = stepText.indexOf("http");
            int end = stepText.indexOf(" ", start);
            if (end == -1) end = stepText.length();
            return stepText.substring(start, end).replace("\"", "").replace("'", "");
        }
        
        if (stepText.contains(" /")) {
            int start = stepText.indexOf(" /") + 1;
            int end = stepText.indexOf(" ", start);
            if (end == -1) end = stepText.length();
            return stepText.substring(start, end).replace("\"", "").replace("'", "");
        }
        
        return null;
    }

    private String resolveUrl(Page page, String url) {
        if (url.startsWith("http")) return url;
        if (url.startsWith("/")) {
            try {
                String current = page.url();
                if (current != null && current.startsWith("http")) {
                    URI uri = new URI(current);
                    return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "") + url;
                }
            } catch (Exception e) {}
        }
        return url;
    }
}
