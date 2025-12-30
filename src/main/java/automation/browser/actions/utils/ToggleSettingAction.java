package automation.browser.actions.utils;

import automation.browser.SmartLocator;
import automation.browser.actions.BrowserAction;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Toggles settings, checkboxes, or switches.
 * Example: "disable Travel Policy In Agency Admin"
 * 
 * Strategy:
 * 1. Navigate to context/section if specified
 * 2. Find the setting checkbox/toggle
 * 3. Set to desired state (enabled/disabled)
 */
public class ToggleSettingAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(ToggleSettingAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator smartLocator, ActionPlan plan) {
        String settingName = plan.getElementName();
        String action = plan.getValue();  // "enable" or "disable"
        
        boolean shouldEnable = "enable".equalsIgnoreCase(action);
        
        logger.info("Toggle Setting -> {} '{}' (target state: {})", 
            action, settingName, shouldEnable ? "ON" : "OFF");
        
        try {
            // Find the setting checkbox/toggle
            Locator setting = findSetting(page, smartLocator, settingName);
            
            if (setting == null) {
                logger.error("Could not find setting: {}", settingName);
                return false;
            }
            
            // Check current state
            boolean currentlyChecked = isChecked(setting);
            
            logger.debug("Current state: {}, Target state: {}", 
                currentlyChecked ? "ON" : "OFF", 
                shouldEnable ? "ON" : "OFF");
            
            // Only toggle if state needs to change
            if (currentlyChecked != shouldEnable) {
                setting.click();
                page.waitForTimeout(300);  // Wait for toggle animation
                
                // Verify the change
                boolean newState = isChecked(setting);
                if (newState == shouldEnable) {
                    logger.success("SUCCESS: {} '{}' (now {})", 
                        action, settingName, newState ? "ON" : "OFF");
                    return true;
                } else {
                    logger.error("Toggle failed: state did not change as expected");
                    return false;
                }
            } else {
                logger.info("Setting '{}' already in desired state ({})", 
                    settingName, shouldEnable ? "ON" : "OFF");
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Failed to toggle setting: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Find the setting checkbox/toggle
     */
    private Locator findSetting(Page page, SmartLocator smartLocator, String settingName) {
        // Strategy 1: Try SmartLocator with checkbox type
        Locator bySmartLocator = smartLocator.findSmartElement(settingName, "checkbox", null, null);
        if (bySmartLocator != null && bySmartLocator.count() > 0) {
            logger.debug("Found setting via SmartLocator");
            return bySmartLocator;
        }
        
        // Strategy 2: Find by label text with associated checkbox
        Locator byLabel = page.locator(
            String.format("label:has-text('%s')", settingName)
        ).first();
        
        if (byLabel.count() > 0) {
            // Try to find associated checkbox
            try {
                // Check for 'for' attribute
                String forId = byLabel.getAttribute("for");
                if (forId != null && !forId.isEmpty()) {
                    Locator checkbox = page.locator("#" + forId);
                    if (checkbox.count() > 0) {
                        logger.debug("Found setting via label[for]");
                        return checkbox;
                    }
                }
                
                // Check for nested checkbox
                Locator nestedCheckbox = byLabel.locator("input[type='checkbox']").first();
                if (nestedCheckbox.count() > 0) {
                    logger.debug("Found setting via nested checkbox");
                    return nestedCheckbox;
                }
                
                // The label itself might be clickable
                logger.debug("Using label as clickable element");
                return byLabel;
                
            } catch (Exception e) {
                logger.debug("Label strategy failed: {}", e.getMessage());
            }
        }
        
        // Strategy 3: Find checkbox near text
        Locator byText = page.locator(
            String.format("*:has-text('%s') input[type='checkbox']", settingName)
        ).first();
        
        if (byText.count() > 0) {
            logger.debug("Found setting via text proximity");
            return byText;
        }
        
        return null;
    }
    
    /**
     * Check if checkbox/toggle is currently checked
     */
    private boolean isChecked(Locator element) {
        try {
            // Check for checkbox
            if (element.getAttribute("type") != null && 
                element.getAttribute("type").equals("checkbox")) {
                return element.isChecked();
            }
            
            // Check for aria-checked
            String ariaChecked = element.getAttribute("aria-checked");
            if ("true".equals(ariaChecked)) {
                return true;
            }
            
            // Check for checked attribute
            String checked = element.getAttribute("checked");
            if (checked != null) {
                return true;
            }
            
            // Check for active/selected classes
            String className = element.getAttribute("class");
            if (className != null) {
                return className.contains("active") || 
                       className.contains("checked") || 
                       className.contains("selected");
            }
            
            return false;
            
        } catch (Exception e) {
            logger.debug("Could not determine checked state: {}", e.getMessage());
            return false;
        }
    }
}
