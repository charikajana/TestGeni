package automation.browser.locator.builders;

import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.HashMap;
import java.util.Map;

/**
 * Dynamically detects dropdown type based on capabilities, not frameworks
 * Works with: Native HTML, ARIA-compliant dropdowns, and custom implementations
 * Framework-agnostic: Supports React, Angular, Vue, Svelte, Bootstrap, etc.
 */
public class DynamicDropdownXPathBuilder {

    private static final LoggerUtil logger = LoggerUtil.getLogger(DynamicDropdownXPathBuilder.class);

    /**
     * Dropdown categories based on capabilities, not framework names
     */
    public enum DropdownType {
        NATIVE_SELECT,      // Standard HTML <select> element
        ARIA_COMPLIANT,     // Uses ARIA roles (role='combobox', role='option')
        CUSTOM_DROPDOWN     // Custom implementation (div-based, requires detection)
    }

    /**
     * Analyze the dropdown wrapper and determine its type based on capabilities
     */
    public Map<String, Object> analyzeDropdown(Page page, Locator dropdownWrapper) {
        Map<String, Object> metadata = new HashMap<>();
        
        try {
            String tagName = (String) dropdownWrapper.evaluate("el => el.tagName.toLowerCase()");
            String className = (String) dropdownWrapper.evaluate("el => el.className || ''");
            String role = (String) dropdownWrapper.evaluate("el => el.getAttribute('role') || ''");
            String dataTestId = (String) dropdownWrapper.evaluate("el => el.getAttribute('data-testid') || ''");
            
            metadata.put("tagName", tagName);
            metadata.put("className", className);
            metadata.put("role", role);
            
            DropdownType type = detectType(tagName, className, role, dataTestId);
            metadata.put("type", type);
            
            // Store framework hint for logging only (not used in logic)
            String frameworkHint = detectFrameworkHint(className);
            metadata.put("frameworkHint", frameworkHint);
            
            logger.debug("Dropdown Analysis:");
            logger.debug("   Tag: {}", tagName);
            logger.debug("   Class: {}", className);
            logger.debug("   Role: {}", role);
            logger.debug("   Detected Type: {}", type);
            if (!frameworkHint.isEmpty()) {
                logger.debug("   Framework Hint: {} (for logging only)", frameworkHint);
            }
            
            return metadata;
        } catch (Exception e) {
            logger.error("Failed to analyze dropdown: {}", e.getMessage());
            metadata.put("type", DropdownType.CUSTOM_DROPDOWN);
            return metadata;
        }
    }

    /**
     * Detect dropdown type based on CAPABILITIES, not framework names
     * Priority: Native HTML > ARIA-compliant > Custom
     */
    private DropdownType detectType(String tag, String className, String role, String dataTestId) {
        // Priority 1: Native HTML <select>
        if ("select".equals(tag)) {
            return DropdownType.NATIVE_SELECT;
        }
        
        // Priority 2: ARIA-compliant (uses standard roles)
        if ("combobox".equals(role) || "listbox".equals(role) || "button".equals(role)) {
            return DropdownType.ARIA_COMPLIANT;
        }
        
        // Priority 3: Custom dropdown (div-based, any framework)
        return DropdownType.CUSTOM_DROPDOWN;
    }
    
    /**
     * Detect framework for logging purposes only (not used in selection logic)
     * This helps developers understand what framework is being used
     */
    private String detectFrameworkHint(String className) {
        if (className.contains("react-select")) return "React-Select";
        if (className.contains("MuiSelect") || className.contains("Mui")) return "Material-UI";
        if (className.contains("ant-select")) return "Ant Design";
        if (className.contains("chakra")) return "Chakra UI";
        if (className.contains("vue-select") || className.contains("v-select")) return "Vue-Select";
        if (className.contains("ng-select")) return "Angular ng-select";
        if (className.contains("dropdown") || className.contains("form-select")) return "Bootstrap (or generic)";
        return "";
    }

    /**
     * Build XPath to find the actual interactive element based on capabilities
     */
    public String buildDropdownTriggerXPath(Locator wrapper, DropdownType type) {
        switch (type) {
            case NATIVE_SELECT:
                // The wrapper itself is the <select> element
                return ".";
                
            case ARIA_COMPLIANT:
                // ARIA-compliant: look for elements with standard roles
                return ".//*[@role='button' or @role='combobox' or @role='listbox']";
                
            case CUSTOM_DROPDOWN:
            default:
                // Custom: try common patterns (input for searchable, or clickable div)
                return ".//*[@role='combobox'] | .//input | .//button | .//*[contains(@class, 'control')]";
        }
    }

    /**
     * Build XPath to find dropdown options (framework-agnostic)
     * Priority: ARIA role > Native option > Generic patterns
     */
    public String buildOptionsXPath(DropdownType type, String optionText) {
        switch (type) {
            case NATIVE_SELECT:
                // Native <select>: look for <option> elements
                return String.format("//option[text()='%s' or @value='%s']", optionText, optionText);
                
            case ARIA_COMPLIANT:
            case CUSTOM_DROPDOWN:
            default:
                // ARIA / Custom: Use generic pattern that works across all frameworks
                // Priority: role='option' > generic classes > list items
                return String.format(
                    "//*[@role='option' and contains(., '%s')] | " +
                    "//*[contains(@class, 'option') and contains(., '%s')] | " +
                    "//*[contains(@class, 'item') and contains(., '%s')] | " +
                    "//li[contains(., '%s')]",
                    optionText, optionText, optionText, optionText
                );
        }
    }

    /**
     * Get interaction strategy based on dropdown capabilities (not framework)
     */
    public InteractionStrategy getInteractionStrategy(DropdownType type) {
        switch (type) {
            case NATIVE_SELECT:
                // Native <select>: Use Playwright's built-in selectOption()
                return new InteractionStrategy(false, "selectOption");
                
            case ARIA_COMPLIANT:
            case CUSTOM_DROPDOWN:
            default:
                // All custom dropdowns: Click to open, then click option
                return new InteractionStrategy(true, "clickAndSelect");
        }
    }

    /**
     * Interaction strategy metadata
     */
    public static class InteractionStrategy {
        public final boolean requiresClick;
        public final String method;

        public InteractionStrategy(boolean requiresClick, String method) {
            this.requiresClick = requiresClick;
            this.method = method;
        }
    }
}
