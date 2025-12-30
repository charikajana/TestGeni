package automation.browser.actions.slider;

import automation.browser.actions.BrowserAction;
import automation.browser.SmartLocator;
import automation.planner.ActionPlan;
import automation.utils.LoggerUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Generic slider action that works with any slider implementation:
 * - HTML5 input[type="range"]
 * - ARIA sliders (role="slider")
 * - Custom React/Angular/Vue sliders
 * - Material-UI, Ant Design, etc.
 * 
 * Uses multiple strategies with automatic fallback:
 * 1. JavaScript event dispatch (most reliable)
 * 2. Playwright native fill
 * 3. Physical drag
 * 4. Keyboard navigation
 */
public class SetSliderAction implements BrowserAction {
    
    private static final LoggerUtil logger = LoggerUtil.getLogger(SetSliderAction.class);
    
    @Override
    public boolean execute(Page page, SmartLocator locator, ActionPlan plan) {
        String sliderName = plan.getElementName();
        String targetValue = plan.getValue();
        
        logger.info("Setting slider '{}' to value: {}", sliderName, targetValue);
        
        // Find slider element
        Locator slider = locator.waitForSmartElement(sliderName, "slider", null, plan.getFrameAnchor());
        
        if (slider == null) {
            logger.failure("Slider not found: {}", sliderName);
            return false;
        }
        
        // Try multiple strategies in order of reliability
        
        // Strategy 1: JavaScript (works for most modern sliders)
        if (tryJavaScriptSet(slider, targetValue, sliderName)) {
            return true;
        }
        
        // Strategy 2: Playwright's built-in fill (HTML5 range)
        if (tryNativeSet(slider, targetValue, sliderName)) {
            return true;
        }
        
        // Strategy 3: Physical drag
        if (tryDragToValue(page, slider, targetValue, sliderName)) {
            return true;
        }
        
        // Strategy 4: Keyboard navigation
        if (tryKeyboardSet(slider, targetValue, sliderName)) {
            return true;
        }
        
        logger.failure("All slider interaction strategies failed for '{}'", sliderName);
        return false;
    }
    
    /**
     * Strategy 1: Set value using JavaScript events
     * Works for: HTML5 range, React sliders, custom sliders
     */
    private boolean tryJavaScriptSet(Locator slider, String value, String name) {
        try {
            logger.debug("Trying JavaScript strategy...");
            
            slider.evaluate("(el, val) => {" +
                "  const numVal = parseInt(val);" +
                "  if (el.type === 'range') {" +
                "    el.value = numVal;" +
                "    el.dispatchEvent(new Event('input', { bubbles: true }));" +
                "    el.dispatchEvent(new Event('change', { bubbles: true }));" +
                "  } else if (el.getAttribute('role') === 'slider') {" +
                "    el.setAttribute('aria-valuenow', numVal);" +
                "    el.dispatchEvent(new CustomEvent('change', { detail: { value: numVal } }));" +
                "  }" +
                "}", value);
            
            Thread.sleep(300); // Allow UI to update
            
            // Verify value was set
            String actualValue = getSliderValue(slider);
            if (actualValue != null && isValueClose(value, actualValue, 1)) {
                logger.success("Set slider '{}' to {} via JavaScript", name, value);
                return true;
            }
            
            logger.debug("JavaScript strategy verification failed. Expected: {}, Actual: {}", value, actualValue);
        } catch (Exception e) {
            logger.debug("JavaScript strategy failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Strategy 2: Use Playwright's native fill method
     * Works for: HTML5 input[type="range"]
     */
    private boolean tryNativeSet(Locator slider, String value, String name) {
        try {
            logger.debug("Trying native fill strategy...");
            
            slider.fill(value);
            Thread.sleep(300);
            
            String actualValue = getSliderValue(slider);
            if (actualValue != null && isValueClose(value, actualValue, 1)) {
                logger.success("Set slider '{}' to {} via native fill", name, value);
                return true;
            }
            
            logger.debug("Native fill verification failed. Expected: {}, Actual: {}", value, actualValue);
        } catch (Exception e) {
            logger.debug("Native fill strategy failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Strategy 3: Physical drag to position
     * Works for: Most visual sliders
     */
    private boolean tryDragToValue(Page page, Locator slider, String value, String name) {
        try {
            logger.debug("Trying drag strategy...");
            
            // Get slider bounds and properties
            String minStr = getSliderAttribute(slider, "min", "0");
            String maxStr = getSliderAttribute(slider, "max", "100");
            
            int min = Integer.parseInt(minStr);
            int max = Integer.parseInt(maxStr);
            int target = Integer.parseInt(value);
            
            // Validate range
            if (target < min || target > max) {
                logger.warning("Target value {} is outside slider range [{}, {}]", target, min, max);
                return false;
            }
            
            // Calculate percentage
            double percentage = (double)(target - min) / (max - min);
            
            // Get bounding box
            var box = slider.boundingBox();
            if (box == null) {
                logger.debug("Bounding box not available");
                return false;
            }
            
            // Calculate target X position
            double targetX = box.x + (box.width * percentage);
            double centerY = box.y + (box.height / 2);
            
            // Perform drag
            slider.click(); // Focus first
            page.mouse().move(targetX, centerY);
            page.mouse().down();
            page.mouse().move(targetX, centerY);
            page.mouse().up();
            
            Thread.sleep(300);
            
            String actualValue = getSliderValue(slider);
            int tolerance = Math.max((max - min) / 20, 2); // 5% tolerance or minimum 2
            if (actualValue != null && isValueClose(value, actualValue, tolerance)) {
                logger.success("Set slider '{}' to {} via drag (actual: {})", name, value, actualValue);
                return true;
            }
            
            logger.debug("Drag verification failed. Expected: {}, Actual: {}", value, actualValue);
        } catch (Exception e) {
            logger.debug("Drag strategy failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Strategy 4: Keyboard navigation
     * Works for: All focusable sliders
     */
    private boolean tryKeyboardSet(Locator slider, String value, String name) {
        try {
            logger.debug("Trying keyboard navigation strategy...");
            
            // Focus slider
            slider.click();
            Thread.sleep(200);
            
            // Get current and target values
            String currentStr = getSliderValue(slider);
            if (currentStr == null) {
                logger.debug("Cannot get current slider value for keyboard navigation");
                return false;
            }
            
            int current = Integer.parseInt(currentStr);
            int target = Integer.parseInt(value);
            int diff = target - current;
            
            if (diff == 0) {
                logger.success("Slider '{}' already at target value {}", name, value);
                return true;
            }
            
            // Navigate with arrow keys
            String key = diff > 0 ? "ArrowRight" : "ArrowLeft";
            int steps = Math.abs(diff);
            
            // Use PageUp/PageDown for large jumps (typically +/- 10)
            if (steps > 20) {
                String pageKey = diff > 0 ? "PageUp" : "PageDown";
                int pageSteps = steps / 10;
                for (int i = 0; i < pageSteps; i++) {
                    slider.press(pageKey);
                    Thread.sleep(50);
                }
                steps = steps % 10;
            }
            
            // Fine-tune with arrow keys (cap at 50 to avoid infinite loops)
            for (int i = 0; i < Math.min(steps, 50); i++) {
                slider.press(key);
                Thread.sleep(50);
            }
            
            Thread.sleep(300);
            
            String actualValue = getSliderValue(slider);
            if (actualValue != null && isValueClose(value, actualValue, 2)) {
                logger.success("Set slider '{}' to {} via keyboard (actual: {})", name, value, actualValue);
                return true;
            }
            
            logger.debug("Keyboard verification failed. Expected: {}, Actual: {}", value, actualValue);
        } catch (Exception e) {
            logger.debug("Keyboard strategy failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Get current slider value
     */
    private String getSliderValue(Locator slider) {
        try {
            Object result = slider.evaluate("el => {" +
                "  if (el.value !== undefined && el.value !== null) return String(el.value);" +
                "  if (el.getAttribute('aria-valuenow')) return el.getAttribute('aria-valuenow');" +
                "  return null;" +
                "}");
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get slider attribute with fallback
     */
    private String getSliderAttribute(Locator slider, String attr, String defaultVal) {
        try {
            String val = slider.getAttribute(attr);
            return val != null ? val : defaultVal;
        } catch (Exception e) {
            return defaultVal;
        }
    }
    
    /**
     * Check if two values are close within tolerance
     */
    private boolean isValueClose(String expected, String actual, int tolerance) {
        try {
            int exp = Integer.parseInt(expected);
            int act = Integer.parseInt(actual);
            return Math.abs(exp - act) <= tolerance;
        } catch (Exception e) {
            return expected.equals(actual);
        }
    }
}
