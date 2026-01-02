package automation.browser.actions.click;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import automation.browser.locator.table.TableNavigator;

public class ClickAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(ClickAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        
        // 1. Try to use intelligent locator if already found during planning
        if (plan.hasMetadata("intelligent_locator")) {
            Locator intelligentLocator = (Locator) plan.getMetadataValue("intelligent_locator");
            if (intelligentLocator != null) {
                logger.debug("Using pre-resolved intelligent locator for: {}", targetName);
                return performClick(intelligentLocator, targetName);
            }
        }

        // 2. Handle row-based scope
        Locator scope = null;
        if (plan.getRowAnchor() != null) {
             TableNavigator navigator = new TableNavigator();
             
             if (plan instanceof automation.planner.EnhancedActionPlan) {
                 automation.planner.EnhancedActionPlan enhanced = (automation.planner.EnhancedActionPlan) plan;
                 String columnName = enhanced.getRowConditionColumn();
                 String columnValue = enhanced.getRowConditionValue();
                 
                 if (columnName != null && columnValue != null) {
                     scope = navigator.findRowByColumnValue(page, columnName, columnValue);
                 } else {
                     scope = navigator.findRowByAnchor(page, plan.getRowAnchor());
                 }
             } else {
                 scope = navigator.findRowByAnchor(page, plan.getRowAnchor());
             }
             
             if (scope == null) {
                  logger.failure("Row not found for anchor: {}", plan.getRowAnchor());
                  return false;
              }
         }
        
        // 3. Find element using SmartLocator
        Locator clickable = locator.waitForSmartElement(targetName, "button", scope, plan.getFrameAnchor());
        
        if (clickable != null) {
            locator.recordMatch(plan);
            return performClick(clickable, targetName);
        }
        
        // 4. If element not found, check if it might be in an autocomplete dropdown
        logger.debug("Element not found normally, checking autocomplete context...");
        if (tryClickAutocompleteOption(page, targetName)) {
            return true;
        }
        
        logger.failure("Element not found for clicking: {}", targetName);
        return false;
    }

    /**
     * Internal helper to perform the click with robust fallback
     */
    private boolean performClick(Locator clickable, String targetName) {
        try {
            String tagName = (String) clickable.evaluate("el => el.tagName.toLowerCase()");
            String type = (String) clickable.evaluate("el => el.type");

            if ("input".equals(tagName) && ("radio".equals(type) || "checkbox".equals(type))) {
                logger.debug("Target is input[type={}], using force click", type);
                clickable.click(new Locator.ClickOptions().setForce(true));
            } else {
                clickable.click();
            }
            logger.browserAction("Click", targetName);
            return true;
        } catch (Exception e) {
            try {
                logger.debug("Standard click failed, trying force click: {}", e.getMessage());
                clickable.click(new Locator.ClickOptions().setForce(true));
                logger.browserAction("Force Click", targetName);
                return true;
            } catch (Exception e2) {
                logger.failure("Failed to click element: {}. Error: {}", targetName, e2.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Try to click an option in an autocomplete dropdown.
     * This method assumes an autocomplete dropdown is already open and visible.
     * 
     * @param page The page object
     * @param optionText The text of the option to click
     * @return true if successfully clicked, false otherwise
     */
    private boolean tryClickAutocompleteOption(Page page, String optionText) {
        try {
            logger.debug("Searching for '{}' in autocomplete suggestions...", optionText);
            
            Locator option = null;
            
            // Pattern 1: React-Select autocomplete options
            option = page.locator(String.format("div[id*='option']:has-text(\"%s\")", optionText)).first();
            if (option.count() > 0 && option.isVisible()) {
                logger.debug("Found autocomplete option using React-Select pattern");
                option.click();
                logger.success("Clicked '{}' in autocomplete dropdown", optionText);
                return true;
            }
            
            // Pattern 2: ARIA role option (standard autocomplete)
            option = page.getByRole(com.microsoft.playwright.options.AriaRole.OPTION,
                    new Page.GetByRoleOptions().setName(optionText)).first();
            if (option.count() > 0 && option.isVisible()) {
                logger.debug("Found autocomplete option using ARIA role");
                option.click();
                logger.success("Clicked '{}' in autocomplete dropdown", optionText);
                return true;
            }
            
            // Pattern 3: Generic dropdown option with class
            option = page.locator(String.format("div[class*='option']:has-text(\"%s\")", optionText)).first();
            if (option.count() > 0 && option.isVisible()) {
                logger.debug("Found autocomplete option using generic class pattern");
                option.click();
                logger.success("Clicked '{}' in autocomplete dropdown", optionText);
                return true;
            }
            
            // Pattern 4: List item in dropdown
            option = page.locator(String.format("li[role='option']:has-text(\"%s\")", optionText)).first();
            if (option.count() > 0 && option.isVisible()) {
                logger.debug("Found autocomplete option using list pattern");
                option.click();
                logger.success("Clicked '{}' in autocomplete dropdown", optionText);
                return true;
            }
            
            logger.debug("Option '{}' not found in autocomplete suggestions", optionText);
            return false;
            
        } catch (Exception e) {
            logger.debug("Error while searching autocomplete: {}", e.getMessage());
            return false;
        }
    }
}
