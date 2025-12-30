package automation.browser.actions.navigation;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Action to select a nested menu item by hovering through the hierarchy.
 * Pattern: Select "Parent > Child > GrandChild" from menu
 */
public class SelectMenuAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SelectMenuAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String menuPath = plan.getValue();
        if (menuPath == null || menuPath.isEmpty()) {
            menuPath = plan.getElementName();
        }
        
        if (menuPath == null || menuPath.isEmpty()) {
            logger.failure("No menu path specified for selection");
            return false;
        }
        
        logger.info("Selecting menu path: {}", menuPath);
        
        // Split by >, |, →, ->, =>, /, then, under, to
        String[] items = menuPath.split("\\s*(?:[>|→]|->|=>|/|then|under)\\s+|\\s+to\\s+");
        
        for (int i = 0; i < items.length; i++) {
            String itemName = items[i].trim();
            logger.debug("Processing menu level {}: '{}'", i + 1, itemName);
            
            // For the last item, we click. For others, we hover.
            boolean isLast = (i == items.length - 1);
            
            // Find the element. 
            // We use includeHidden=true because sub-menus are usually in the DOM but hidden until hover.
            Locator itemLocator = smartLocator.waitForSmartElement(itemName, null, null, null, true);
            
            if (itemLocator == null) {
                logger.failure("Could not find menu item: '{}' in path: {}", itemName, menuPath);
                return false;
            }
            
            try {
                if (isLast) {
                    logger.debug("Clicking final menu item: {}", itemName);
                    try {
                        // 1. Standard click
                        itemLocator.click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(3000));
                    } catch (Exception e) {
                        try {
                            // 2. Force click
                            logger.debug("Standard click failed for '{}', trying force click...", itemName);
                            itemLocator.click(new com.microsoft.playwright.Locator.ClickOptions().setForce(true).setTimeout(3000));
                        } catch (Exception e2) {
                            // 3. JS click (Ultimate fallback)
                            logger.debug("Force click failed for '{}', trying JS click...", itemName);
                            itemLocator.evaluate("el => el.click()");
                        }
                    }
                    logger.success("Successfully selected menu item: {}", menuPath);
                } else {
                    logger.debug("Hovering over menu item: {}", itemName);
                    try {
                        // Try standard hover first
                        itemLocator.hover(new Locator.HoverOptions().setTimeout(3000));
                    } catch (Exception e) {
                        logger.debug("Standard hover failed for '{}', trying JS dispatch...", itemName);
                        itemLocator.evaluate("el => {" +
                            "el.dispatchEvent(new MouseEvent('mouseover', {bubbles: true}));" +
                            "el.dispatchEvent(new MouseEvent('mouseenter', {bubbles: true}));" +
                            "el.dispatchEvent(new MouseEvent('mousemove', {bubbles: true}));" +
                        "}");
                    }
                    // Small wait for sub-menu to appear and stabilize
                    Thread.sleep(800);
                }
            } catch (Exception e) {
                logger.failure("Failed to interact with menu item '{}': {}", itemName, e.getMessage());
                return false;
            }
        }
        
        return true;
    }
}
