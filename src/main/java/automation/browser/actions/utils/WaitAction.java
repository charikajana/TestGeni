package automation.browser.actions.utils;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;

public class WaitAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(WaitAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String actionType = plan.getActionType();
        
        try {
            switch (actionType) {
                case "wait_time":
                    return handleTimeWait(plan);
                    
                case "wait_page":
                    return handlePageLoadWait(page);
                    
                case "wait_appear":
                    return handleElementAppearWait(page, locator, plan);
                    
                case "wait_disappear":
                    return handleElementDisappearWait(page, locator, plan);
                    
                default:
                    // Fallback for generic "wait" action
                    return handlePageLoadWait(page);
            }
        } catch (Exception e) {
            logger.failure("Wait action failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle time-based waits: "wait for 20 seconds", "wait 5 sec"
     */
    private boolean handleTimeWait(ActionPlan plan) {
        try {
            // Element name contains the number, Value contains the unit
            String durationStr = plan.getElementName();
            String unitStr = plan.getValue();
            
            if (durationStr == null || durationStr.trim().isEmpty()) {
                logger.failure("No duration specified for time wait");
                return false;
            }
            
            long duration = Long.parseLong(durationStr.trim());
            long millis;
            
            // Normalize unit
            String unit = (unitStr != null) ? unitStr.toLowerCase() : "s";
            if (unit.startsWith("m")) { // minute, min, m
                millis = duration * 60 * 1000L;
                logger.waiting((int) (duration * 60)); // Log as seconds for consistency if logger expects int
            } else { // second, sec, s
                millis = duration * 1000L;
                logger.waiting((int) duration);
            }
            
            Thread.sleep(millis);
            logger.success("Wait complete ({} {})", duration, unit);
            return true;
        } catch (NumberFormatException e) {
            logger.failure("Invalid wait duration: {}", plan.getElementName());
            return false;
        } catch (InterruptedException e) {
            logger.failure("Wait interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Handle page load waits: "wait for page load", "wait for page to be loaded"
     */
    private boolean handlePageLoadWait(Page page) {
        try {
            logger.info("WAIT: Waiting for page to load (DOM ready)...");
            // Use DOMCONTENTLOADED instead of NETWORKIDLE (more reliable)
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
            Thread.sleep(2000); // Additional wait for dynamic content
            logger.success("Page loaded successfully");
            return true;
        } catch (Exception e) {
            try {
                Thread.sleep(2000);
                logger.success("Page load wait complete (fallback)");
                return true;
            } catch (InterruptedException ie) {
                return false;
            }
        }
    }
    
    /**
     * Handle element appearance waits: "wait for 'Submit' to appear"
     */
    private boolean handleElementAppearWait(Page page, SmartLocator locator, ActionPlan plan) {
        String elementName = plan.getElementName();
        if (elementName == null || elementName.trim().isEmpty()) {
            logger.failure("No element name specified for appearance wait");
            return false;
        }
        
        logger.info("WAIT: Waiting for '{}' to appear...", elementName);
        
        try {
            // Use the smart locator to find and wait for the element
            Locator element = locator.waitForSmartElement(elementName, "button", null, plan.getFrameAnchor()); // Use generic type
            
            // Additional check to ensure it's visible
            element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(30000));
            
            logger.success("Element '{}' is now visible", elementName);
            return true;
        } catch (Exception e) {
            logger.failure("Element '{}' did not appear: {}", elementName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle element disappearance waits: "wait for 'Loading' to disappear"
     */
    private boolean handleElementDisappearWait(Page page, SmartLocator locator, ActionPlan plan) {
        String elementName = plan.getElementName();
        if (elementName == null || elementName.trim().isEmpty()) {
            logger.failure("No element name specified for disappearance wait");
            return false;
        }
        
        logger.info("WAIT: Waiting for '{}' to disappear...", elementName);
        
        try {
            // Try to find the element using smart locator
            Locator element = locator.findSmartElement(elementName, "button"); // Use generic type
            
            // Wait for it to be hidden (with timeout)
            element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(30000));
            
            logger.success("Element '{}' has disappeared", elementName);
            return true;
        } catch (Exception e) {
            // If element is not found initially, it's already gone - success
            logger.success("Element '{}' is not present (already gone)", elementName);
            return true;
        }
    }
}
