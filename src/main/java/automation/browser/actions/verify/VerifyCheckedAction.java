package automation.browser.actions.verify;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Verifies if an element (checkbox, radio) is checked/selected or not.
 */
public class VerifyCheckedAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(VerifyCheckedAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String targetName = plan.getElementName();
        // If element name is null, check plan.getValue() as a fallback for some legacy patterns
        if (targetName == null || targetName.isEmpty()) {
            targetName = plan.getValue();
        }
        
        String actionType = plan.getActionType();
        boolean expectChecked = !"verify_unchecked".equals(actionType) && !"verify_not_selected".equals(actionType);
        
        Locator scope = null;
        if (plan.getRowAnchor() != null) {
            automation.browser.locator.table.TableNavigator navigator = new automation.browser.locator.table.TableNavigator();
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

        java.util.List<String> items = new java.util.ArrayList<>();
        if (targetName != null && targetName.contains(";")) {
            items.addAll(java.util.Arrays.asList(targetName.split(";")));
        } else {
            items.add(targetName);
        }

        boolean allMatched = true;
        java.util.List<String> actualStates = new java.util.ArrayList<>();
        
        for (String itemText : items) {
            String target = itemText.trim();
            if (target.isEmpty()) continue;

            Locator element = locator.waitForSmartElement(target, "checkbox", scope, plan.getFrameAnchor(), true);
            
            if (element != null) {
                // Small wait to allow state change if it was just clicked
                page.waitForTimeout(300);

                boolean isChecked = false;
                String debugInfo = "N/A";
                try {
                    // Try to get some debug info first
                    try {
                        debugInfo = (String) element.evaluate("el => el.tagName + ' id=' + el.id + ' class=' + el.className");
                    } catch (Exception ignored) {}

                    // Use generic, framework-agnostic detection methods
                    if (isMultiValueComponent(element)) {
                        isChecked = element.isVisible();
                    } else if (isSelectableListItem(element)) {
                        isChecked = hasActiveOrSelectedState(element);
                    } else {
                        try {
                            String tagName = (String) element.evaluate("el => el.tagName");
                            if ("INPUT".equalsIgnoreCase(tagName)) {
                                isChecked = element.isChecked();
                            } else {
                                String className = (String) element.evaluate("el => el.className || ''");
                                String ariaChecked = (String) element.evaluate("el => el.getAttribute('aria-checked') || ''");
                                String ariaSelected = (String) element.evaluate("el => el.getAttribute('aria-selected') || ''");
                                isChecked = "true".equals(ariaChecked) || "true".equals(ariaSelected) || (className != null && (className.toLowerCase().contains("active") || className.toLowerCase().contains("selected") || className.toLowerCase().contains("checked")));
                            }
                        } catch (Exception evalEx) {
                            isChecked = element.isVisible();
                        }
                    }
                } catch (Exception e) {
                    if (!expectChecked) {
                        logger.success(" Element '{}' is not checkable - correctly NOT SELECTED", target);
                        actualStates.add(target + ": Unchecked");
                        continue;
                    } else {
                        logger.failure("Element '{}' check failed. Info: {}. Error: {}", target, debugInfo, e.getMessage());
                        actualStates.add(target + ": Error");
                        allMatched = false;
                        continue;
                    }
                }

                actualStates.add(target + ": " + (isChecked ? "Checked" : "Unchecked"));
                if (isChecked == expectChecked) {
                    logger.success(" Element '{}' matches expected state: {}", target, expectChecked ? "CHECKED/SELECTED" : "UNCHECKED/NOT SELECTED");
                } else {
                    logger.error(" Element '{}' state mismatch. Info: {}. Expected: {}, Actual: {}", 
                        target, debugInfo, expectChecked ? "CHECKED/SELECTED" : "UNCHECKED/NOT SELECTED", isChecked ? "CHECKED/SELECTED" : "UNCHECKED/NOT SELECTED");
                    allMatched = false;
                }
            } else {
                 if (!expectChecked) {
                     logger.success(" Element '{}' not found - correctly NOT SELECTED", target);
                     actualStates.add(target + ": Not Found");
                 } else {
                     logger.failure("Element not found for state check: {}", target);
                     actualStates.add(target + ": Not Found");
                     allMatched = false;
                 }
            }
        }
        
        automation.reporting.StepExecutionReport.ValidationResult result = 
            new automation.reporting.StepExecutionReport.ValidationResult()
                .expected(expectChecked ? "CHECKED" : "UNCHECKED")
                .actual(String.join(", ", actualStates))
                .match(allMatched)
                .comparisonType("BOOLEAN")
                .details(allMatched ? "All elements matched expected state" : "One or more elements failed state verification");
        
        plan.setMetadataValue("validation", result);
        
        if (allMatched) {
            logger.section("VALIDATION SUCCESS");
            logger.info("--------------------------------------------------");
            return true;
        } else {
            logger.section("VALIDATION FAILED");
            logger.info("--------------------------------------------------");
            return false;
        }
    }
    
    /**
     * Detects multi-value components (tags, chips, tokens) using generic patterns.
     * Works with: React-Select, Angular Material Chips, Vue Tags Input, vanilla JS implementations
     * 
     * Detection Strategy:
     * 1. Check ARIA role (listbox, combobox) - WCAG standard
     * 2. Check for common class patterns (tag, chip, token)
     * 3. Framework-specific patterns as fallback only
     */
    private boolean isMultiValueComponent(Locator element) {
        try {
            // Strategy 1: Check ARIA role (highest priority - framework-agnostic)
            String role = (String) element.evaluate("el => el.getAttribute('role')");
            if ("listbox".equals(role) || "combobox".equals(role)) {
                return true;
            }
            
            // Strategy 2: Check for generic tag/chip/token patterns
            String classList = (String) element.evaluate("el => el.className || ''");
            if (classList != null && !classList.isEmpty()) {
                String lowerClass = classList.toLowerCase();
                // Generic patterns that work across frameworks
                if (lowerClass.contains("tag") && !lowerClass.contains("stage")) {  // Avoid "stage"
                    return true;
                }
                if (lowerClass.contains("chip")) {
                    return true;
                }
                if (lowerClass.contains("token")) {
                    return true;
                }
                if (lowerClass.contains("badge") && lowerClass.contains("dismiss")) {
                    return true;
                }
                
                // Framework-specific patterns as fallback
                if (lowerClass.contains("multi-value")) {  // React-Select
                    return true;
                }
                if (lowerClass.contains("mat-chip")) {  // Angular Material
                    return true;
                }
                if (lowerClass.contains("auto-complete")) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detects selectable list items using generic patterns.
     * Works with: Bootstrap, Material-UI, Ant Design, Chakra UI, vanilla HTML
     * 
     * Detection Strategy:
     * 1. Check if element is an LI tag
     * 2. Verify it's part of a selectable list context
     */
    private boolean isSelectableListItem(Locator element) {
        try {
            String tagName = (String) element.evaluate("el => el.tagName");
            if (!"LI".equalsIgnoreCase(tagName)) {
                return false;
            }
            
            // Check if this LI has selectable characteristics
            String classList = (String) element.evaluate("el => el.className || ''");
            if (classList != null && !classList.isEmpty()) {
                String lowerClass = classList.toLowerCase();
                // Generic patterns
                if (lowerClass.contains("selectable")) {
                    return true;
                }
                if (lowerClass.contains("clickable")) {
                    return true;
                }
                
                // Common UI framework patterns (as additional indicators)
                if (lowerClass.contains("list-group-item")) {  // Bootstrap
                    return true;
                }
                if (lowerClass.contains("list-item")) {  // Generic pattern
                    return true;
                }
                if (lowerClass.contains("menu-item")) {
                    return true;
                }
            }
            
            // Check if parent UL/OL has selectable attributes
            String parentRole = (String) element.evaluate("el => el.parentElement?.getAttribute('role') || ''");
            if ("listbox".equals(parentRole) || "menu".equals(parentRole)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if an element has active or selected state using generic patterns.
     * Works with all frameworks by checking both ARIA attributes and class names.
     * 
     * Priority: ARIA > Class patterns
     */
    private boolean hasActiveOrSelectedState(Locator element) {
        try {
            // Priority 1: Check ARIA attributes (framework-agnostic standard)
            String ariaSelected = (String) element.evaluate("el => el.getAttribute('aria-selected')");
            if ("true".equals(ariaSelected)) {
                return true;
            }
            
            String ariaChecked = (String) element.evaluate("el => el.getAttribute('aria-checked')");
            if ("true".equals(ariaChecked)) {
                return true;
            }
            
            // Priority 2: Check generic class patterns
            String classList = (String) element.evaluate("el => el.className || ''");
            if (classList != null && !classList.isEmpty()) {
                String lowerClass = classList.toLowerCase();
                return lowerClass.contains("active") ||
                       lowerClass.contains("selected") ||
                       lowerClass.contains("is-active") ||
                       lowerClass.contains("is-selected");
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
