package automation.browser.locator.cache;

import com.microsoft.playwright.Locator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comprehensive locator extraction utility.
 * Captures ALL possible locator strategies from a UI element.
 * 
 * Strategies captured (in priority order):
 * 1. ID
 * 2. data-testid
 * 3. data-test  
 * 4. data-cy (Cypress)
 * 5. name
 * 6. aria-label
 * 7. aria-labelledby
 * 8. role + accessible name
 * 9. placeholder
 * 10. title
 * 11. alt (for images)
 * 12. value (for inputs)
 * 13. text content
 * 14. CSS class (if stable)
 * 15. CSS selector (custom)
 * 16. Relative XPath
 * 17. Link text (for anchors)
 * 
 * @author Chari
 * @version 2.0
 */
public class LocatorExtractor {
    
    /**
     * Extract all possible locator strategies from an element
     * Returns a LinkedHashMap to maintain priority order
     * 
     * @param locator The Playwright Locator
     * @return Map of strategy -> selector (ordered by priority)
     */
    public static Map<String, String> extractAllLocators(Locator locator) {
        Map<String, String> allLocators = new LinkedHashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> extractedData = (Map<String, Object>) locator.first().evaluate("""
                element => {
                    const result = {};
                    
                    // 1. ID - Highest priority
                    if (element.id) {
                        result.id = '#' + element.id;
                    }
                    
                    // 2. data-testid (common in React/modern frameworks)
                    if (element.hasAttribute('data-testid')) {
                        result['data-testid'] = '[data-testid="' + element.getAttribute('data-testid') + '"]';
                    }
                    
                    // 3. data-test (alternative)
                    if (element.hasAttribute('data-test')) {
                        result['data-test'] = '[data-test="' + element.getAttribute('data-test') + '"]';
                    }
                    
                    // 4. data-cy (Cypress convention)
                    if (element.hasAttribute('data-cy')) {
                        result['data-cy'] = '[data-cy="' + element.getAttribute('data-cy') + '"]';
                    }
                    
                    // 5. name attribute (forms)
                    if (element.name) {
                        result.name = '[name="' + element.name + '"]';
                    }
                    
                    // 6. aria-label (accessibility)
                    if (element.hasAttribute('aria-label')) {
                        result['aria-label'] = '[aria-label="' + element.getAttribute('aria-label') + '"]';
                    }
                    
                    // 7. aria-labelledby (accessibility)
                    if (element.hasAttribute('aria-labelledby')) {
                        result['aria-labelledby'] = '[aria-labelledby="' + element.getAttribute('aria-labelledby') + '"]';
                    }
                    
                    // 8. role + accessible name (ARIA)
                    if (element.hasAttribute('role')) {
                        const role = element.getAttribute('role');
                        const accessibleName = element.getAttribute('aria-label') || element.textContent.trim().substring(0, 30);
                        if (accessibleName) {
                            result.role = '[role="' + role + '"][aria-label*="' + accessibleName + '"]';
                        }
                    }
                    
                    // 9. placeholder (inputs)
                    if (element.placeholder) {
                        result.placeholder = '[placeholder="' + element.placeholder + '"]';
                    }
                    
                    // 10. title attribute
                    if (element.title) {
                        result.title = '[title="' + element.title + '"]';
                    }
                    
                    // 11. alt attribute (images)
                    if (element.alt) {
                        result.alt = 'img[alt="' + element.alt + '"]';
                    }
                    
                    // 12. value attribute (for inputs with values)
                    if (element.value && element.value.trim()) {
                        result.value = '[value="' + element.value + '"]';
                    }
                    
                    // 13. text content (if unique and not too long)
                    const textContent = element.textContent.trim();
                    if (textContent && textContent.length < 50 && textContent.length > 0) {
                        const tag = element.tagName.toLowerCase();
                        if (['button', 'a', 'span', 'label', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(tag)) {
                            result.text = tag + ':has-text("' + textContent + '")';
                        }
                    }
                    
                    // 14. CSS class (only if stable-looking - not auto-generated)
                    if (element.className && typeof element.className === 'string') {
                        const classes = element.className.trim().split(/\\s+/).filter(cls => {
                            // Filter out auto-generated classes (with random chars/numbers)
                            return !cls.match(/^[a-z0-9]{6,}$/i) && // Not random hash
                                   !cls.match(/^css-[a-z0-9]+$/i) && // Not CSS-in-JS
                                   cls.length < 30; // Not too long
                        });
                        
                        if (classes.length > 0 && classes.length <= 3) {
                            result['css-class'] = '.' + classes.join('.');
                        }
                    }
                    
                    // 15. Custom CSS selector (tag + unique combination)
                    const buildCssSelector = () => {
                        let selector = element.tagName.toLowerCase();
                        
                        // Add type for inputs
                        if (element.type) {
                            selector += '[type="' + element.type + '"]';
                        }
                        
                        // Add first stable class if exists
                        if (element.className && typeof element.className === 'string') {
                            const firstClass = element.className.trim().split(/\\s+/)[0];
                            if (firstClass && !firstClass.match(/^[a-z0-9]{6,}$/i)) {
                                selector += '.' + firstClass;
                            }
                        }
                        
                        return selector;
                    };
                    result['css-custom'] = buildCssSelector();
                    
                    // 16. Relative XPath (most flexible, works even with DOM changes)
                    const buildRelativeXPath = () => {
                        const tag = element.tagName.toLowerCase();
                        const attributes = [];
                        
                        // Prefer structural attributes for XPath
                        if (element.id) {
                            return '//' + tag + '[@id="' + element.id + '"]';
                        }
                        
                        if (element.hasAttribute('data-testid')) {
                            return '//' + tag + '[@data-testid="' + element.getAttribute('data-testid') + '"]';
                        }
                        
                        if (element.name) {
                            attributes.push('@name="' + element.name + '"');
                        }
                        
                        if (element.type) {
                            attributes.push('@type="' + element.type + '"');
                        }
                        
                        if (element.className && typeof element.className === 'string') {
                            const firstClass = element.className.trim().split(/\\s+/)[0];
                            if (firstClass && !firstClass.match(/^[a-z0-9]{6,}$/i)) {
                                attributes.push('contains(@class, "' + firstClass + '")');
                            }
                        }
                        
                        if (element.placeholder) {
                            attributes.push('@placeholder="' + element.placeholder + '"');
                        }
                        
                        if (element.hasAttribute('aria-label')) {
                            attributes.push('@aria-label="' + element.getAttribute('aria-label') + '"');
                        }
                        
                        // If we have attributes, use them
                        if (attributes.length > 0) {
                            return '//' + tag + '[' + attributes.join(' and ') + ']';
                        }
                        
                        // Fallback: use text content for clickable elements
                        const text = element.textContent.trim();
                        if (text && text.length < 50 && ['button', 'a', 'span', 'label'].includes(tag)) {
                            return '//' + tag + '[contains(text(), "' + text + '")]';
                        }
                        
                        // Last resort: tag with position
                        let position = 1;
                        let sibling = element.previousElementSibling;
                        while (sibling) {
                            if (sibling.tagName === element.tagName) {
                                position++;
                            }
                            sibling = sibling.previousElementSibling;
                        }
                        
                        return '//' + tag + '[' + position + ']';
                    };
                    result.xpath = buildRelativeXPath();
                    
                    // 17. Link text (for anchor tags)
                    if (element.tagName.toLowerCase() === 'a' && element.textContent.trim()) {
                        result['link-text'] = 'text="' + element.textContent.trim() + '"';
                    }
                    
                    // Additional metadata
                    result._tag = element.tagName.toLowerCase();
                    result._hasId = !!element.id;
                    result._hasStableClass = element.className && 
                        !element.className.match(/^[a-z0-9]{6,}$/i);
                    
                    return result;
                }
            """);
            
            // Convert to String map and maintain order
            extractedData.forEach((key, value) -> {
                if (value != null && !key.startsWith("_")) {
                    String strValue = value.toString();
                    if (!strValue.isEmpty() && !strValue.equals("null")) {
                        allLocators.put(key, strValue);
                    }
                }
            });
            
        } catch (Exception e) {
            // Fallback: at minimum, return a basic selector
            allLocators.put("fallback", "unknown-selector");
        }
        
        return allLocators;
    }
    
    /**
     * Get the best (most reliable) locator from the extracted set
     * 
     * @param allLocators Map of all extracted locators
     * @return The best selector string
     */
    public static String getBestLocator(Map<String, String> allLocators) {
        // Priority order for selection
        String[] preferredOrder = {
            "id", 
            "data-testid", 
            "data-test", 
            "data-cy",
            "name",
            "aria-label",
            "placeholder",
            "role",
            "text",
            "css-class",
            "css-custom",
            "xpath",
            "link-text"
        };
        
        for (String strategy : preferredOrder) {
            if (allLocators.containsKey(strategy)) {
                return allLocators.get(strategy);
            }
        }
        
        // Return first available
        return allLocators.values().iterator().next();
    }
    
    /**
     * Get the strategy name for a given selector
     * 
     * @param allLocators Map of all locators
     * @param selector The selector to find
     * @return Strategy name or "unknown"
     */
    public static String getStrategyForSelector(Map<String, String> allLocators, String selector) {
        for (Map.Entry<String, String> entry : allLocators.entrySet()) {
            if (entry.getValue().equals(selector)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }
    
    /**
     * Extract element attributes for verification
     * 
     * @param locator The Playwright Locator
     * @return Map of attribute name -> value
     */
    public static Map<String, String> extractElementAttributes(Locator locator) {
        Map<String, String> attributes = new LinkedHashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) locator.first().evaluate("""
                element => {
                    return {
                        'id': element.id || '',
                        'class': element.className || '',
                        'name': element.name || '',
                        'type': element.type || '',
                        'tag': element.tagName.toLowerCase(),
                        'data-testid': element.getAttribute('data-testid') || '',
                        'data-test': element.getAttribute('data-test') || '',
                        'data-cy': element.getAttribute('data-cy') || '',
                        'aria-label': element.getAttribute('aria-label') || '',
                        'role': element.getAttribute('role') || '',
                        'placeholder': element.placeholder || '',
                        'title': element.title || '',
                        'alt': element.alt || '',
                        'href': element.href || '',
                        'text': element.textContent ? element.textContent.trim().substring(0, 50) : ''
                    };
                }
            """);
            
            // Convert to string map, keeping only non-empty values
            attrs.forEach((key, value) -> {
                if (value != null && !value.toString().isEmpty()) {
                    attributes.put(key, value.toString());
                }
            });
            
        } catch (Exception e) {
            // Return empty map if extraction fails
        }
        
        return attributes;
    }
}
